package buildlogic

import groovy.json.JsonSlurper
import groovy.json.JsonOutput
import net.fabricmc.loader.api.Version
import net.fabricmc.loader.api.metadata.version.VersionPredicate
import java.io.ByteArrayInputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.LinkOption
import java.nio.file.StandardCopyOption
import java.nio.file.AtomicMoveNotSupportedException
import java.security.MessageDigest
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream

data class TargetSpec(val minecraft: String, val fabricApi: String, val hmApi: String)
interface VerifiedAsset {
    val path: Path
    val size: Long
    val sha256: String
}
data class VerifiedJar(override val path: Path, val minecraft: String, val modVersion: String,
    override val size: Long, override val sha256: String, val edition: Edition = Edition.FULL) : VerifiedAsset
data class VerifiedManifest(override val path: Path, override val size: Long, override val sha256: String) : VerifiedAsset

private const val MANIFEST = "build-manifest.json"
internal val minecraftTargets = setOf("26.1.2", "26.2", "26.3")

fun collectDistribution(paths: List<Path>, targets: List<TargetSpec>, modVersion: String, destination: Path,
    editions: Set<Edition> = setOf(Edition.FULL)): List<VerifiedJar> {
    val verified = verifyDistribution(paths, targets, modVersion, editions)
    val dest = destination.toAbsolutePath().normalize()
    require(!Files.isSymbolicLink(dest)) { "Distribution destination must not be a symbolic link" }
    val parent = dest.parent
    Files.createDirectories(parent)
    val stage = Files.createTempDirectory(parent, ".${dest.fileName}-stage-")
    val backup = Files.createTempDirectory(parent, ".${dest.fileName}-backup-")
    Files.delete(backup)
    fun move(from: Path, to: Path) {
        try { Files.move(from, to, StandardCopyOption.ATOMIC_MOVE) }
        catch (_: AtomicMoveNotSupportedException) { Files.move(from, to) }
    }
    fun removeTemporary(path: Path) {
        require(path.toAbsolutePath().normalize().parent == parent && path != dest) { "Unsafe temporary directory" }
        if (Files.exists(path, LinkOption.NOFOLLOW_LINKS)) Files.walk(path).use { stream -> stream.sorted(Comparator.reverseOrder()).forEach { Files.delete(it) } }
    }
    var installed = false
    try {
        verified.forEach { Files.copy(it.path, stage.resolve(it.path.fileName)) }
        // Validate the copied bytes too, before replacing the previous complete directory.
        val staged = verifyDistribution(verified.map { stage.resolve(it.path.fileName) }, targets, modVersion, editions)
        Files.writeString(stage.resolve(MANIFEST), manifestText(modVersion, staged))
        if (Files.exists(dest)) move(dest, backup)
        try { move(stage, dest) }
        catch (failure: Exception) {
            if (Files.exists(backup)) try { move(backup, dest) } catch (rollback: Exception) { failure.addSuppressed(rollback) }
            throw failure
        }
        installed = true
        return staged.map { it.copy(path = dest.resolve(it.path.fileName)) }
    } finally {
        removeTemporary(stage)
        // If rollback failed, retain the backup for recovery.
        if (installed) removeTemporary(backup)
    }
}

private fun manifestText(version: String, jars: List<VerifiedJar>) = JsonOutput.prettyPrint(JsonOutput.toJson(mapOf(
    "version" to version, "files" to jars.sortedBy { it.path.fileName.toString() }.map {
        mapOf("name" to it.path.fileName.toString(), "minecraft" to it.minecraft, "edition" to it.edition.id,
            "size" to it.size, "sha256" to it.sha256)
    }
)))

fun verifyPublicManifest(path: Path, version: String, jars: List<VerifiedJar>): VerifiedManifest {
    require(path.fileName.toString() == MANIFEST && Files.isRegularFile(path) &&
        Files.size(path) in 1..65536L && Files.readString(path) == manifestText(version, jars)) {
        "The public manifest does not match the verified GitHub JARs"
    }
    return VerifiedManifest(path, Files.size(path), sha256(path))
}

private const val MAX_ENTRY = 32 * 1024 * 1024
private fun json(bytes: ByteArray): Map<*, *> =
    (JsonSlurper().parseText(bytes.toString(Charsets.UTF_8)) as? Map<*, *>)
        ?: throw IllegalArgumentException("Expected JSON object")
private fun bytes(input: java.io.InputStream): ByteArray = input.readNBytes(MAX_ENTRY + 1).also {
    require(it.size <= MAX_ENTRY) { "Oversized archive entry" }
}
private fun nested(data: ByteArray): Map<String, ByteArray> {
    val entries = linkedMapOf<String, ByteArray>()
    var total = 0L
    ZipInputStream(ByteArrayInputStream(data)).use { z ->
        while (true) {
            val entry = z.nextEntry ?: break
            if (!entry.isDirectory) {
                require(entry.name !in entries) { "Duplicate nested entry ${entry.name}" }
                val value = bytes(z); total += value.size
                require(total <= 128L * 1024 * 1024) { "Oversized nested archive" }
                entries[entry.name] = value
            }
        }
    }
    return entries
}
private fun accepts(requirement: Any?, version: String): Boolean = when (requirement) {
    is String -> VersionPredicate.parse(requirement).test(Version.parse(version))
    is List<*> -> requirement.any { accepts(it, version) }
    null -> true
    else -> false
}

fun verifyJar(path: Path, target: TargetSpec, modVersion: String, edition: Edition = Edition.FULL): VerifiedJar {
    require(path.fileName.toString() == edition.jarName(modVersion, target.minecraft)) { "Unexpected installable filename: $path" }
    require(Files.isRegularFile(path) && Files.size(path) in 1..(64L * 1024 * 1024)) { "Missing or oversized JAR: $path" }
    try {
        ZipFile(path.toFile()).use { z ->
            val names = z.entries().asSequence().map { it.name }.toList()
            require(names.size == names.toSet().size) { "Duplicate archive entries" }
            fun entry(name: String) = z.getEntry(name)?.let { z.getInputStream(it).use(::bytes) }
                ?: throw IllegalArgumentException("Missing runtime resource: $name")
            val metadata = json(entry("fabric.mod.json"))
            require(metadata["id"] == "sparkling-mutuals" && metadata["version"] == modVersion && metadata["environment"] == "client") { "Incorrect mod identity/version/environment" }
            val declaredEdition = (metadata["custom"] as? Map<*, *>)?.get("sparkling-mutuals:edition")
            require(declaredEdition == edition.id) { "Incorrect mod edition" }
            verifyEditionContents(edition, names)
            val depends = metadata["depends"] as? Map<*, *> ?: error("Missing dependency metadata")
            require(depends["minecraft"] == target.minecraft && depends["java"] == ">=25" && depends["fabricloader"] == ">=0.19.5") { "Incorrect target/Java/loader metadata" }
            require(depends["fabric-api"] != null && accepts(depends["fabric-api"], target.fabricApi)) { "Incompatible Fabric API" }
            require(depends["fabric-language-kotlin"] != null && accepts(depends["fabric-language-kotlin"], "1.14.1+kotlin.2.4.20")) { "Incompatible Fabric Kotlin" }
            val main = (metadata["entrypoints"] as? Map<*, *>)?.get("main") as? List<*>
            require(main?.any { it is Map<*, *> && it["value"] == "net.johnceo.sparklingmutuals.SparklingMutuals" && it["adapter"] == "kotlin" } == true) { "Missing Kotlin entrypoint" }
            entry("net/johnceo/sparklingmutuals/SparklingMutuals.class")
            entry(metadata["icon"] as? String ?: throw IllegalArgumentException("Missing icon"))
            entry("LICENSE_sparkling-mutuals")
            require(names.any { it.startsWith("licenses/") && !it.endsWith("/") }) { "Missing third-party notices" }
            val mixinFiles = metadata["mixins"] as? List<*> ?: throw IllegalArgumentException("Missing mixins")
            require(mixinFiles.isNotEmpty()) { "Empty mixin configuration" }
            mixinFiles.forEach { name ->
                require(name is String) { "Unexpected mixin registration" }
                val mixins = json(entry(name))
                require(mixins["required"] == true) { "Mixins must remain required" }
                val prefix = (mixins["package"] as? String ?: throw IllegalArgumentException("Missing mixin package")).replace('.', '/')
                val clients = mixins["client"] as? List<*> ?: throw IllegalArgumentException("Missing client mixins")
                require(clients.isNotEmpty()) { "No client mixins" }
                clients.forEach { entry("$prefix/$it.class") }
            }
            val hm = "META-INF/jars/hm-api-${target.hmApi}.jar"
            val declared = (metadata["jars"] as? List<*>)?.map { (it as? Map<*, *>)?.get("file") }
            require(declared?.toSet() == setOf(hm) && declared.size == 1) { "Incorrect bundled artifacts" }
            val embedded = names.filter { it.startsWith("META-INF/jars/") && it.endsWith(".jar") }
            require(embedded.toSet() == setOf(hm)) { "Unexpected nested dependency (development JARs forbidden)" }
            for (file in embedded) {
                val content = nested(entry(file))
                val mod = json(content["fabric.mod.json"] ?: throw IllegalArgumentException("Missing nested Fabric metadata"))
                require(!(mod["id"] as? String).orEmpty().contains("devauth", ignoreCase = true)) { "DevAuth must not ship" }
                val requirements = mod["depends"] as? Map<*, *> ?: emptyMap<Any, Any>()
                require(accepts(requirements["minecraft"], target.minecraft) && accepts(requirements["java"], "25") &&
                    accepts(requirements["fabricloader"], "0.19.5") && accepts(requirements["fabric-api"], target.fabricApi)) { "Incompatible nested metadata: $file" }
                if (file == hm) require(mod["id"] == "hm-api" && mod["version"] == target.hmApi) { "Incorrect HM API" }

            }
        }
    } catch (e: Exception) {
        if (e is IllegalArgumentException) throw e
        throw IllegalArgumentException("Invalid distributable $path: ${e.message}", e)
    }
    return VerifiedJar(path, target.minecraft, modVersion, Files.size(path), sha256(path), edition)
}

private fun sha256(path: Path) = Files.newInputStream(path).use { input ->
    val digest = MessageDigest.getInstance("SHA-256")
    val buffer = ByteArray(8192)
    while (true) {
        val read = input.read(buffer)
        if (read < 0) break
        digest.update(buffer, 0, read)
    }
    digest.digest().joinToString("") { "%02x".format(it) }
}

private fun verifyEditionContents(edition: Edition, names: List<String>) {
    val prefix = "net/johnceo/sparklingmutuals/"
    if (edition != Edition.FULL) {
        require(names.none { it.startsWith(prefix + "config/ReleaseUpdateChecker") || it.startsWith(prefix + "update/") }) { "Updater class found in ${edition.id}" }
    }
    if (edition == Edition.STANDARD) {
        require(prefix + "safari/visibility/LocalVisibility.class" in names) { "Standard visibility policy is missing" }
        val forbidden = listOf("alerts/SparkleAutoParty", "alerts/SparkleWarpPolicy", "alerts/AlertManager",
            "alerts/WarpAlertTimer", "alerts/WarpInvitations", "alerts/WarpList.class", "alerts/WarpList\$",
            "alerts/WarpListCsv", "alerts/WarpListRefresh", "alerts/WarpListScreen", "alerts/WarpPlayerStatus",
            "commands/AlertCommand", "mixin/StandardInvite", "mixin/SafariManagerMixin",
            "config/SafariSettings\$Warp",
            "safari/automation/AutoClicker", "safari/automation/GemzieAutoSelect", "safari/automation/entry/CjsApproachInput",
            "safari/automation/entry/CjsEntryDispatch", "safari/automation/entry/CjsEntryInput",
            "safari/automation/entry/CjsSafariEntry", "safari/automation/entry/CjsWalkingKeys", "safari/hideyho/HideyhoQuest",
            "mixin/CjsEntry", "mixin/DisconnectedScreenAccessor", "mixin/GemzieUseMixin", "mixin/HypixelDisconnectMixin",
            "mixin/SafariAttackKeyAccessor", "mixin/SafariAutoClickerMixin", "mixin/SafariCandleMixin", "mixin/SafariTicketScreenMixin")
        require(names.none { name -> forbidden.any { name.startsWith(prefix + it) } }) { "Full gameplay automation found in Standard" }
        val fullRenderers = listOf("safari/esp/TeammateEsp", "safari/esp/DuplicoGlow",
            "mixin/CritterTextDisplayMixin", "mixin/SafariShinyMixin")
        require(names.none { it.removePrefix(prefix).removeSuffix(".class").substringBefore('$') in fullRenderers }) {
            "Full nameplate, unrestricted glow or teammate renderer found in Standard"
        }
    }
}

fun verifyDistribution(paths: List<Path>, targets: List<TargetSpec>, modVersion: String,
    editions: Set<Edition> = setOf(Edition.FULL)): List<VerifiedJar> {
    require(targets.map { it.minecraft }.toSet() == minecraftTargets && targets.size == minecraftTargets.size) { "Expected exactly three supported targets" }
    val count = targets.size * editions.size
    require(count > 0 && paths.size == count && paths.map { it.toAbsolutePath().normalize() }.distinct().size == count) { "Missing or duplicate target JARs" }
    val verified = editions.flatMap { edition ->
        targets.map { target ->
            val file = paths.singleOrNull { it.fileName.toString() == edition.jarName(modVersion, target.minecraft) }
                ?: throw IllegalArgumentException("Missing or duplicate ${edition.id} target ${target.minecraft}")
            verifyJar(file, target, modVersion, edition)
        }
    }
    // Platform adapters compile differently, but every target must ship the same feature classes and assets.
    // Only the 26.1.2 immediate renderer needs this anonymous captured-pose adapter for Iris composition.
    val immediatePoseAdapter = "net/johnceo/sparklingmutuals/safari/shots/preview/CapsulePreviewRenderer\$render\$captured\$1.class"
    fun contents(path: Path): Map<String, String> = ZipFile(path.toFile()).use { zip ->
        zip.entries().asSequence().filter { !it.isDirectory && !it.name.startsWith("META-INF/") && it.name != "fabric.mod.json" && it.name != immediatePoseAdapter }
            .associate { entry -> entry.name to if (entry.name.endsWith(".class")) "class" else
                zip.getInputStream(entry).use { MessageDigest.getInstance("SHA-256").digest(bytes(it)) }.joinToString("") { "%02x".format(it) } }
    }
    verified.groupBy { it.edition }.values.forEach { jars ->
        val baseline = contents(jars.first().path)
        jars.drop(1).forEach { jar ->
            val actual = contents(jar.path)
            require(actual == baseline) { "Feature/resource parity differs for ${jar.edition.id} ${jar.minecraft}: " +
                (baseline.keys + actual.keys).filter { baseline[it] != actual[it] }.joinToString() }
        }
    }
    return verified
}
