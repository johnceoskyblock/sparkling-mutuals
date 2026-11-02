package net.johnceo.sparklingmutuals.skyblock

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import net.fabricmc.loader.api.FabricLoader
import net.johnceo.sparklingmutuals.history.BazaarPrices
import net.minecraft.client.Minecraft
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.time.Duration

internal class MarketRefreshWindow {
    private var attemptedAt: Long?=null
    private var pending=false
    fun begin(now: Long): Boolean {
        if(pending || attemptedAt?.let { now-it<3*60*60_000L }==true) return false
        attemptedAt=now;pending=true;return true
    }
    fun finished() { pending=false }
    fun restored(at: Long) { attemptedAt=at }
}
data class MarketQuote(val coins: Double,val source: String)
object ValueMarket {
    private val log=LoggerFactory.getLogger("sparkling-mutuals/item-values")
    private val http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build()
    private val refresh=MarketRefreshWindow()
    private var loaded=false
    @Volatile var prices: Map<String,MarketQuote> = emptyMap()
        private set
    var revision=0
        private set
    private class Saved(var at: Long=0,var source: String="",var values: Map<String,Double> = emptyMap())
    private val file get()=FabricLoader.getInstance().configDir.resolve("sparkling-mutuals-market-prices.json")
    private fun restore() {
        if(loaded) return
        loaded=true
        runCatching {
            if(Files.exists(file)) {
                val saved=Gson().fromJson(Files.readString(file),Saved::class.java)
                prices=saved.values.filterValues { it.isFinite() && it>0 }.mapValues { MarketQuote(it.value,saved.source) }
                if(saved.at<=System.currentTimeMillis()) refresh.restored(saved.at)
                revision++
            }
        }.onFailure { log.warn("Couldn't restore public item prices: {}",it.message) }
    }
    fun ensureFresh() {
        BazaarPrices.ensureFresh()
        restore()
        if(!refresh.begin(System.currentTimeMillis())) return
        fetch("https://moulberry.codes/lowestbin.json") { values ->
            if(values.isNotEmpty()) accept(values,"Lowest BIN")
            else fetch("https://raw.githubusercontent.com/SkyHelperBot/Prices/main/pricesV2.json") { fallback -> accept(fallback,"Market estimate") }
        }
    }
    private fun fetch(url: String,done: (Map<String,Double>)->Unit) {
        val request=HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(20))
            .header("User-Agent","Sparkling Mutuals").GET().build()
        http.sendAsync(request,HttpResponse.BodyHandlers.ofString()).whenComplete { response,error ->
            val data=runCatching {
                check(error==null && response.statusCode()==200)
                decode(JsonParser.parseString(response.body()).asJsonObject)
            }.getOrDefault(emptyMap())
            // Keep callbacks and all published snapshot changes on the client thread.
            Minecraft.getInstance().execute { done(data) }
        }
    }
    internal fun decode(body: JsonObject): Map<String,Double> = body.entrySet().mapNotNull { (id,value) ->
        val coins=runCatching { value.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isNumber }?.asDouble }.getOrNull()
        coins?.takeIf { it.isFinite() && it>0 }?.let { id to it }
    }.toMap()
    private fun accept(values: Map<String,Double>,source: String) {
        refresh.finished()
        if(values.isEmpty()) { log.warn("Public auction price sources unavailable; retaining earlier quotes");return }
        prices=values.mapValues { MarketQuote(it.value,source) };revision++
        val saved=Saved(System.currentTimeMillis(),source,values)
        // No inventory contents, identifiers or account data are sent or saved here.
        java.util.concurrent.CompletableFuture.runAsync {
            runCatching {
                Files.createDirectories(file.parent)
                val temp=Files.createTempFile(file.parent,"sparkling-mutuals-market-",".tmp")
                try {
                    Files.writeString(temp,Gson().toJson(saved))
                    Files.move(temp,file,java.nio.file.StandardCopyOption.REPLACE_EXISTING)
                } finally { Files.deleteIfExists(temp) }
            }.onFailure { log.warn("Couldn't cache public item prices: {}",it.message) }
        }
    }
}
