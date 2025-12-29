package player

import com.dongchyeon.domain.model.PlaybackState
import com.dongchyeon.domain.model.Track
import com.dongchyeon.domain.player.MusicPlayer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeMusicPlayer : MusicPlayer {
    private val _playbackState = MutableStateFlow<PlaybackState>(PlaybackState.Idle)
    override val playbackState: Flow<PlaybackState> = _playbackState.asStateFlow()

    private val _currentTrack = MutableStateFlow<Track?>(null)
    override val currentTrack: Flow<Track?> = _currentTrack.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    override val currentPosition: Flow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    override val duration: Flow<Long> = _duration.asStateFlow()

    private var playlist: List<Track> = emptyList()
    private var currentIndex: Int = -1

    companion object {
        private const val SKIP_TO_PREVIOUS_THRESHOLD_MS = 5000L
    }

    override suspend fun play(track: Track) {
        _currentTrack.value = track
        _playbackState.value = PlaybackState.Playing
        _duration.value = track.duration
        _currentPosition.value = 0L

        val indexInPlaylist = playlist.indexOfFirst { it.id == track.id }
        if (indexInPlaylist != -1) {
            currentIndex = indexInPlaylist
        }
    }

    override suspend fun pause() {
        if (_playbackState.value == PlaybackState.Playing) {
            _playbackState.value = PlaybackState.Paused
        }
    }

    override suspend fun resume() {
        if (_playbackState.value == PlaybackState.Paused) {
            _playbackState.value = PlaybackState.Playing
        }
    }

    override suspend fun seekTo(position: Long) {
        _currentPosition.value = position
    }

    override suspend fun skipToNext() {
        if (currentIndex >= 0 && currentIndex < playlist.size - 1) {
            currentIndex++
            play(playlist[currentIndex])
        }
    }

    override suspend fun skipToPrevious() {
        val currentPos = _currentPosition.value

        when {
            // Case 1: 재생 시점이 5초 이하 & 이전 트랙 존재
            currentPos <= SKIP_TO_PREVIOUS_THRESHOLD_MS && currentIndex > 0 -> {
                currentIndex--
                play(playlist[currentIndex])
            }

            // Case 2: 재생 시점이 5초 초과 or 첫 번째 트랙
            else -> {
                seekTo(0L)
            }
        }
    }

    override suspend fun stop() {
        _playbackState.value = PlaybackState.Idle
        _currentTrack.value = null
        _currentPosition.value = 0L
        _duration.value = 0L
    }

    override suspend fun setPlaylist(tracks: List<Track>) {
        playlist = tracks
    }
}
