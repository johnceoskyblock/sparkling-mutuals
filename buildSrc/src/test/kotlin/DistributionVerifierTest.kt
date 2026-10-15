package buildlogic

import groovy.json.JsonOutput
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class DistributionVerifierTest {
    @TempDir lateinit var dir: Path
    private val targets = listOf(
        TargetSpec("26.1.2", "0.155.3+26.1.2", "1.0.3+26.1", "modern-26.1"),
        TargetSpec("26.2", "0.161.0+26.2", "1.0.4+26.2", "modern-26.2"),
        TargetSpec("26.3", "0.163.0+26.3", "1.0.5+26.3", "modern-26.3"))
    private fun zip(entries: Map<String, ByteArray>): ByteArray = ByteArrayOutputStream().use { bytes ->
        ZipOutputStream(bytes).use { z -> entries.forEach { (name, data) ->
            z.putNextEntry(ZipEntry(name)); z.write(data); z.closeEntry()
        } }; bytes.toByteArray()
    }
    private fun jar(target: TargetSpec, moulMinecraft: String = target.moulConfig.removePrefix("modern-"), change: (MutableMap<String, Any>, MutableMap<String, ByteArray>) -> Unit = { _, _ -> }): Path {
        val hmName = "META-INF/jars/hm-api-${target.hmApi}.jar"
        val moulName = "META-INF/jars/${target.moulConfig}-4.8.0.jar"
        val entries = mutableMapOf(
            "net/johnceo/sparklingmutuals/SparklingMutuals.class" to byteArrayOf(1),
            "assets/sparkling-mutuals/icon.png" to byteArrayOf(1),
            "LICENSE_sparkling-mutuals" to "CC0".toByteArray(),
            "licenses/HM-API-LICENSE" to "Apache".toByteArray(),
            "sparkling-mutuals.mixins.json" to JsonOutput.toJson(mapOf("required" to true,
                "package" to "net.johnceo.sparklingmutuals.mixin", "client" to listOf("SafariCapsuleMixin"))).toByteArray(),
            "net/johnceo/sparklingmutuals/mixin/SafariCapsuleMixin.class" to byteArrayOf(1),
            hmName to zip(mapOf("fabric.mod.json" to JsonOutput.toJson(mapOf("id" to "hm-api", "version" to target.hmApi,
                "depends" to mapOf("minecraft" to "~${target.minecraft}", "java" to ">=25"))).toByteArray())),
            moulName to zip(mapOf("fabric.mod.json" to "{\"id\":\"libninepatch\",\"version\":\"1.2.0\"}".toByteArray(),
                "META-INF/MANIFEST.MF" to "Manifest-Version: 1.0\r\nFabric-Minecraft-Version: $moulMinecraft\r\n\r\n".toByteArray(),
                "io/github/notenoughupdates/moulconfig/Config.class" to byteArrayOf(1))))
        val metadata = mutableMapOf<String, Any>("schemaVersion" to 1, "id" to "sparkling-mutuals", "version" to "3.3.0",
            "environment" to "client", "icon" to "assets/sparkling-mutuals/icon.png",
            "entrypoints" to mapOf("main" to listOf(mapOf("value" to "net.johnceo.sparklingmutuals.SparklingMutuals", "adapter" to "kotlin"))),
            "mixins" to listOf("sparkling-mutuals.mixins.json"), "jars" to listOf(mapOf("file" to hmName), mapOf("file" to moulName)),
            "depends" to mapOf("minecraft" to target.minecraft, "java" to ">=25", "fabricloader" to ">=0.19.5",
                "fabric-api" to "*", "fabric-language-kotlin" to "*"))
        change(metadata, entries)
        entries["fabric.mod.json"] = JsonOutput.toJson(metadata).toByteArray()
        return dir.resolve("sparkling-mutuals-3.3.0+mc${target.minecraft}.jar").also { Files.write(it, zip(entries)) }
    }
    @Test fun completeDistributionHasExactlyThreeVerifiedTargets() {
        val files = targets.map { jar(it) }
        val result = verifyDistribution(files, targets, "3.3.0")
        assertEquals(setOf("26.1.2", "26.2", "26.3"), result.map { it.minecraft }.toSet())
        assertEquals(3, result.size)
        result.forEach { verified ->
            assertEquals("3.3.0", verified.modVersion); assertTrue(verified.size > 0)
            assertEquals(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(verified.path)).joinToString("") { "%02x".format(it) }, verified.sha256)
        }
    }
    @Test fun wrongInternalMinecraftFailsDespiteCorrectFilename() {
        val p = jar(targets[2]) { m, _ -> m["depends"] = mapOf("minecraft" to "26.2", "java" to ">=25", "fabricloader" to ">=0.19.5") }
        assertThrows(IllegalArgumentException::class.java) { verifyJar(p, targets[2], "3.3.0") }
    }
    @Test fun missingAndDuplicateTargetsCannotBecomeACompleteDistribution() {
        val files = targets.map { jar(it) }
        assertThrows(IllegalArgumentException::class.java) { verifyDistribution(files.take(2), targets, "3.3.0") }
        assertThrows(IllegalArgumentException::class.java) { verifyDistribution(listOf(files[0],files[1],files[1]), targets, "3.3.0") }
    }
    @Test fun wrongVersionAndMissingRuntimeResourcesAreRejected() {
        val missing = listOf("net/johnceo/sparklingmutuals/SparklingMutuals.class", "assets/sparkling-mutuals/icon.png",
            "LICENSE_sparkling-mutuals", "licenses/HM-API-LICENSE", "sparkling-mutuals.mixins.json",
            "net/johnceo/sparklingmutuals/mixin/SafariCapsuleMixin.class")
        for (entry in missing) {
            val p = jar(targets[0]) { _, e -> e.remove(entry) }
            assertThrows(IllegalArgumentException::class.java, { verifyJar(p, targets[0], "3.3.0") }, entry)
        }
        val p = jar(targets[0]) { m, _ -> m["version"] = "3.2.0" }
        assertThrows(IllegalArgumentException::class.java) { verifyJar(p, targets[0], "3.3.0") }
    }
    @Test fun sourceOrDevelopmentNamesCannotBeInstalled() {
        val p = jar(targets[0])
        for (suffix in listOf("-sources", "-dev", "-test")) {
            val renamed = Files.copy(p, dir.resolve("sparkling-mutuals-3.3.0+mc26.1.2$suffix.jar"))
            assertThrows(IllegalArgumentException::class.java) { verifyJar(renamed, targets[0], "3.3.0") }
        }
    }
    @Test fun wrongNestedTargetAndEmbeddedDevAuthAreRejected() {
        val p = jar(targets[2]) { _, e -> e["META-INF/jars/hm-api-1.0.5+26.3.jar"] = zip(mapOf("fabric.mod.json" to
            "{\"id\":\"hm-api\",\"version\":\"1.0.5+26.3\",\"depends\":{\"minecraft\":\"~26.2\"}}".toByteArray())) }
        assertThrows(IllegalArgumentException::class.java) { verifyJar(p, targets[2], "3.3.0") }
        val dev = jar(targets[0]) { _, e -> e["META-INF/jars/devauth.jar"] = zip(mapOf("fabric.mod.json" to "{\"id\":\"devauth\",\"version\":\"1.2.2\"}".toByteArray())) }
        assertThrows(IllegalArgumentException::class.java) { verifyJar(dev, targets[0], "3.3.0") }
    }
    @Test fun incompleteInvocationCannotReplaceAPreviousCompleteDistribution() {
        val destination = dir.resolve("distribution")
        Files.createDirectories(destination)
        Files.writeString(destination.resolve("previous.jar"), "keep until a complete replacement is verified")
        val files = targets.map { jar(it) }
        assertThrows(IllegalArgumentException::class.java) { collectDistribution(files.take(2), targets, "3.3.0", destination) }
        assertEquals("keep until a complete replacement is verified", Files.readString(destination.resolve("previous.jar")))
        collectDistribution(files, targets, "3.3.0", destination)
        assertFalse(Files.exists(destination.resolve("previous.jar")))
        assertEquals(3, Files.list(destination).use { s -> s.filter { it.fileName.toString().endsWith(".jar") }.count() })
        assertTrue(Files.exists(destination.resolve("build-manifest.json")))
    }
    @Test fun moulConfigManifestMustIdentifyTheResolvedTargetFamily() {
        for (target in targets) {
            val wrong = jar(target, "26.0")
            assertThrows(IllegalArgumentException::class.java) { verifyJar(wrong, target, "3.3.0") }
        }
    }
    @Test fun pinnedMoulConfig262RetainsItsUpstreamPreReleaseBuildLabel() {
        // modern-26.2:4.8.0 is published with this exact build label; the client/settings smoke validates linkage.
        verifyJar(jar(targets[1], "26.2-pre-2"), targets[1], "3.3.0")
        assertThrows(IllegalArgumentException::class.java) { verifyJar(jar(targets[1], "26.2-pre-1"), targets[1], "3.3.0") }
    }
}
