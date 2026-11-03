package net.johnceo.sparklingmutuals.skyblock.market

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.http.HttpResponse
import java.nio.ByteBuffer
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import java.util.concurrent.Flow

internal object BazaarQuoteParsing {
    fun parse(json: String, receivedAt: Long): QuoteSnapshot? = runCatching {
        val root = JsonParser.parseString(json).asJsonObject
        if (root.get("success")?.asBoolean != true) return null
        val at = root.get("lastUpdated")?.asLong ?: return null
        if (at <= 0 || at > receivedAt + 60_000L) return null
        val products = root.getAsJsonObject("products") ?: return null
        val quotes = linkedMapOf<String, MarketQuote>()
        val prices = linkedMapOf<String, DoubleArray>()
        for ((id, element) in products.entrySet()) {
            val product = runCatching { element.asJsonObject }.getOrNull() ?: continue
            runCatching {
                product.getAsJsonObject("quick_status")?.let { status ->
                    val sell = status.get("sellPrice").asDouble
                    val buy = status.get("buyPrice").asDouble
                    if (sell.isFinite() && sell >= 0 && buy.isFinite() && buy >= 0)
                        prices[id] = doubleArrayOf(sell,buy)
                }
            }
            val asks = orders(product,"buy_summary")
            val bids = orders(product,"sell_summary")
            val ask = asks.minOrNull() ?: continue
            val bid = bids.maxOrNull() ?: continue
            if (bid <= ask) quotes[id] = MarketQuote(id,ask,bid,at)
        }
        QuoteSnapshot(at,quotes.toMap(),prices.toMap())
    }.getOrNull()

    private fun orders(product: JsonObject, side: String): List<Double> = runCatching {
        product.getAsJsonArray(side)?.map { item ->
            val order = item.asJsonObject
            require(listOf("pricePerUnit","amount","orders").all {order.getAsJsonPrimitive(it).isNumber})
            val price = order.get("pricePerUnit").asDouble
            val amount = order.get("amount").asDouble
            val count = order.get("orders").asBigDecimal.intValueExact()
            require(price.isFinite() && price>0 && amount.isFinite() && amount>0 && count>0)
            price
        }.orEmpty()
    }.getOrDefault(emptyList())
}

/** Cap bytes while receiving, before copying a chunk; HTTP failure never publishes a partial body. */
internal class LimitedStringSubscriber(private val limit: Int) : HttpResponse.BodySubscriber<String> {
    private val result = CompletableFuture<String>()
    private val bytes = ByteArrayOutputStream()
    private var subscription: Flow.Subscription? = null
    init { require(limit > 0) }
    override fun getBody(): CompletionStage<String> = result
    override fun onSubscribe(value: Flow.Subscription) {
        if (subscription != null || result.isDone) { value.cancel(); return }
        subscription = value
        result.whenComplete { _, error -> if (error != null) value.cancel() }
        value.request(1)
    }
    override fun onNext(items: List<ByteBuffer>) {
        if (result.isDone) return
        for (buffer in items) {
            val count = buffer.remaining()
            if (count > limit - bytes.size()) { onError(IOException("Market response exceeds byte limit")); return }
            val copy = ByteArray(count)
            buffer.get(copy)
            bytes.write(copy)
        }
        subscription?.request(1)
    }
    override fun onError(error: Throwable) { result.completeExceptionally(error) }
    override fun onComplete() { result.complete(bytes.toString(Charsets.UTF_8)) }
}
