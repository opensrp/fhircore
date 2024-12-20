package org.smartregister.fhircore.quest.ui.speechtoform

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONObject
import java.io.IOException

class GeminiClient(private val apiKey: String) {
    private val client = OkHttpClient()
    private val baseUrl = "https://generativeai.googleapis.com/v1/models/gemini-1.5-flash:generateContent"

    fun generateContent(prompt: String): String {
        val requestBody = JSONObject().put("prompt", prompt).toString()

        val request = Request.Builder()
            .url(baseUrl)
            .addHeader("Authorization", "Bearer $apiKey")
            .post(RequestBody.create("application/json".toMediaType(), requestBody))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Unexpected code $response")

            val jsonResponse = JSONObject(response.body?.string() ?: "")
            return jsonResponse.getString("text")
        }
    }
}
