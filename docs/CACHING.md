# 캐싱 시스템

ExoPlayer의 `SimpleCache`를 활용한 스트리밍 음악 캐싱 시스템입니다.

## 개요

네트워크에서 스트리밍한 음악 데이터를 디스크에 캐싱하여:
- **데이터 사용량 절약**: 같은 곡 재생 시 네트워크 요청 불필요
- **빠른 재생 시작**: 캐시된 곡은 즉시 재생
- **오프라인 재생 지원**: 캐시된 곡은 네트워크 없이도 재생 가능

---

## 캐싱 정책

### 캐시 크기 (동적)

디바이스 가용 공간의 **3%**를 캐시로 사용합니다.

| 디바이스 가용 공간 | 캐시 크기 |
|-----------------|---------|
| 1 GB | 50 MB (최소) |
| 5 GB | 150 MB |
| 10 GB | 300 MB |
| 20 GB+ | 500 MB (최대) |

### 제거 정책

- **LRU (Least Recently Used)**: 오래된 항목부터 자동 삭제
- **청크 단위**: 2MB 단위로 저장/삭제 (음악에 최적화)
- **자동 관리**: 수동 개입 불필요

### 에러 처리

- **캐시 에러 시**: 네트워크로 fallback (`FLAG_IGNORE_CACHE_ON_ERROR`)
- **재생 에러 시**: Snackbar 표시 + 자동 다음 트랙 이동

---

## 아키텍처

```
┌─────────────────────────────────────────────────────────────────┐
│                      캐싱 아키텍처                               │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│   ExoPlayer ──► CacheDataSource ──► SimpleCache (Disk)         │
│       │              │                    │                     │
│       │         Cache Hit? ───► Yes ──► Read from Disk         │
│       │              │                                          │
│       │              └──► No ──► HttpDataSource ──► Write Cache │
│       │                                                         │
│       │                         LeastRecentlyUsedCacheEvictor   │
│       │                         (캐시 초과 시 자동 LRU 삭제)      │
│       │                                                         │
│       └──► onPlayerError() ──► playerError Flow                │
│                                      │                          │
│                                      ▼                          │
│                               ViewModel                         │
│                                      │                          │
│                                      ▼                          │
│                          SideEffect (Snackbar)                  │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## 구현 코드

### DynamicCacheConfig

```kotlin
object DynamicCacheConfig {
    const val FRAGMENT_SIZE_BYTES = 2L * 1024 * 1024  // 2MB 청크

    private const val MIN_CACHE_SIZE = 50L * 1024 * 1024   // 50MB
    private const val MAX_CACHE_SIZE = 500L * 1024 * 1024  // 500MB
    private const val CACHE_PERCENTAGE = 0.03  // 가용 공간의 3%

    fun calculateCacheSize(context: Context): Long {
        val availableSpace = context.cacheDir.usableSpace
        val calculatedSize = (availableSpace * CACHE_PERCENTAGE).toLong()
        return calculatedSize.coerceIn(MIN_CACHE_SIZE, MAX_CACHE_SIZE)
    }
}
```

### CacheModule

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object CacheModule {

    @Provides
    @Singleton
    fun provideSimpleCache(
        context: Context,
        cacheDir: File,
        databaseProvider: StandaloneDatabaseProvider,
    ): SimpleCache {
        val cacheSize = DynamicCacheConfig.calculateCacheSize(context)
        val cacheEvictor = LeastRecentlyUsedCacheEvictor(cacheSize)
        return SimpleCache(cacheDir, cacheEvictor, databaseProvider)
    }

    @Provides
    @Singleton
    fun provideCacheDataSourceFactory(
        simpleCache: SimpleCache,
    ): CacheDataSource.Factory {
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(15_000)

        val cacheDataSinkFactory = CacheDataSink.Factory()
            .setCache(simpleCache)
            .setFragmentSize(DynamicCacheConfig.FRAGMENT_SIZE_BYTES)

        return CacheDataSource.Factory()
            .setCache(simpleCache)
            .setUpstreamDataSourceFactory(httpDataSourceFactory)
            .setCacheWriteDataSinkFactory(cacheDataSinkFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }
}
```

---

## 에러 메시지

| 에러 유형 | 메시지 |
|----------|-------|
| 네트워크 연결 실패 | "Network connection failed" |
| 타임아웃 | "Network connection timed out" |
| HTTP 에러 (502 등) | "Unable to play this track (server error)" |
| 파일 없음 | "Track not found" |
| 지원하지 않는 포맷 | "Unsupported audio format" |
| 기타 | "Unable to play this track" |

---

## 관련 파일

| 파일 | 설명 |
|------|------|
| `core/media/cache/DynamicCacheConfig.kt` | 동적 캐시 크기 계산 |
| `core/media/cache/CacheModule.kt` | Hilt DI 모듈 |
| `core/media/service/MusicService.kt` | 캐시 적용된 ExoPlayer |
| `domain/model/PlayerError.kt` | 에러 모델 |

---

## 참고 자료

- [ExoPlayer Caching - Android Developers](https://developer.android.com/media/media3/exoplayer/caching)
- [CacheDataSource API Reference](https://developer.android.com/reference/androidx/media3/datasource/cache/CacheDataSource)
