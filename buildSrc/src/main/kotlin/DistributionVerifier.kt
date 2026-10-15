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
import java.util.jar.Manifest
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream

data class TargetSpec(val minecraft: String, val fabricApi: String, val hmApi: String, val moulConfig: String)
data class VerifiedJar(val path: Path, val minecraft: String, val modVersion: String, val size: Long, val sha256: String)

fun collectDistribution(paths: List<Path>, targets: List<TargetSpec>, modVersion: String, destination: Path): List<VerifiedJar> {
    val verified = verifyDistribution(paths, targets, modVersion)
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
        val staged = verifyDistribution(verified.map { stage.resolve(it.path.fileName) }, targets, modVersion)
        Files.writeString(stage.resolve("build-manifest.json"), JsonOutput.prettyPrint(JsonOutput.toJson(mapOf(
            "version" to modVersion, "files" to staged.map { mapOf("name" to it.path.fileName.toString(),
                "minecraft" to it.minecraft, "size" to it.size, "sha256" to it.sha256) }))))
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

fun verifyJar(path: Path, target: TargetSpec, modVersion: String): VerifiedJar {
    require(path.fileName.toString() == "sparkling-mutuals-$modVersion+mc${target.minecraft}.jar") { "Unexpected installable filename: $path" }
    require(Files.isRegularFile(path) && Files.size(path) in 1..(64L * 1024 * 1024)) { "Missing or oversized JAR: $path" }
    try {
        ZipFile(path.toFile()).use { z ->
            val names = z.entries().asSequence().map { it.name }.toList()
            require(names.size == names.toSet().size) { "Duplicate archive entries" }
            fun entry(name: String) = z.getEntry(name)?.let { z.getInputStream(it).use(::bytes) }
                ?: throw IllegalArgumentException("Missing runtime resource: $name")
            val metadata = json(entry("fabric.mod.json"))
            require(metadata["id"] == "sparkling-mutuals" && metadata["version"] == modVersion && metadata["environment"] == "client") { "Incorrect mod identity/version/environment" }
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
            val moul = "META-INF/jars/${target.moulConfig}-4.8.0.jar"
            val declared = (metadata["jars"] as? List<*>)?.map { (it as? Map<*, *>)?.get("file") }
            require(declared?.toSet() == setOf(hm, moul) && declared.size == 2) { "Incorrect bundled artifacts" }
            val embedded = names.filter { it.startsWith("META-INF/jars/") && it.endsWith(".jar") }
            require(embedded.toSet() == setOf(hm, moul)) { "Unexpected nested dependency (development JARs forbidden)" }
            for (file in embedded) {
                val content = nested(entry(file))
                val mod = json(content["fabric.mod.json"] ?: throw IllegalArgumentException("Missing nested Fabric metadata"))
                require(!(mod["id"] as? String).orEmpty().contains("devauth", ignoreCase = true)) { "DevAuth must not ship" }
                val requirements = mod["depends"] as? Map<*, *> ?: emptyMap<Any, Any>()
                require(accepts(requirements["minecraft"], target.minecraft) && accepts(requirements["java"], "25") &&
                    accepts(requirements["fabricloader"], "0.19.5") && accepts(requirements["fabric-api"], target.fabricApi)) { "Incompatible nested metadata: $file" }
                if (file == hm) require(mod["id"] == "hm-api" && mod["version"] == target.hmApi) { "Incorrect HM API" }
                if (file == moul) {
                    require("io/github/notenoughupdates/moulconfig/Config.class" in content) { "Missing MoulConfig runtime" }
                    val manifest = Manifest(ByteArrayInputStream(content["META-INF/MANIFEST.MF"] ?: throw IllegalArgumentException("Missing MoulConfig manifest")))
                    val buildTarget = manifest.mainAttributes.getValue("Fabric-Minecraft-Version")
                    // Upstream modern-26.2:4.8.0 retains its 26.2-pre-2 build label. Its startup/settings linkage is smoke-tested.
                    val pinned262 = target.minecraft == "26.2" && target.moulConfig == "modern-26.2" && buildTarget == "26.2-pre-2"
                    require(buildTarget == target.moulConfig.removePrefix("modern-") || pinned262) { "Wrong MoulConfig target" }
                }
            }
        }
    } catch (e: Exception) {
        if (e is IllegalArgumentException) throw e
        throw IllegalArgumentException("Invalid distributable $path: ${e.message}", e)
    }
    val digest = Files.newInputStream(path).use { input ->
        val md = MessageDigest.getInstance("SHA-256"); val buffer = ByteArray(8192)
        while (true) { val read = input.read(buffer); if (read < 0) break; md.update(buffer, 0, read) }
        md.digest().joinToString("") { "%02x".format(it) }
    }
    return VerifiedJar(path, target.minecraft, modVersion, Files.size(path), digest)
}

fun verifyDistribution(paths: List<Path>, targets: List<TargetSpec>, modVersion: String): List<VerifiedJar> {
    require(targets.map { it.minecraft }.toSet() == setOf("26.1.2", "26.2", "26.3") && targets.size == 3) { "Expected exactly three supported targets" }
    require(paths.size == 3 && paths.map { it.toAbsolutePath().normalize() }.distinct().size == 3) { "Missing or duplicate target JARs" }
    return targets.map { target ->
        val file = paths.singleOrNull { it.fileName.toString() == "sparkling-mutuals-$modVersion+mc${target.minecraft}.jar" }
            ?: throw IllegalArgumentException("Missing or duplicate target ${target.minecraft}")
        verifyJar(file, target, modVersion)
    }
}
