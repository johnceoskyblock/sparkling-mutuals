package net.johnceo.sparklingmutuals.config

import net.fabricmc.loader.api.FabricLoader
import net.fabricmc.loader.api.metadata.ModOrigin
import net.johnceo.sparklingmutuals.update.FullUpdateManifest
import net.johnceo.sparklingmutuals.update.UpdateInstall
import net.johnceo.sparklingmutuals.update.UpdateTransport
import java.io.IOException
import java.net.URI
import java.nio.file.Path
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.slf4j.LoggerFactory

object ModUpdates {
    const val canCheck = true
    const val REPOSITORY = "https://github.com/johnceoskyblock/sparkling-mutuals"
    private val loader get() = FabricLoader.getInstance()
    val installed: String = loader.getModContainer("sparkling-mutuals").map { it.metadata.version.friendlyString }.orElse("dev")
    val minecraftVersion: String = loader.getModContainer("minecraft").map { it.metadata.version.friendlyString }.orElse("")


    private val logger = LoggerFactory.getLogger("sparkling-mutuals/update")
    private val worker = Executors.newSingleThreadExecutor { work ->
        Thread(work, "Sparkling Mutuals updates").apply { isDaemon = true }
    }
    private val checker = ReleaseUpdateChecker(installed, minecraftVersion, fetch = {
        UpdateTransport.metadata(URI("https://api.github.com/repos/johnceoskyblock/sparkling-mutuals/releases?per_page=100"),
            8 * 1024 * 1024, githubApi = true)
    }, execute = { worker.execute(it) }, onUpdate = { candidate ->
        if (autoUpdate && candidate.tag != attemptedAutomatically) {
            attemptedAutomatically = candidate.tag
            download()
        }
    })

    @Volatile private var installer: UpdateInstall? = null
    @Volatile private var status: String? = null
    @Volatile private var staging = false
    @Volatile private var pending = false
    @Volatile private var retryInstall = false
    @Volatile private var preferencesSaving = false
    @Volatile var autoUpdate = false
        private set
    private var attemptedAutomatically: String? = null

    val release get() = checker.state.release
    val label get() = status ?: checker.state.label
    val canToggle get() = installer != null && !preferencesSaving
    val canDownload get() = installer != null && !staging && (!pending || retryInstall) &&
        (retryInstall || checker.state.release?.let {
            it.asset != null && it.manifest != null && (ReleaseVersion.compareVersions(it.tag, installed) ?: 0) > 0
        } == true)
    val downloadLabel get() = when {
        retryInstall -> "Retry on restart"
        status?.startsWith("Download failed") == true -> "Retry Download"
        else -> "Download Update"
    }

    fun initialize(configDirectory: Path) {
        val origin = loader.getModContainer("sparkling-mutuals").orElse(null)?.origin ?: return
        if (origin.kind != ModOrigin.Kind.PATH) return
        val installedPath = origin.paths.singleOrNull() ?: return
        worker.execute {
            try {
                val transaction = UpdateInstall(loader.gameDir.resolve("mods"), configDirectory.resolve("updates"),
                    installedPath, installed, minecraftVersion)
                autoUpdate = transaction.autoUpdate()
                installer = transaction
                Runtime.getRuntime().addShutdownHook(Thread({
                    if (transaction.hasPending() && !retryInstall) {
                        try { transaction.launch(ProcessHandle.current()) }
                        catch (failure: Exception) {
                            logger.warn("Could not launch Sparkling Mutuals restart installer", failure)
                        }
                    }
                }, "Sparkling Mutuals restart installer"))
                transaction.removeOldHelpers()
                pending = transaction.verifyPending() != null
                val last = transaction.status()
                if (pending) {
                    retryInstall = last?.failed() == true
                    status = if (retryInstall) "Install failed. Use Retry on restart." else "Ready to install on restart"
                } else if (last?.failed() == true) {
                    status = last.message()
                }
            } catch (failure: Exception) {
                status = "Update installer unavailable"
                logger.warn("Update installer could not initialize", failure)
            }
        }
    }

    fun check(force: Boolean = false) = checker.check(force)

    fun toggleAutoUpdate() {
        val transaction = installer ?: return
        if (preferencesSaving) return
        preferencesSaving = true
        worker.execute {
            try {
                val enabled = !autoUpdate
                transaction.saveAutoUpdate(enabled)
                autoUpdate = enabled
                if (enabled) {
                    attemptedAutomatically = null
                    download()
                }
            } catch (failure: Exception) {
                status = "Could not save Auto Update"
                logger.warn("Could not save update preferences", failure)
            } finally {
                preferencesSaving = false
            }
        }
    }

    @Synchronized fun download() {
        val transaction = installer ?: return
        if (retryInstall) {
            retryInstall = false
            status = "Ready to install on restart"
            return
        }
        if (!canDownload) return
        val release = checker.state.release ?: return
        val asset = release.asset ?: return
        val manifest = release.manifest ?: return
        staging = true
        status = "Preparing download…"
        worker.execute {
            try {
                val candidate = FullUpdateManifest.artifact(UpdateTransport.metadata(manifest, 64 * 1024),
                    release.tag.removePrefix("v"), minecraftVersion)
                val started = System.nanoTime()
                transaction.stage(candidate) { output ->
                    UpdateTransport.read(asset) { input ->
                        val buffer = ByteArray(64 * 1024)
                        var total = 0L
                        while (true) {
                            if (System.nanoTime() - started > TimeUnit.MINUTES.toNanos(3))
                                throw IOException("Update download timed out")
                            val size = input.read(buffer)
                            if (size < 0) break
                            output.write(buffer, 0, size)
                            total += size
                            status = "Downloading update… ${total * 100 / candidate.size()}%"
                        }
                    }
                }
                pending = true
                status = "Ready to install on restart"
            } catch (failure: Exception) {
                status = "Download failed. You can retry."
                logger.warn("Sparkling Mutuals update download failed", failure)
            } finally {
                staging = false
            }
        }
    }
    fun compareVersions(a: String, b: String): Int? = ReleaseVersion.compareVersions(a, b)
}
