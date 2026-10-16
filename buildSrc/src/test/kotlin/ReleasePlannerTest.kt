package buildlogic

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.nio.file.Path

class ReleasePlannerTest {
    private val files = releaseFiles()
    @Test fun tagMustMatchInternalVersion() {
        assertThrows(IllegalArgumentException::class.java) { planRelease("v3.3.1", "3.3.0", files, null) }
        assertEquals(ReleaseDecision.CREATE_DRAFT, planRelease("v3.3.0", "3.3.0", files, null).decision)
        assertThrows(IllegalArgumentException::class.java) { planRelease("v3.3.0", "3.3.0", files.take(2), null) }
    }
    @Test fun maintainerNotesSurviveDraftRerun() {
        val result = planRelease("v3.3.0", "3.3.0", files, ExistingRelease(true, "Maintainer notes\nwith detail", emptyList()))
        assertTrue(result.preserveBody); assertEquals(ReleaseDecision.UPDATE_DRAFT, result.decision)
    }
    @Test fun publishedAssetsAreImmutable() {
        val existing = ExistingRelease(false, "notes", files.mapIndexed { i, f -> ReleaseAsset(i.toLong(), f.path.fileName.toString(), f.size, "sha256:${f.sha256}") })
        assertEquals(ReleaseDecision.PUBLISHED_NOOP, planRelease("v3.3.0", "3.3.0", files, existing).decision)
        for (changed in listOf(existing.copy(assets=existing.assets.dropLast(1)), existing.copy(assets=existing.assets.map { it.copy(sha256=null) }),
            existing.copy(assets=existing.assets.map { it.copy(sha256="b".repeat(64)) }))) {
            assertThrows(IllegalArgumentException::class.java) { planRelease("v3.3.0", "3.3.0", files, changed) }
        }
    }
    @Test fun unexpectedOrDuplicateJarAssetsStopEvenDraftPublication() {
        val wrong = ReleaseAsset(99, "another-mod.jar", 12, "a".repeat(64))
        assertThrows(IllegalArgumentException::class.java) { planRelease("v3.3.0", "3.3.0", files, ExistingRelease(true, "", listOf(wrong))) }
        val matching = ReleaseAsset(1, files[0].path.fileName.toString(), files[0].size, files[0].sha256)
        assertThrows(IllegalArgumentException::class.java) { planRelease("v3.3.0", "3.3.0", files, ExistingRelease(true, "", listOf(matching,matching.copy(id=2)))) }
        assertEquals(ReleaseDecision.UPDATE_DRAFT, planRelease("v3.3.0", "3.3.0", files, ExistingRelease(true, "", listOf(wrong.copy(name="readme.txt")))).decision)
    }
}

internal fun releaseFiles(): List<VerifiedJar> = listOf("26.1.2","26.2","26.3").map {
    VerifiedJar(Path.of("sparkling-mutuals-3.3.0+mc$it.jar"), it, "3.3.0", 123, "a".repeat(64))
}
