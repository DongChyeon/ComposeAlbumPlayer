package com.dongchyeon.core.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Audius API의 동적 BASE_URL을 관리하는 Provider
 *
 * Audius는 여러 API 호스트를 운영하며, https://api.audius.co 를 호출하면
 * 사용 가능한 호스트 목록을 반환합니다.
 *
 * 응답 예시:
 * {
 *   "data": ["https://discoveryprovider.audius.co", ...],
 *   "env": "prod",
 *   ...
 * }
 */
@Singleton
class BaseUrlProvider @Inject constructor() {

    companion object {
        private const val TAG = "BaseUrlProvider"
        private const val DISCOVERY_URL = "https://api.audius.co"
        const val DEFAULT_BASE_URL = "https://api.audius.co/"
    }

    @Volatile
    private var _baseUrl: String = DEFAULT_BASE_URL

    val baseUrl: String
        get() = _baseUrl

    /**
     * Audius Discovery API를 호출하여 사용 가능한 호스트를 가져온 후
     * 첫 번째 호스트를 BASE_URL로 설정
     */
    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        try {
            val client = OkHttpClient.Builder()
                .build()

            val request = Request.Builder()
                .url(DISCOVERY_URL)
                .get()
                .build()

            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val body = response.body?.string()
                if (body != null) {
                    val json = Json { ignoreUnknownKeys = true }
                    val jsonObject = json.parseToJsonElement(body).jsonObject
                    val dataArray = jsonObject["data"]?.jsonArray

                    if (dataArray != null && dataArray.isNotEmpty()) {
                        val selectedHost = dataArray.first().jsonPrimitive.content
                        _baseUrl = if (selectedHost.endsWith("/")) selectedHost else "$selectedHost/"
                        Log.d(TAG, "BASE_URL updated: $_baseUrl")
                        return@withContext true
                    }
                }
            }

            Log.w(TAG, "Failed to fetch discovery hosts, using default: $DEFAULT_BASE_URL")
            false
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching discovery hosts: ${e.message}", e)
            false
        }
    }
}
