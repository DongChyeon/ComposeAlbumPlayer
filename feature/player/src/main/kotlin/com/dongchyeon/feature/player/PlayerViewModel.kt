package com.dongchyeon.feature.player

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.dongchyeon.core.media.controller.MediaControllerManager
import com.dongchyeon.core.ui.base.BaseViewModel
import com.dongchyeon.domain.model.PlaybackState
import com.dongchyeon.domain.repository.AlbumRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val albumRepository: AlbumRepository,
    private val mediaControllerManager: MediaControllerManager,
) : BaseViewModel<PlayerUiState, PlayerIntent, PlayerSideEffect>(
    initialState = PlayerUiState(
        isLoading = (savedStateHandle.get<String>("trackId") ?: "").isNotEmpty(),
    ),
) {
    private val trackId: String = savedStateHandle["trackId"] ?: ""

    init {
        if (trackId.isNotEmpty()) {
            loadTrack()
        }
        observeMediaController()
    }

    private fun observeMediaController() {
        // PlaybackState 관찰
        mediaControllerManager.playbackState
            .onEach { playbackState ->
                updateState {
                    it.copy(
                        playbackState = playbackState,
                        isPlaying = playbackState == PlaybackState.Playing,
                    )
                }
            }
            .launchIn(viewModelScope)

        // CurrentPosition 관찰
        mediaControllerManager.currentPosition
            .onEach { position ->
                updateState { it.copy(currentPosition = position) }
            }
            .launchIn(viewModelScope)

        // Duration 관찰
        mediaControllerManager.duration
            .onEach { duration ->
                updateState { it.copy(duration = duration) }
            }
            .launchIn(viewModelScope)

        // CurrentTrack 관찰
        mediaControllerManager.currentTrack
            .onEach { track ->
                track?.let {
                    updateState { state -> state.copy(currentTrack = track) }
                }
            }
            .launchIn(viewModelScope)
    }

    private fun loadTrack() {
        launchInScope {
            updateState { it.copy(isLoading = true) }

            albumRepository.getTrackById(trackId)
                .onSuccess { track ->
                    updateState {
                        it.copy(
                            currentTrack = track,
                            duration = track.duration,
                            isLoading = false,
                            error = null,
                        )
                    }
                    // 트랙 로드 성공 시 자동으로 재생
                    mediaControllerManager.play(track)
                    Log.d("PlayerViewModel", "트랙 로드 성공: $track")
                }
                .onFailure { exception ->
                    updateState {
                        it.copy(
                            isLoading = false,
                            error = exception.message ?: "트랙을 불러올 수 없습니다",
                        )
                    }
                }
        }
    }

    override fun handleIntent(intent: PlayerIntent) {
        when (intent) {
            is PlayerIntent.PlayPause -> togglePlayPause()
            is PlayerIntent.SeekTo -> seekTo(intent.position)
            is PlayerIntent.Next -> playNext()
            is PlayerIntent.Previous -> playPrevious()
            is PlayerIntent.NavigateBack -> navigateBack()
        }
    }

    private fun togglePlayPause() {
        if (currentState.isPlaying) {
            mediaControllerManager.pause()
        } else {
            mediaControllerManager.resume()
        }
    }

    private fun seekTo(position: Long) {
        mediaControllerManager.seekTo(position)
    }

    private fun playNext() {
        launchInScope {
            mediaControllerManager.skipToNext()
        }
    }

    private fun playPrevious() {
        launchInScope {
            mediaControllerManager.skipToPrevious()
        }
    }

    private fun navigateBack() {
        sendSideEffect(PlayerSideEffect.NavigateBack)
    }
}
