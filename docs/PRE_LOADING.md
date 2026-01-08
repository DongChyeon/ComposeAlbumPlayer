# 버퍼링 및 프리로드 시스템

ExoPlayer의 `LoadControl`과 `DefaultPreloadManager`를 활용한 음악 최적화 버퍼링/프리로드 시스템입니다.

## 개요

- **LoadControl**: 현재 재생 중인 곡의 버퍼링 정책
- **DefaultPreloadManager**: 다음/이전 곡의 MediaSource 미리 준비

두 시스템이 함께 동작하여 끊김 없는 음악 재생 경험을 제공합니다.

---

## 버퍼링 정책 (LoadControl)

### 비디오 기본값 vs 음악 최적화

| 파라미터 | 비디오 기본값 | 음악 최적화 | 변경 이유 |
|----------|-------------|------------|----------|
| **minBufferMs** | 50초 | 30초 | 음악은 비트레이트가 낮아 적은 버퍼로 충분 |
| **maxBufferMs** | 50초 | 2분 | 다음 곡 전환까지 여유있게 버퍼링 |
| **bufferForPlaybackMs** | 2.5초 | 1.5초 | 빠른 재생 시작 (40% 단축) |
| **bufferForPlaybackAfterRebufferMs** | 5초 | 3초 | 끊김 후 빠른 재개 |
| **backBufferDurationMs** | 0초 | 30초 | 뒤로 감기 즉시 반응 |

### 시각적 비교

```
비디오 기본 설정:
    재생 위치
        │
        ▼
────────┼────────────────────────────────────────────────────►
        │◄──────────── 50초 버퍼 ─────────────►│
        │  백버퍼: 없음 (0초)

음악 최적화 설정:
                            재생 위치
                                │
                                ▼
◄────── 30초 백버퍼 ──────┼──────────────────────────────────────────────►
                          │◄────────────── 최대 2분 버퍼 ───────────────►│
                          │  재생 시작: 1.5초만 버퍼되면 즉시 시작
```

### 구현 코드

```kotlin
// MusicService.kt
val loadControl = DefaultLoadControl.Builder()
    .setBufferDurationsMs(
        /* minBufferMs */ 30_000,
        /* maxBufferMs */ 120_000,
        /* bufferForPlaybackMs */ 1_500,
        /* bufferForPlaybackAfterRebufferMs */ 3_000
    )
    .setBackBuffer(
        /* backBufferDurationMs */ 30_000,
        /* retainBackBufferFromKeyframe */ false
    )
    .build()
```

---

## 프리로드 시스템 (DefaultPreloadManager)

### 아키텍처

```
┌─────────────────────────────────────────────────────────────────┐
│              프리로드 아키텍처 (DefaultPreloadManager)            │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│   [App Process]                    [Service Process]            │
│                                                                 │
│   MediaControllerMusicPlayer       MusicService                 │
│          │                              │                       │
│          │  CustomCommand               ├── ExoPlayer           │
│          │  (PRELOAD_ADJACENT)          │                       │
│          │  (PLAY_PRELOADED)            ├── DefaultPreloadManager│
│          └──────────────────────────────┤      │                │
│                                         │      ▼                │
│                                         │   MediaSource 준비    │
│                                         │      │                │
│                                         │      ▼                │
│                                         └── CacheDataSource     │
│                                              (자동 캐시 저장)    │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 프리로드 정책

| 거리 | 프리로드 수준 | 설명 |
|------|-------------|------|
| **1칸 (다음/이전)** | 30초 프리로드 | MediaSource 준비 + 30초 데이터 로드 |
| **2칸** | 트랙 선택까지 | 포맷 분석, 트랙 선택 완료 |
| **3~4칸** | 소스 준비까지 | MediaSource 생성만 |
| **5칸 이상** | 프리로드 안 함 | 리소스 절약 |

### CustomCommand 정의

```kotlin
// MusicCommand.kt
sealed class MusicCommand(val action: String) {
    // 인접 트랙 프리로드 요청
    data class PreloadAdjacentTracks(val currentIndex: Int)
        : MusicCommand(ACTION_PRELOAD_ADJACENT)

    // 프리로드된 트랙 재생
    data class PlayPreloaded(val mediaId: String)
        : MusicCommand(ACTION_PLAY_PRELOADED)

    // 프리로드 초기화
    data object ResetPreload
        : MusicCommand(ACTION_RESET_PRELOAD)
}
```

### 구현 코드

```kotlin
// MusicService.kt - MusicPreloadStatusControl
private inner class MusicPreloadStatusControl :
    TargetPreloadStatusControl<Int, DefaultPreloadManager.PreloadStatus> {

    override fun getTargetPreloadStatus(rankingData: Int): DefaultPreloadManager.PreloadStatus {
        val distance = abs(rankingData - currentPlayingIndex)

        return when {
            distance == 1 -> DefaultPreloadManager.PreloadStatus.specifiedRangeLoaded(30_000L)
            distance == 2 -> DefaultPreloadManager.PreloadStatus.PRELOAD_STATUS_TRACKS_SELECTED
            distance in 3..4 -> DefaultPreloadManager.PreloadStatus.PRELOAD_STATUS_SOURCE_PREPARED
            else -> DefaultPreloadManager.PreloadStatus.PRELOAD_STATUS_NOT_PRELOADED
        }
    }
}

// PreloadManager 초기화
val preloadManagerBuilder = DefaultPreloadManager.Builder(this, MusicPreloadStatusControl())
preloadManager = preloadManagerBuilder.build()
player = preloadManagerBuilder.buildExoPlayer(ExoPlayer.Builder(this)...)

// 프리로드 요청 처리
private fun preloadAdjacentTracks(newIndex: Int) {
    currentPlayingIndex = newIndex
    preloadManager.reset()

    for (i in 0 until player.mediaItemCount) {
        val distance = abs(i - newIndex)
        if (distance in 1..4) {
            preloadManager.add(player.getMediaItemAt(i), i)
        }
    }

    preloadManager.invalidate()  // 프리로드 실행 트리거
}
```

### 동작 흐름

```
┌─────────────────────────────────────────────────────────────────┐
│              프리로드 동작 흐름                                   │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│   [곡 인덱스 변경]                                               │
│          │                                                      │
│          ▼                                                      │
│   MediaControllerMusicPlayer                                    │
│          │                                                      │
│          │  sendCustomCommand(PRELOAD_ADJACENT, index)          │
│          ▼                                                      │
│   ───────────────── IPC ─────────────────                       │
│          │                                                      │
│          ▼                                                      │
│   MusicService.onCustomCommand()                                │
│          │                                                      │
│          ├──► preloadManager.reset()                            │
│          ├──► preloadManager.add(인접 트랙들)                    │
│          └──► preloadManager.invalidate()  ← 프리로드 시작!      │
│                       │                                         │
│                       ▼                                         │
│               MusicPreloadStatusControl                         │
│                       │                                         │
│                       ├── 1칸: 30초 프리로드                     │
│                       ├── 2칸: 트랙 선택까지                     │
│                       └── 3~4칸: 소스 준비까지                   │
│                                                                 │
│   [다음 곡 버튼] ──► seekTo() ──► 즉시 재생!                     │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## LoadControl + PreloadManager 시너지

```
┌─────────────────────────────────────────────────────────────────┐
│                    전체 버퍼링 아키텍처                           │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│   LoadControl                    DefaultPreloadManager          │
│   ────────────                   ───────────────────            │
│   "현재 곡을 안정적으로"          "다음 곡 MediaSource 미리 준비" │
│                                                                 │
│   ┌─────────────────┐           ┌─────────────────┐            │
│   │  곡 A 재생 중    │           │  곡 B 프리로드   │            │
│   │  ▶ ━━━━━━━░░░░░ │           │  MediaSource 준비│            │
│   │   2분 버퍼 관리  │           │  + 캐시 자동 저장 │            │
│   └─────────────────┘           └─────────────────┘            │
│          │                              │                      │
│          └──────────────┬───────────────┘                      │
│                         │                                      │
│                         ▼                                      │
│              CacheDataSource (디스크 캐시)                       │
│                         │                                      │
│                         ▼                                      │
│              [다음 곡] 버튼 클릭 시 즉시 재생!                    │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## 기대 효과

| 항목 | 개선 내용 |
|------|----------|
| **재생 시작 속도** | 2.5초 -> 1.5초 (40% 단축) |
| **끊김 후 복구** | 5초 -> 3초 (40% 단축) |
| **뒤로 감기** | 네트워크 재요청 -> 즉시 재생 |
| **다음 곡 전환** | MediaSource 준비 완료로 즉시 재생 |
| **이전 곡 전환** | 30초 프리로드로 빠른 재생 |

---

## 관련 파일

| 파일 | 설명 |
|------|------|
| `core/media/service/MusicService.kt` | LoadControl, PreloadManager, CustomCommand 처리 |
| `core/media/command/MusicCommand.kt` | CustomCommand sealed class |
| `core/media/controller/MediaControllerMusicPlayer.kt` | CustomCommand 전송 |

---

## 참고 자료

- [ExoPlayer Customization - Android Developers](https://developer.android.com/media/media3/exoplayer/customization)
- [DefaultLoadControl API Reference](https://developer.android.com/reference/androidx/media3/exoplayer/DefaultLoadControl)
- [Media3 PreloadManager](https://developer.android.com/media/media3/exoplayer/preloading-media)
