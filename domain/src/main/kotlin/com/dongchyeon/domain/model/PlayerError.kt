package com.dongchyeon.domain.model

/**
 * Player error event for propagating playback errors to UI
 */
data class PlayerError(
    val message: String,
    val trackId: String? = null,
)
