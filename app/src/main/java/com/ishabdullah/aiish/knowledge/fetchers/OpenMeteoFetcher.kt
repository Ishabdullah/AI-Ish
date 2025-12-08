package com.ishabdullah.aiish.knowledge.fetchers

import com.ishabdullah.aiish.domain.models.KnowledgeResult
import com.google.gson.JsonParser
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class OpenMeteoFetcher : BaseFetcher() {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    override suspend fun fetch(query: String): KnowledgeResult {
        return try {
            // For demo, use fixed coordinates (NYC: 40.7128, -74.0060)
            // In production, extract location from query or use GPS
            val lat = 40.7128
            val lon = -74.0060

            val url = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current_weather=true"

            val request = Request.Builder()
                .url(url)
                .build()

            val response = client.newCall(request).execute()
            val jsonString = response.body?.string() ?: "{}"
            val json = JsonParser.parseString(jsonString).asJsonObject

            if (json.has("current_weather")) {
                val weather = json.getAsJsonObject("current_weather")
                val temp = weather.get("temperature")?.asDouble ?: 0.0
                val windspeed = weather.get("windspeed")?.asDouble ?: 0.0

                val content = "Current weather in NYC: ${temp}°C, wind speed: ${windspeed} km/h"

                createResult(
                    source = "OpenMeteo",
                    content = content,
                    confidence = 0.9f
                )
            } else {
                createResult("OpenMeteo", "Weather data unavailable", 0.3f)
            }
        } catch (e: Exception) {
            createResult("OpenMeteo", "Failed to fetch weather: ${e.message}", 0.1f)
        }
    }
}
