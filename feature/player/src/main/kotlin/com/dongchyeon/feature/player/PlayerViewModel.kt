package com.dongchyeon.feature.player

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dongchyeon.domain.model.PlaybackState
import com.dongchyeon.domain.model.Track
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
    // TODO: Inject PlayerRepository when implementing ExoPlayer integration
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()
    
    private val _sideEffect = Channel<PlayerSideEffect>()
    val sideEffect = _sideEffect.receiveAsFlow()
    
    fun handleIntent(intent: PlayerIntent) {
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
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    currentTrack = track,
                    playbackState = PlaybackState.Playing,
                    isPlaying = true,
                    duration = track.duration
                )
            }
            // TODO: Initialize ExoPlayer with track
        }
    }
    
    private fun togglePlayPause() {
        viewModelScope.launch {
            val currentIsPlaying = _uiState.value.isPlaying
            _uiState.update {
                it.copy(
                    isPlaying = !currentIsPlaying,
                    playbackState = if (!currentIsPlaying) PlaybackState.Playing else PlaybackState.Paused
                )
            }
            // TODO: Control ExoPlayer playback
        }
    }
    
    private fun seekTo(position: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(currentPosition = position) }
            // TODO: Seek ExoPlayer to position
        }
    }
    
    private fun playNext() {
        viewModelScope.launch {
            // TODO: Implement next track logic
            _sideEffect.send(PlayerSideEffect.ShowError("다음 트랙이 없습니다"))
        }
    }
    
    private fun playPrevious() {
        viewModelScope.launch {
            // TODO: Implement previous track logic
            _sideEffect.send(PlayerSideEffect.ShowError("이전 트랙이 없습니다"))
        }
    }
    
    private fun navigateBack() {
        viewModelScope.launch {
            _sideEffect.send(PlayerSideEffect.NavigateBack)
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        // TODO: Release ExoPlayer resources
    }
}
