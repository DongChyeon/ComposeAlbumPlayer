package com.dongchyeon.feature.player

import com.dongchyeon.domain.model.PlaybackState
import com.dongchyeon.domain.model.RepeatMode
import com.dongchyeon.domain.model.ShuffleMode
import com.dongchyeon.domain.model.Track

data class PlayerUiState(
    val currentTrack: Track? = null,
    val playbackState: PlaybackState = PlaybackState.Idle,
    val isPlaying: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.NONE,
    val shuffleMode: ShuffleMode = ShuffleMode.OFF,
)

sealed interface PlayerIntent {
    data object TogglePlayPause : PlayerIntent
    data class SeekTo(val position: Long) : PlayerIntent
    data object SkipToNext : PlayerIntent
    data object SkipToPrevious : PlayerIntent
    data object ToggleRepeatMode : PlayerIntent
    data object ToggleShuffle : PlayerIntent
    data object NavigateBack : PlayerIntent
}

sealed interface PlayerSideEffect {
    data object NavigateBack : PlayerSideEffect
    data class ShowPlaybackError(val message: String) : PlayerSideEffect
}
