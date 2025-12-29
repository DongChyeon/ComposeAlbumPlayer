package com.dongchyeon.core.media.player

import com.dongchyeon.domain.model.PlaybackState
import com.dongchyeon.domain.model.RepeatMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 테스트용 FakeExoPlayerManager
 * 실제 ExoPlayer 없이 동작을 시뮬레이션
 */
class FakeExoPlayerManager : ExoPlayerManager {
    private val _isPlaying = MutableStateFlow(false)
    override val isPlaying: Flow<Boolean> = _isPlaying.asStateFlow()

    private val _playbackState = MutableStateFlow<PlaybackState>(PlaybackState.Idle)
    override val playbackState: Flow<PlaybackState> = _playbackState.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    override val currentPosition: Flow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    override val duration: Flow<Long> = _duration.asStateFlow()

    private val _currentMediaItemIndex = MutableStateFlow(-1)
    override val currentMediaItemIndex: Flow<Int> = _currentMediaItemIndex.asStateFlow()

    private var playlist: List<String> = emptyList()
    private var currentUrl: String? = null
    private var repeatMode: RepeatMode = RepeatMode.NONE

    // 테스트를 위한 헬퍼 메서드
    fun setDuration(durationMs: Long) {
        _duration.value = durationMs
    }

    private fun updatePlaybackState() {
        _playbackState.value =
            when {
                currentUrl == null -> PlaybackState.Idle
                _isPlaying.value -> PlaybackState.Playing
                else -> PlaybackState.Paused
            }
    }

    override fun prepareAndPlay(url: String) {
        currentUrl = url
        _isPlaying.value = true
        _currentPosition.value = 0L

        // 플레이리스트에서 인덱스 찾기
        val index = playlist.indexOf(url)
        if (index != -1) {
            _currentMediaItemIndex.value = index
        }
        updatePlaybackState()
    }

    override fun play() {
        _isPlaying.value = true
        updatePlaybackState()
    }

    override fun pause() {
        _isPlaying.value = false
        updatePlaybackState()
    }

    override fun stop() {
        _isPlaying.value = false
        _currentPosition.value = 0L
        currentUrl = null
        _currentMediaItemIndex.value = -1
        updatePlaybackState()
    }

    override fun seekTo(positionMs: Long) {
        _currentPosition.value = positionMs
    }

    override fun setPlaylist(urls: List<String>) {
        playlist = urls
        if (urls.isNotEmpty() && _currentMediaItemIndex.value < 0) {
            _currentMediaItemIndex.value = 0
        }
    }

    override fun seekToMediaItem(index: Int) {
        if (index in playlist.indices) {
            _currentMediaItemIndex.value = index
            currentUrl = playlist[index]
            _currentPosition.value = 0L
        }
    }

    override fun seekToNext() {
        val nextIndex = _currentMediaItemIndex.value + 1
        if (nextIndex < playlist.size) {
            seekToMediaItem(nextIndex)
        }
    }

    override fun seekToPrevious() {
        val prevIndex = _currentMediaItemIndex.value - 1
        if (prevIndex >= 0) {
            seekToMediaItem(prevIndex)
        }
    }

    override fun getCurrentPositionSync(): Long {
        return _currentPosition.value
    }

    override fun setRepeatMode(mode: RepeatMode) {
        repeatMode = mode
    }

    override fun release() {
        stop()
        playlist = emptyList()
    }
}
