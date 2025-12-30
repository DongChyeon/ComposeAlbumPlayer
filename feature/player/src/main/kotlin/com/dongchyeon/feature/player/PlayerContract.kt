package com.dongchyeon.feature.player

import com.dongchyeon.domain.model.PlaybackState
import com.dongchyeon.domain.model.Track

data class PlayerUiState(
    val currentTrack: Track? = null,
    val playbackState: PlaybackState = PlaybackState.Idle,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val isPlaying: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
)

sealed interface PlayerIntent {
    data object PlayPause : PlayerIntent

    data class SeekTo(val position: Long) : PlayerIntent

    data object Next : PlayerIntent

    data object Previous : PlayerIntent

    data object NavigateBack : PlayerIntent
}

sealed interface PlayerSideEffect {
    data object NavigateBack : PlayerSideEffect

    data class ShowError(val message: String) : PlayerSideEffect
}
