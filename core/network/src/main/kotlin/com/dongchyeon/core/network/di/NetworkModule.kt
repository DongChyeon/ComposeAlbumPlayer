package com.dongchyeon.core.network.di

import android.util.Log
import com.dongchyeon.core.network.HostSelectionInterceptor
import com.dongchyeon.core.network.api.AlbumApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    // 초기 BASE_URL (HostSelectionInterceptor가 동적으로 교체함)
    private const val BASE_URL = "https://api.audius.co/"

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        hostSelectionInterceptor: HostSelectionInterceptor,
    ): OkHttpClient {
        // JSON Pretty Print를 위한 로깅 전용 Json 인스턴스
        val prettyJson = Json {
            prettyPrint = true
            prettyPrintIndent = "  "
        }

        val loggingInterceptor = HttpLoggingInterceptor { message ->
            // JSON 형식인지 확인 후 pretty print 적용
            if (message.startsWith("{") || message.startsWith("[")) {
                runCatching {
                    val jsonElement = prettyJson.parseToJsonElement(message)
                    val prettyMessage = prettyJson.encodeToString(JsonElement.serializer(), jsonElement)
                    // Logcat 라인 제한(4000자)을 고려하여 줄 단위로 출력
                    prettyMessage.lines().forEach { line ->
                        Log.d("API", line)
                    }
                }.getOrElse {
                    Log.d("API", message)
                }
            } else {
                Log.d("API", message)
            }
        }.apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        return OkHttpClient.Builder()
            .addInterceptor(hostSelectionInterceptor) // 동적 호스트 선택
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        json: Json,
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(
                json.asConverterFactory("application/json".toMediaType()),
            )
            .build()
    }

    @Provides
    @Singleton
    fun provideAlbumApiService(retrofit: Retrofit): AlbumApiService {
        return retrofit.create(AlbumApiService::class.java)
    }
}
