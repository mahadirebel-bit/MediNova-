package com.example.data.api

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

object GeminiClient {
    private const val TAG = "GeminiClient"
    private const val MODEL_NAME = "gemini-3.5-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_NAME:generateContent"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private fun Bitmap.toBase64(): String {
        val outputStream = ByteArrayOutputStream()
        // Compress the bitmap to fit inside Gemini limits smoothly (80% quality JPEG is perfect)
        compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    suspend fun analyzeReport(bitmap: Bitmap, prompt: String): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e(TAG, "Gemini API key is not configured!")
            return@withContext "ERROR_KEY_MISSING"
        }

        try {
            val base64Image = bitmap.toBase64()

            // Construct Gemini Request Body using JSONObject
            val requestJson = JSONObject().apply {
                val contentsArray = JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        val partsArray = JSONArray().apply {
                            // Text part
                            val textPart = JSONObject().put("text", prompt)
                            put(textPart)

                            // Image part
                            val imagePart = JSONObject().apply {
                                val inlineDataObj = JSONObject().apply {
                                    put("mimeType", "image/jpeg")
                                    put("data", base64Image)
                                }
                                put("inlineData", inlineDataObj)
                            }
                            put(imagePart)
                        }
                        put("parts", partsArray)
                    }
                    put(contentObj)
                }
                put("contents", contentsArray)

                // Force JSON output in generation config
                val genConfig = JSONObject().apply {
                    put("responseMimeType", "application/json")
                }
                put("generationConfig", genConfig)
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = requestJson.toString().toRequestBody(mediaType)

            val url = "$BASE_URL?key=$apiKey"
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (!response.isSuccessful || responseBody == null) {
                Log.e(TAG, "Request failed: ${response.code} - $responseBody")
                return@withContext "ERROR_API_FAILED: ${response.code}"
            }

            Log.d(TAG, "Gemini Response: $responseBody")

            // Parse response to extract the generated text
            val rootJson = JSONObject(responseBody)
            val candidates = rootJson.optJSONArray("candidates")
            if (candidates != null && candidates.length() > 0) {
                val firstCandidate = candidates.getJSONObject(0)
                val responseContent = firstCandidate.optJSONObject("content")
                if (responseContent != null) {
                    val parts = responseContent.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        return@withContext parts.getJSONObject(0).optString("text")
                    }
                }
            }
            return@withContext "ERROR_NO_CONTENT"
        } catch (e: Exception) {
            Log.e(TAG, "Error in analyzeReport: ${e.message}", e)
            return@withContext "ERROR_EXCEPTION: ${e.message}"
        }
    }

    suspend fun generateText(prompt: String): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e(TAG, "Gemini API key is not configured!")
            return@withContext "ERROR_KEY_MISSING"
        }

        try {
            // Construct Gemini Request Body using JSONObject (text only)
            val requestJson = JSONObject().apply {
                val contentsArray = JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        val partsArray = JSONArray().apply {
                            val textPart = JSONObject().put("text", prompt)
                            put(textPart)
                        }
                        put("parts", partsArray)
                    }
                    put(contentObj)
                }
                put("contents", contentsArray)

                // Force JSON output
                val genConfig = JSONObject().apply {
                    put("responseMimeType", "application/json")
                }
                put("generationConfig", genConfig)
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = requestJson.toString().toRequestBody(mediaType)

            val url = "$BASE_URL?key=$apiKey"
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (!response.isSuccessful || responseBody == null) {
                Log.e(TAG, "Request failed: ${response.code} - $responseBody")
                return@withContext "ERROR_API_FAILED: ${response.code}"
            }

            Log.d(TAG, "Gemini Response: $responseBody")

            val rootJson = JSONObject(responseBody)
            val candidates = rootJson.optJSONArray("candidates")
            if (candidates != null && candidates.length() > 0) {
                val firstCandidate = candidates.getJSONObject(0)
                val responseContent = firstCandidate.optJSONObject("content")
                if (responseContent != null) {
                    val parts = responseContent.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        return@withContext parts.getJSONObject(0).optString("text")
                    }
                }
            }
            return@withContext "ERROR_NO_CONTENT"
        } catch (e: Exception) {
            Log.e(TAG, "Error in generateText: ${e.message}", e)
            return@withContext "ERROR_EXCEPTION: ${e.message}"
        }
    }
}
