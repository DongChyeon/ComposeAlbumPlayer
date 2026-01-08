package com.dongchyeon.core.network

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 동적 호스트 선택 Interceptor
 *
 * BaseUrlProvider에서 제공하는 동적 BASE_URL로 요청의 호스트를 교체합니다.
 * 이를 통해 Retrofit 빌드 후에도 BASE_URL을 변경할 수 있습니다.
 */
@Singleton
class HostSelectionInterceptor @Inject constructor(
    private val baseUrlProvider: BaseUrlProvider,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val originalUrl = originalRequest.url

        // BaseUrlProvider에서 동적 URL 가져오기
        val newBaseUrl = baseUrlProvider.baseUrl.toHttpUrlOrNull()

        val newUrl = if (newBaseUrl != null) {
            originalUrl.newBuilder()
                .scheme(newBaseUrl.scheme)
                .host(newBaseUrl.host)
                .port(newBaseUrl.port)
                .build()
        } else {
            originalUrl
        }

        val newRequest = originalRequest.newBuilder()
            .url(newUrl)
            .build()

        return chain.proceed(newRequest)
    }
}
