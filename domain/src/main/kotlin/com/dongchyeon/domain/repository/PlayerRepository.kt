package com.dongchyeon.domain.repository

import com.dongchyeon.domain.model.PlaybackState
import com.dongchyeon.domain.model.Track
import kotlinx.coroutines.flow.Flow

interface PlayerRepository {
    val playbackState: Flow<PlaybackState>
    val currentTrack: Flow<Track?>
    val currentPosition: Flow<Long>
    val duration: Flow<Long>

    fun playTrack(track: Track)

    fun play()

    fun pause()

    fun seekTo(position: Long)

    fun next()

    fun previous()

    fun release()
}
