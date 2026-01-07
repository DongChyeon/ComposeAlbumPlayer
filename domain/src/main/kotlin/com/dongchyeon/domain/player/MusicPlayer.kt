package com.dongchyeon.domain.player

import com.dongchyeon.domain.model.PlaybackState
import com.dongchyeon.domain.model.PlayerError
import com.dongchyeon.domain.model.RepeatMode
import com.dongchyeon.domain.model.ShuffleMode
import com.dongchyeon.domain.model.Track
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface MusicPlayer {
    // 상태
    val playbackState: StateFlow<PlaybackState>
    val currentTrack: StateFlow<Track?>
    val currentPosition: StateFlow<Long>
    val duration: StateFlow<Long>
    val repeatMode: StateFlow<RepeatMode>
    val shuffleMode: StateFlow<ShuffleMode>

    // 에러 이벤트 (일회성)
    val playerError: SharedFlow<PlayerError>

    // 재생 제어
    suspend fun initialize()
    fun play(track: Track)
    fun pause()
    fun resume()
    fun stop()
    fun seekTo(positionMs: Long)

    // 플레이리스트 제어
    fun setPlaylist(tracks: List<Track>)
    fun skipToNext()
    fun skipToPrevious()

    // 설정
    fun setRepeatMode(mode: RepeatMode)
    fun setShuffleMode(enabled: Boolean)

    // 리소스 관리
    fun release()
}
