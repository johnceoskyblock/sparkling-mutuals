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
        TargetSpec("26.1.2", "0.155.3+26.1.2", "1.0.3+26.1"),
        TargetSpec("26.2", "0.161.0+26.2", "1.0.4+26.2"),
        TargetSpec("26.3", "0.163.0+26.3", "1.0.5+26.3"))
    private fun zip(entries: Map<String, ByteArray>): ByteArray = ByteArrayOutputStream().use { bytes ->
        ZipOutputStream(bytes).use { z -> entries.forEach { (name, data) ->
            z.putNextEntry(ZipEntry(name)); z.write(data); z.closeEntry()
        } }; bytes.toByteArray()
    }
    private fun jar(target: TargetSpec, edition: Edition = Edition.FULL,
        change: (MutableMap<String, Any>, MutableMap<String, ByteArray>) -> Unit = { _, _ -> }): Path {
        val hmName = "META-INF/jars/hm-api-${target.hmApi}.jar"
        val entries = mutableMapOf(
            "net/johnceo/sparklingmutuals/SparklingMutuals.class" to byteArrayOf(1),
            "assets/sparkling-mutuals/icon.png" to byteArrayOf(1),
            "LICENSE_sparkling-mutuals" to "CC0".toByteArray(),
            "licenses/HM-API-LICENSE" to "Apache".toByteArray(),
            "sparkling-mutuals.mixins.json" to JsonOutput.toJson(mapOf("required" to true,
                "package" to "net.johnceo.sparklingmutuals.mixin", "client" to listOf("SafariCapsuleMixin"))).toByteArray(),
            "net/johnceo/sparklingmutuals/mixin/SafariCapsuleMixin.class" to byteArrayOf(1),
            hmName to zip(mapOf("fabric.mod.json" to JsonOutput.toJson(mapOf("id" to "hm-api", "version" to target.hmApi,
                "depends" to mapOf("minecraft" to "~${target.minecraft}", "java" to ">=25"))).toByteArray()))
        )
        val metadata = mutableMapOf<String, Any>("schemaVersion" to 1, "id" to "sparkling-mutuals", "version" to "3.3.0",
            "environment" to "client", "icon" to "assets/sparkling-mutuals/icon.png",
            "custom" to mapOf("sparkling-mutuals:edition" to edition.id),
            "entrypoints" to mapOf("main" to listOf(mapOf("value" to "net.johnceo.sparklingmutuals.SparklingMutuals", "adapter" to "kotlin"))),
            "mixins" to listOf("sparkling-mutuals.mixins.json"), "jars" to listOf(mapOf("file" to hmName)),
            "depends" to mapOf("minecraft" to target.minecraft, "java" to ">=25", "fabricloader" to ">=0.19.5",
                "fabric-api" to "*", "fabric-language-kotlin" to "*"))
        if (edition == Edition.STANDARD) entries["net/johnceo/sparklingmutuals/safari/visibility/LocalVisibility.class"] = byteArrayOf(1)
        change(metadata, entries)
        entries["fabric.mod.json"] = JsonOutput.toJson(metadata).toByteArray()
        return dir.resolve(edition.jarName("3.3.0", target.minecraft)).also { Files.write(it, zip(entries)) }
    }
    @Test fun verifyNineJarsAndPreparePublicManifestForAllEditions() {
        val editions = Edition.entries.toSet()
        val matrix = verifyDistribution(editions.flatMap { edition -> targets.map { jar(it, edition) } },
            targets, "3.3.0", editions)
        assertEquals(9, matrix.size)
        val publicEditions = editions
        val output = dir.resolve("public")
        val publicJars = collectDistribution(matrix.filter { it.edition in publicEditions }.map { it.path },
            targets, "3.3.0", output, publicEditions)
        val manifest = output.resolve("build-manifest.json")
        assertEquals(9, publicJars.size)
        assertTrue(Files.readString(manifest).contains("standard"))
        assertTrue(verifyPublicManifest(manifest, "3.3.0", publicJars).size > 0)
        Files.writeString(manifest, "{}")
        assertThrows(IllegalArgumentException::class.java) { verifyPublicManifest(manifest, "3.3.0", publicJars) }
    }
    @Test fun rejectFullAutomationAndUpdaterClassesFromOtherEditions() {
        val target = targets.first()
        val standard = jar(target, Edition.STANDARD) { _, entries ->
            entries["net/johnceo/sparklingmutuals/safari/automation/AutoClicker.class"] = byteArrayOf(1)
        }
        val noUpdate = jar(target, Edition.FULL_NO_AUTO_UPDATE) { _, entries ->
            entries["net/johnceo/sparklingmutuals/config/ReleaseUpdateChecker.class"] = byteArrayOf(1)
        }
        assertThrows(IllegalArgumentException::class.java) { verifyJar(standard, target, "3.3.0", Edition.STANDARD) }
        assertThrows(IllegalArgumentException::class.java) { verifyJar(noUpdate, target, "3.3.0", Edition.FULL_NO_AUTO_UPDATE) }
    }
    @Test fun rejectInstallerAndTransportClassesFromBothNonUpdaterEditions() {
        for (edition in listOf(Edition.STANDARD, Edition.FULL_NO_AUTO_UPDATE)) {
            for (entry in listOf("UpdateInstall.class", "UpdateInstall\$Artifact.class", "UpdateTransport.class", "FullUpdateManifest.class")) {
                val target = targets.first()
                val path = jar(target, edition) { _, entries ->
                    entries["net/johnceo/sparklingmutuals/update/$entry"] = byteArrayOf(1)
                }
                assertThrows(IllegalArgumentException::class.java) { verifyJar(path, target, "3.3.0", edition) }
            }
        }
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
    @Test fun nativeSettingsMustNotBundleTheOldSettingsLibrary() {
        val target=targets.first()
        val bad=jar(target) { metadata,entries ->
            entries["META-INF/jars/moulconfig.jar"]=byteArrayOf(1)
            metadata["jars"]=(metadata["jars"] as List<*>)+mapOf("file" to "META-INF/jars/moulconfig.jar")
        }
        assertThrows(IllegalArgumentException::class.java) { verifyJar(bad,target,"3.3.0") }
    }
    @Test fun targetsMustShipTheSameFeaturesAndResources() {
        for (entry in listOf("net/johnceo/sparklingmutuals/OnlyOneTarget.class", "defaults/settings.properties", "assets/sparkling-mutuals/icon.png")) {
            val files=targets.map { target -> jar(target) { _,entries ->
                if(target.minecraft=="26.3") entries[entry]=byteArrayOf(9)
            } }
            assertThrows(IllegalArgumentException::class.java, { verifyDistribution(files,targets,"3.3.0") },entry)
        }
    }
}
