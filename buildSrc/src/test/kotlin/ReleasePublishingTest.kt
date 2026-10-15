package buildlogic

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ReleasePublishingTest {
    private class FakeApi(var release: RemoteRelease? = null): ReleaseApi {
        var writes=0; var publications=0; var uploads=0; var failSecond=false; var reads=0
        val deleted = mutableListOf<Long>()
        var onRead: ((FakeApi) -> Unit)? = null
        override fun find(tag: String): RemoteRelease? { reads++; onRead?.invoke(this); return release }
        override fun createDraft(tag: String, body: String): RemoteRelease {
            writes++; return RemoteRelease(1, ExistingRelease(true, body, emptyList())).also { release=it }
        }
        override fun generatedNotes(tag: String) = "Generated notes for $tag"
        override fun updateBody(id: Long, body: String) { writes++; release=release!!.copy(state=release!!.state.copy(body=body)) }
        override fun deleteAsset(id: Long) { writes++; deleted += id; release=release!!.copy(state=release!!.state.copy(assets=release!!.state.assets.filter { it.id!=id })) }
        override fun upload(id: Long, file: VerifiedJar): ReleaseAsset {
            writes++; uploads++
            if(failSecond && uploads==2) throw java.io.IOException("Upload failed")
            val asset=ReleaseAsset(uploads.toLong(),file.path.fileName.toString(),file.size,file.sha256)
            release=release!!.copy(state=release!!.state.copy(assets=release!!.state.assets+asset)); return asset
        }
        override fun publish(id: Long) { writes++; publications++; release=release!!.copy(state=release!!.state.copy(draft=false)) }
    }
    @Test fun partialUploadNeverPublishesAndRerunFinishesOneCompleteRelease() {
        val api=FakeApi(); api.failSecond=true
        assertThrows(java.io.IOException::class.java) { publishVerifiedRelease(api,"v3.3.0","3.3.0",releaseFiles()) }
        assertTrue(api.release!!.state.draft); assertEquals(0,api.publications)
        api.failSecond=false
        publishVerifiedRelease(api,"v3.3.0","3.3.0",releaseFiles())
        assertFalse(api.release!!.state.draft); assertEquals(3,api.release!!.state.assets.size); assertEquals(1,api.publications)
        val writes=api.writes
        assertEquals(ReleaseDecision.PUBLISHED_NOOP,publishVerifiedRelease(api,"v3.3.0","3.3.0",releaseFiles()))
        assertEquals(writes,api.writes)
    }
    @Test fun existingWrittenNotesAndUnrelatedNonJarAssetsArePreserved() {
        val api=FakeApi(RemoteRelease(1,ExistingRelease(true,"My release description",listOf(ReleaseAsset(88,"notes.txt",2,null)))))
        publishVerifiedRelease(api,"v3.3.0","3.3.0",releaseFiles())
        assertEquals("My release description",api.release!!.state.body)
        assertTrue(api.release!!.state.assets.any {it.id==88L}); assertEquals(1,api.publications)
    }
    @Test fun incompleteOrMismatchedPublishedReleaseCannotBeMutated() {
        val api=FakeApi(RemoteRelease(1,ExistingRelease(false,"Published notes",emptyList())))
        assertThrows(IllegalArgumentException::class.java) {publishVerifiedRelease(api,"v3.3.0","3.3.0",releaseFiles())}
        assertEquals(0,api.writes); assertEquals(0,api.publications)
        val empty=FakeApi()
        assertThrows(IllegalArgumentException::class.java) {publishVerifiedRelease(empty,"v3.3.0","3.3.0",releaseFiles().take(2))}
        assertEquals(0,empty.writes)
        assertEquals(0,empty.reads)
    }
    @Test fun draftRerunReusesMatchingAssetsAndReplacesOnlyOneStaleOwnedJar() {
        val files = releaseFiles()
        val assets = files.mapIndexed { i, f -> ReleaseAsset(10L+i, f.path.fileName.toString(), f.size,
            if (i == 1) "b".repeat(64) else f.sha256) } + ReleaseAsset(99,"notes.txt",8,null)
        val api = FakeApi(RemoteRelease(1,ExistingRelease(true,"Written notes",assets)))
        publishVerifiedRelease(api,"v3.3.0","3.3.0",files)
        assertEquals(listOf(11L),api.deleted); assertEquals(1,api.uploads)
        assertTrue(api.release!!.state.assets.any {it.id==99L})
        assertEquals("Written notes",api.release!!.state.body)
    }
    @Test fun releasePublishedDuringStagingCannotReceiveMoreWrites() {
        val api = FakeApi(RemoteRelease(1,ExistingRelease(true,"Written notes",emptyList())))
        api.onRead = { if(it.reads == 3) it.release = it.release!!.copy(state=it.release!!.state.copy(draft=false)) }
        assertThrows(IllegalArgumentException::class.java) { publishVerifiedRelease(api,"v3.3.0","3.3.0",releaseFiles()) }
        assertEquals(0,api.writes); assertEquals(0,api.publications)
    }
}
