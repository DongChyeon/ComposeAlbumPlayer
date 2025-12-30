package com.dongchyeon.feature.album

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.dongchyeon.core.ui.base.BaseViewModel
import com.dongchyeon.domain.model.PlaybackState
import com.dongchyeon.domain.model.RepeatMode
import com.dongchyeon.domain.model.ShuffleMode
import com.dongchyeon.domain.model.Track
import com.dongchyeon.domain.player.MusicPlayer
import com.dongchyeon.domain.repository.AlbumRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlbumPlayerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val albumRepository: AlbumRepository,
    private val musicPlayer: MusicPlayer,
) : BaseViewModel<AlbumPlayerUiState, AlbumPlayerIntent, AlbumPlayerSideEffect>(
    initialState = AlbumPlayerUiState(isLoading = true),
) {

    private val albumId: String = savedStateHandle["albumId"] ?: ""

    init {
        loadAlbumData()
        observeMediaController()
    }

    override fun handleIntent(intent: AlbumPlayerIntent) {
        when (intent) {
            // 앨범 관련
            is AlbumPlayerIntent.Retry -> loadAlbumData()

            // 재생 제어
            is AlbumPlayerIntent.PlayTrack -> playTrack(intent.track)
            is AlbumPlayerIntent.TogglePlayPause -> togglePlayPause()
            is AlbumPlayerIntent.SeekTo -> seekTo(intent.position)
            is AlbumPlayerIntent.SkipToNext -> skipToNext()
            is AlbumPlayerIntent.SkipToPrevious -> skipToPrevious()

            // 재생 모드
            is AlbumPlayerIntent.ToggleRepeatMode -> toggleRepeatMode()
            is AlbumPlayerIntent.ToggleShuffle -> toggleShuffle()

            // 네비게이션
            is AlbumPlayerIntent.NavigateBack -> sendSideEffect(AlbumPlayerSideEffect.NavigateBack)
            is AlbumPlayerIntent.NavigateToPlayer -> {
                playTrack(intent.track)
                sendSideEffect(AlbumPlayerSideEffect.NavigateToPlayer(intent.track))
            }
        }
    }

    private fun loadAlbumData() {
        launchInScope {
            updateState { it.copy(isLoading = true, error = null) }

            try {
                // 앨범 정보와 트랙 목록을 병렬로 로드
                val albumDeferred = async { albumRepository.getAlbumById(albumId) }
                val tracksDeferred = async { albumRepository.getTracksByAlbumId(albumId) }

                val albumResult = albumDeferred.await()
                val tracksResult = tracksDeferred.await()

                if (albumResult.isSuccess && tracksResult.isSuccess) {
                    val albumData = albumResult.getOrNull()!!
                    val tracksList = tracksResult.getOrNull()!!

                    updateState {
                        it.copy(
                            album = albumData.copy(tracks = tracksList),
                            isLoading = false,
                            error = null,
                        )
                    }
                } else {
                    val error = albumResult.exceptionOrNull() ?: tracksResult.exceptionOrNull()
                    updateState {
                        it.copy(
                            isLoading = false,
                            error = error?.message ?: "알 수 없는 오류가 발생했습니다",
                        )
                    }
                }
            } catch (e: Exception) {
                updateState {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "알 수 없는 오류가 발생했습니다",
                    )
                }
            }
        }
    }

    private fun observeMediaController() {
        // PlaybackState 관찰
        musicPlayer.playbackState
            .onEach { state ->
                updateState {
                    it.copy(
                        playbackState = state,
                        isPlaying = state == PlaybackState.Playing,
                    )
                }
            }
            .launchIn(viewModelScope)

        // CurrentTrack 관찰
        musicPlayer.currentTrack
            .onEach { track ->
                updateState { it.copy(currentTrack = track) }
            }
            .launchIn(viewModelScope)

        // CurrentPosition 관찰
        musicPlayer.currentPosition
            .onEach { position ->
                updateState {
                    it.copy(
                        currentPosition = position,
                        currentPositionSeconds = (position / 1000).toInt(),
                    )
                }
            }
            .launchIn(viewModelScope)

        // Duration 관찰
        musicPlayer.duration
            .onEach { duration ->
                updateState {
                    it.copy(
                        duration = duration,
                        durationSeconds = (duration / 1000).toInt(),
                    )
                }
            }
            .launchIn(viewModelScope)

        // RepeatMode 관찰
        musicPlayer.repeatMode
            .onEach { mode ->
                updateState { it.copy(repeatMode = mode) }
            }
            .launchIn(viewModelScope)

        // ShuffleMode 관찰
        musicPlayer.shuffleMode
            .onEach { mode ->
                updateState { it.copy(shuffleMode = mode) }
            }
            .launchIn(viewModelScope)
    }

    private fun playTrack(track: Track) {
        viewModelScope.launch {
            val tracksList = currentState.album?.tracks ?: emptyList()
            if (tracksList.isNotEmpty()) {
                // 플레이리스트 설정
                musicPlayer.setPlaylist(tracksList)
                // 선택한 트랙부터 재생
                musicPlayer.play(track)
            }
        }
    }

    private fun togglePlayPause() {
        if (currentState.isPlaying) {
            musicPlayer.pause()
        } else {
            musicPlayer.resume()
        }
    }

    private fun seekTo(position: Long) {
        musicPlayer.seekTo(position)
    }

    private fun skipToNext() {
        viewModelScope.launch {
            musicPlayer.skipToNext()
        }
    }

    private fun skipToPrevious() {
        viewModelScope.launch {
            musicPlayer.skipToPrevious()
        }
    }

    private fun toggleRepeatMode() {
        val nextMode = when (currentState.repeatMode) {
            RepeatMode.NONE -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.NONE
        }
        musicPlayer.setRepeatMode(nextMode)
    }

    private fun toggleShuffle() {
        val isCurrentlyOn = currentState.shuffleMode == ShuffleMode.ON
        musicPlayer.setShuffleMode(!isCurrentlyOn)
    }
}
