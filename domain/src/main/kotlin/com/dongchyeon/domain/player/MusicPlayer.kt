package com.dongchyeon.domain.player

import com.dongchyeon.domain.model.PlaybackState
import com.dongchyeon.domain.model.Track
import kotlinx.coroutines.flow.Flow

interface MusicPlayer {
    val playbackState: Flow<PlaybackState>
    val currentTrack: Flow<Track?>
    val currentPosition: Flow<Long>
    val duration: Flow<Long>

    suspend fun play(track: Track)
    suspend fun pause()
    suspend fun resume()
    suspend fun seekTo(position: Long)
    suspend fun skipToNext()
    suspend fun skipToPrevious()
    suspend fun stop()

    suspend fun setPlaylist(tracks: List<Track>)
}
