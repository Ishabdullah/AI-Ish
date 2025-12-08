package com.ishabdullah.aiish.knowledge.fetchers

import com.ishabdullah.aiish.domain.models.KnowledgeResult
import com.google.gson.JsonParser
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class CoinGeckoFetcher : BaseFetcher() {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    override suspend fun fetch(query: String): KnowledgeResult {
        return try {
            // Extract coin name from query
            val coinId = query.lowercase()
                .replace(Regex("\\b(price|of|crypto|coin|what|is|the)\\b"), "")
                .trim()
                .replace(" ", "-")

            val url = "https://api.coingecko.com/api/v3/simple/price?ids=$coinId&vs_currencies=usd&include_24hr_change=true"

            val request = Request.Builder()
                .url(url)
                .build()

            val response = client.newCall(request).execute()
            val jsonString = response.body?.string() ?: "{}"
            val json = JsonParser.parseString(jsonString).asJsonObject

            if (json.has(coinId)) {
                val data = json.getAsJsonObject(coinId)
                val price = data.get("usd")?.asDouble ?: 0.0
                val change = data.get("usd_24h_change")?.asDouble ?: 0.0

                val content = "$coinId is currently $${String.format("%.2f", price)} " +
                             "(24h change: ${String.format("%.2f", change)}%)"

                createResult(
                    source = "CoinGecko",
                    content = content,
                    confidence = 0.95f
                )
            } else {
                createResult("CoinGecko", "Crypto price not found for: $coinId", 0.2f)
            }
        } catch (e: Exception) {
            createResult("CoinGecko", "Failed to fetch crypto price: ${e.message}", 0.1f)
        }
    }
}
