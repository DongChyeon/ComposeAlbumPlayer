package com.dongchyeon.domain.model

sealed class PlaybackState {
    data object Idle : PlaybackState()

    data object Buffering : PlaybackState()

    data object Playing : PlaybackState()

    data object Paused : PlaybackState()

    data class Error(val message: String) : PlaybackState()
}
