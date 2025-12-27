package com.dongchyeon.feature.player

import androidx.lifecycle.SavedStateHandle
import com.dongchyeon.core.ui.base.BaseViewModel
import com.dongchyeon.domain.model.PlaybackState
import com.dongchyeon.domain.model.Track
import com.dongchyeon.domain.repository.AlbumRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val albumRepository: AlbumRepository
) : BaseViewModel<PlayerUiState, PlayerIntent, PlayerSideEffect>(
    initialState = PlayerUiState()
) {
    
    private val trackId: String = savedStateHandle["trackId"] ?: ""
    
    init {
        if (trackId.isNotEmpty()) {
            loadTrack()
        }
    }
    
    private fun loadTrack() {
        launchInScope {
            updateState { it.copy(isLoading = true) }
            
            albumRepository.getTrackById(trackId)
                .onSuccess { track ->
                    updateState {
                        it.copy(
                            currentTrack = track,
                            playbackState = PlaybackState.Buffering,
                            duration = track.duration,
                            isLoading = false,
                            error = null
                        )
                    }
                }
                .onFailure { exception ->
                    updateState {
                        it.copy(
                            isLoading = false,
                            error = exception.message ?: "트랙을 불러올 수 없습니다"
                        )
                    }
                }
        }
    }
    
    override fun handleIntent(intent: PlayerIntent) {
        when (intent) {
            is PlayerIntent.InitializePlayer -> initializePlayer(intent.track)
            is PlayerIntent.PlayPause -> togglePlayPause()
            is PlayerIntent.SeekTo -> seekTo(intent.position)
            is PlayerIntent.Next -> playNext()
            is PlayerIntent.Previous -> playPrevious()
            is PlayerIntent.NavigateBack -> navigateBack()
        }
    }
    
    private fun initializePlayer(track: Track) {
        updateState {
            it.copy(
                currentTrack = track,
                playbackState = PlaybackState.Playing,
                isPlaying = true,
                duration = track.duration
            )
        }
        // TODO: Initialize ExoPlayer with track
    }
    
    private fun togglePlayPause() {
        val currentIsPlaying = currentState.isPlaying
        updateState {
            it.copy(
                isPlaying = !currentIsPlaying,
                playbackState = if (!currentIsPlaying) PlaybackState.Playing else PlaybackState.Paused
            )
        }
        // TODO: Control ExoPlayer playback
    }
    
    private fun seekTo(position: Long) {
        updateState { it.copy(currentPosition = position) }
        // TODO: Seek ExoPlayer to position
    }
    
    private fun playNext() {
        // TODO: Implement next track logic
        sendSideEffect(PlayerSideEffect.ShowError("다음 트랙이 없습니다"))
    }
    
    private fun playPrevious() {
        // TODO: Implement previous track logic
        sendSideEffect(PlayerSideEffect.ShowError("이전 트랙이 없습니다"))
    }
    
    private fun navigateBack() {
        sendSideEffect(PlayerSideEffect.NavigateBack)
    }
}
