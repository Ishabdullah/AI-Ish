package com.ishabdullah.aiish.knowledge.fetchers

import com.ishabdullah.aiish.domain.models.KnowledgeResult
import com.google.gson.JsonParser
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class WikipediaFetcher : BaseFetcher() {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    override suspend fun fetch(query: String): KnowledgeResult {
        return try {
            val url = "https://en.wikipedia.org/api/rest_v1/page/summary/${query.replace(" ", "_")}"

            val request = Request.Builder()
                .url(url)
                .build()

            val response = client.newCall(request).execute()
            val jsonString = response.body?.string() ?: "{}"
            val json = JsonParser.parseString(jsonString).asJsonObject

            val extract = json.get("extract")?.asString ?: "No summary available"

            createResult(
                source = "Wikipedia",
                content = extract,
                confidence = if (extract.isNotEmpty() && extract != "No summary available") 0.9f else 0.3f
            )
        } catch (e: Exception) {
            createResult("Wikipedia", "Failed to fetch: ${e.message}", 0.1f)
        }
    }
}
