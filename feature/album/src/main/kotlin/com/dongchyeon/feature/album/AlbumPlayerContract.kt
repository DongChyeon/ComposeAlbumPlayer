package com.dongchyeon.feature.album

import com.dongchyeon.domain.model.Album
import com.dongchyeon.domain.model.PlaybackState
import com.dongchyeon.domain.model.Track

/**
 * AlbumPlayer의 UiState
 */
data class AlbumPlayerUiState(
    // 앨범 정보
    val album: Album? = null,
    val isLoading: Boolean = false,
    val error: String? = null,

    // 재생 상태
    val currentTrack: Track? = null,
    val playbackState: PlaybackState = PlaybackState.Idle,
    val isPlaying: Boolean = false,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
)

/**
 * AlbumPlayer의 Intent
 */
sealed interface AlbumPlayerIntent {
    // 앨범 관련
    data object Retry : AlbumPlayerIntent

    // 재생 제어
    data class PlayTrack(val track: Track) : AlbumPlayerIntent
    data object TogglePlayPause : AlbumPlayerIntent
    data class SeekTo(val position: Long) : AlbumPlayerIntent
    data object SkipToNext : AlbumPlayerIntent
    data object SkipToPrevious : AlbumPlayerIntent

    // 네비게이션
    data object NavigateBack : AlbumPlayerIntent
    data class NavigateToPlayer(val track: Track) : AlbumPlayerIntent
}

/**
 * AlbumPlayer의 SideEffect
 */
sealed interface AlbumPlayerSideEffect {
    data object NavigateBack : AlbumPlayerSideEffect
    data class NavigateToPlayer(val track: Track) : AlbumPlayerSideEffect
}
