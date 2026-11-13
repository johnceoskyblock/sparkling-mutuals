package net.johnceo.sparklingmutuals.alerts

import java.util.concurrent.CompletableFuture
import net.johnceo.sparklingmutuals.JoinWelcomeState
import net.johnceo.sparklingmutuals.api.ApiFailure
import net.johnceo.sparklingmutuals.api.ApiFailureKind
import net.johnceo.sparklingmutuals.api.ApiProvider
import net.johnceo.sparklingmutuals.api.HypixelApi
import net.johnceo.sparklingmutuals.commands.SafariLookup
import net.johnceo.sparklingmutuals.config.ConfigManager
import net.minecraft.client.Minecraft

/** One player at a time, at most once per day each; no API work runs on the game thread. */
internal object WarpListRefresh {
    private data class Result(val uuid: String?,val discoveries: Set<String>?,val pause: Boolean=false)
    private data class Lookup(val name: String,val revision: Long,val provider: ApiProvider,val job: CompletableFuture<Result>)
    private var pending: Lookup?=null
    private var nextAt=0L
    fun tick(client: Minecraft,now: Long) {
        pending?.let { lookup ->
            if(!lookup.job.isDone) return
            pending=null
            val result=runCatching { lookup.job.join() }.getOrNull()
            if(lookup.revision==WarpList.loadRevision && lookup.provider==HypixelApi.provider) {
                if(result?.discoveries!=null) WarpList.removeAcquired(lookup.name,result.discoveries)
                // Failed/invalid IGNs do not loop on every login; try again at the next daily refresh.
                WarpList.checked(lookup.name,now,result?.uuid)
                ConfigManager.save()
            }
            if(result?.pause==true) nextAt=maxOf(nextAt,now+300_000)
        }
        if(now<nextAt || client.connection==null || client.player==null ||
            !JoinWelcomeState.hypixel(client.currentServer?.ip)) return
        val provider=HypixelApi.provider
        if(!provider.enabled) return
        val name=WarpList.players().firstOrNull { WarpList.refreshDue(it,now) }
        nextAt=now+if(name==null) 5_000 else 2_000
        if(name==null) return
        val knownUuid=WarpList.uuid(name)
        pending=Lookup(name,WarpList.loadRevision,provider,CompletableFuture.supplyAsync {
            var uuid=knownUuid
            try {
                check(provider==HypixelApi.provider)
                uuid=uuid ?: HypixelApi.getPlayerUuid(name)
                val found=SafariLookup.discoveriesOrEmpty {
                    val profile=HypixelApi.getCurrentProfileUuid(uuid, fresh=true)
                    HypixelApi.getSparklingCritters(uuid,profile)
                }
                check(provider==HypixelApi.provider)
                Result(uuid,found)
            } catch(failure: ApiFailure) {
                Result(uuid,null,failure.kind in setOf(ApiFailureKind.RATE_LIMIT,ApiFailureKind.INVALID_KEY))
            } catch(_: Exception) { Result(uuid,null) }
        })
    }
}
