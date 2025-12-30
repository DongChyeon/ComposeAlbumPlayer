package com.dongchyeon.core.media.player

import com.dongchyeon.domain.model.PlaybackState
import com.dongchyeon.domain.model.RepeatMode
import com.dongchyeon.domain.model.Track
import com.dongchyeon.domain.player.MusicPlayer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * MusicPlayer의 실제 구현
 * ExoPlayerManager를 주입받아 사용하여 테스트 가능
 */
class MusicPlayerImpl @Inject constructor(
    private val exoPlayer: ExoPlayerManager,
) : MusicPlayer {
    // ExoPlayerManager의 playbackState를 그대로 사용
    override val playbackState: Flow<PlaybackState> = exoPlayer.playbackState

    private val _currentTrack = MutableStateFlow<Track?>(null)
    override val currentTrack: Flow<Track?> = _currentTrack.asStateFlow()

    private val _repeatMode = MutableStateFlow(RepeatMode.NONE)
    override val repeatMode: Flow<RepeatMode> = _repeatMode.asStateFlow()

    override val currentPosition: Flow<Long> = exoPlayer.currentPosition

    override val duration: Flow<Long> = exoPlayer.duration

    private var playlist: List<Track> = emptyList()
    private var currentIndex: Int = -1

    companion object {
        private const val SKIP_TO_PREVIOUS_THRESHOLD_MS = 5000L
    }

    override suspend fun play(track: Track) {
        _currentTrack.value = track

        // 플레이리스트에서 현재 트랙의 인덱스 찾기
        val indexInPlaylist = playlist.indexOfFirst { it.id == track.id }
        if (indexInPlaylist != -1) {
            currentIndex = indexInPlaylist
            exoPlayer.seekToMediaItem(indexInPlaylist)
        } else {
            // 플레이리스트에 없으면 단일 트랙 재생
            exoPlayer.prepareAndPlay(track.streamUrl)
        }

        exoPlayer.play()
        // playbackState는 exoPlayer.isPlaying 관찰을 통해 자동 업데이트
    }

    override suspend fun pause() {
        exoPlayer.pause()
        // playbackState는 exoPlayer.isPlaying 관찰을 통해 자동 업데이트
    }

    override suspend fun resume() {
        exoPlayer.play()
        // playbackState는 exoPlayer.isPlaying 관찰을 통해 자동 업데이트
    }

    override suspend fun seekTo(position: Long) {
        exoPlayer.seekTo(position)
    }

    override suspend fun skipToNext() {
        if (currentIndex >= 0 && currentIndex < playlist.size - 1) {
            currentIndex++
            val nextTrack = playlist[currentIndex]
            play(nextTrack)
        }
    }

    override suspend fun skipToPrevious() {
        val currentPos = exoPlayer.getCurrentPositionSync()

        when {
            // Case 1: 재생 시점이 5초 이하 & 이전 트랙 존재
            currentPos <= SKIP_TO_PREVIOUS_THRESHOLD_MS && currentIndex > 0 -> {
                currentIndex--
                val previousTrack = playlist[currentIndex]
                play(previousTrack)
            }

            // Case 2: 재생 시점이 5초 초과 or 첫 번째 트랙
            else -> {
                seekTo(0L)
            }
        }
    }

    override suspend fun stop() {
        _currentTrack.value = null
        exoPlayer.stop()
        // playbackState는 exoPlayer.isPlaying 관찰을 통해 자동 업데이트
    }

    override suspend fun setPlaylist(tracks: List<Track>) {
        playlist = tracks

        // ExoPlayer에 URL 리스트 전달
        val urls = tracks.map { it.streamUrl }
        exoPlayer.setPlaylist(urls)

        if (tracks.isNotEmpty() && currentIndex < 0) {
            currentIndex = 0
        }
    }

    override suspend fun setRepeatMode(mode: RepeatMode) {
        _repeatMode.value = mode
        exoPlayer.setRepeatMode(mode)
    }
}
