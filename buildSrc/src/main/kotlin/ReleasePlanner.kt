package buildlogic

data class ReleaseAsset(val id: Long, val name: String, val size: Long, val sha256: String?)
data class ExistingRelease(val draft: Boolean, val body: String?, val assets: List<ReleaseAsset>)
enum class ReleaseDecision { CREATE_DRAFT, UPDATE_DRAFT, PUBLISHED_NOOP }
data class ReleasePlan(val decision: ReleaseDecision, val preserveBody: Boolean)
data class RemoteRelease(val id: Long, val state: ExistingRelease)
interface ReleaseApi {
    fun find(tag: String): RemoteRelease?
    fun createDraft(tag: String, body: String): RemoteRelease
    fun generatedNotes(tag: String): String
    fun updateBody(id: Long, body: String)
    fun deleteAsset(id: Long)
    fun upload(id: Long, file: VerifiedJar): ReleaseAsset
    fun publish(id: Long)
}
fun planRelease(tag: String, modVersion: String, files: List<VerifiedJar>, existing: ExistingRelease?): ReleasePlan {
    require(modVersion.matches(Regex("[0-9]+\\.[0-9]+\\.[0-9]+(?:-[A-Za-z0-9.-]+)?")) && tag == "v$modVersion") { "Tag must match the internal mod version" }
    require(files.size == 3 && files.map { it.minecraft }.toSet() == setOf("26.1.2", "26.2", "26.3")) { "Publication requires exactly three verified targets" }
    files.forEach {
        require(it.modVersion == modVersion && it.path.fileName.toString() == "sparkling-mutuals-$modVersion+mc${it.minecraft}.jar" &&
            it.size > 0 && it.sha256.matches(Regex("[a-f0-9]{64}"))) { "Invalid verified target: ${it.minecraft}" }
    }
    if (existing == null) return ReleasePlan(ReleaseDecision.CREATE_DRAFT, false)
    val jars = existing.assets.filter { it.name.endsWith(".jar", ignoreCase = true) }
    val names = files.map { it.path.fileName.toString() }.toSet()
    require(jars.all { it.name in names } && jars.map { it.name }.distinct().size == jars.size) { "Unexpected or duplicate JAR assets; review them manually" }
    if (!existing.draft) {
        require(files.all { file -> jars.singleOrNull { it.name == file.path.fileName.toString() }?.matches(file) == true }) {
            "Published release differs from the verified distribution; it will not be modified"
        }
        return ReleasePlan(ReleaseDecision.PUBLISHED_NOOP, !existing.body.isNullOrBlank())
    }
    return ReleasePlan(ReleaseDecision.UPDATE_DRAFT, !existing.body.isNullOrBlank())
}

private fun ReleaseAsset.matches(file: VerifiedJar) = size == file.size && sha256?.removePrefix("sha256:") == file.sha256

/** Only a complete, verified draft can cross the final publish boundary. */
fun publishVerifiedRelease(api: ReleaseApi, tag: String, modVersion: String, files: List<VerifiedJar>): ReleaseDecision {
    planRelease(tag, modVersion, files, null)
    var remote = api.find(tag)
    val initial = planRelease(tag, modVersion, files, remote?.state)
    if (initial.decision == ReleaseDecision.PUBLISHED_NOOP) return initial.decision
    if (remote == null) remote = api.createDraft(tag, api.generatedNotes(tag))
    val releaseId = remote.id
    fun draft(): RemoteRelease {
        val current = requireNotNull(api.find(tag)) { "Release disappeared during staging" }
        require(current.id == releaseId && current.state.draft) { "Release changed or was published during staging" }
        planRelease(tag, modVersion, files, current.state)
        return current
    }
    if (draft().state.body.isNullOrBlank()) {
        val notes = api.generatedNotes(tag)
        // Generating notes can take time; do not overwrite notes written meanwhile.
        if (draft().state.body.isNullOrBlank()) api.updateBody(releaseId, notes)
    }
    for (file in files) {
        val asset = draft().state.assets.singleOrNull { it.name == file.path.fileName.toString() }
        if (asset?.matches(file) == true) continue
        if (asset != null) { draft(); api.deleteAsset(asset.id) }
        draft()
        require(api.upload(releaseId, file).matches(file)) { "Uploaded asset digest/size mismatch" }
    }
    val ready = draft()
    require(files.all { file -> ready.state.assets.singleOrNull { it.name == file.path.fileName.toString() }?.matches(file) == true }) {
        "Draft does not contain all three matching uploads"
    }
    api.publish(releaseId)
    val published = requireNotNull(api.find(tag)) { "Published release could not be read back" }
    require(published.id == releaseId && !published.state.draft) { "Release publication could not be confirmed" }
    planRelease(tag, modVersion, files, published.state)
    return initial.decision
}
