package net.johnceo.sparklingmutuals.skyblock.market

import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/** Public, anonymous reference history. No cookies, account IDs or order content are transmitted. */
internal class FinanceHistoryTransport(
    private val clock: ()->Long=System::currentTimeMillis,
    private val client: HttpClient=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build(),
): HistoryTransport {
    override fun request(job: HistoryJob,reply: (HistoryReply)->Unit): MarketCancel {
        val request=HttpRequest.newBuilder(uri(job,clock())).timeout(Duration.ofSeconds(20))
            .header("User-Agent","Sparkling Mutuals experimental shard ranges").GET().build()
        val pending=client.sendAsync(request,HttpResponse.BodyHandler {LimitedStringSubscriber(2*1024*1024)})
        pending.whenComplete {response,error ->
            reply(if(error!=null)HistoryReply(0,"") else HistoryReply(response.statusCode(),response.body(),
                retryAfter(response.headers().firstValue("Retry-After").orElse(null),clock())))
        }
        return MarketCancel {pending.cancel(true)}
    }

    companion object {
        fun uri(job: HistoryJob,now: Long): URI {
            require(SaleMarketProducts.historyId(job.productId))
            val until=now/60000*60
            val from=until-(if(job.part==HistoryPart.RECENT)2 else 8)*86400L
            val resolution=if(job.part==HistoryPart.RECENT)"1" else "15"
            val json="""{"0":{"json":{"symbol":"bz!a!${job.productId}","resolution":"$resolution","from":$from,"to":$until}}}"""
            return URI.create("https://skyblock.finance/api/trpc/tradingview.getBars2?batch=1&input="+
                URLEncoder.encode(json,Charsets.UTF_8))
        }
        fun retryAfter(value: String?,now: Long): Long? {
            if(value==null)return null
            value.toLongOrNull()?.let {seconds ->
                return if(seconds<0)null else if(seconds>Long.MAX_VALUE/1000)Long.MAX_VALUE else seconds*1000
            }
            return runCatching {(ZonedDateTime.parse(value,DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toEpochMilli()-now).coerceAtLeast(0)}.getOrNull()
        }
    }
}
