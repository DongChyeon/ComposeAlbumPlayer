package com.dongchyeon.feature.player

import androidx.lifecycle.viewModelScope
import com.dongchyeon.core.ui.base.BaseViewModel
import com.dongchyeon.domain.model.PlaybackState
import com.dongchyeon.domain.model.RepeatMode
import com.dongchyeon.domain.model.ShuffleMode
import com.dongchyeon.domain.player.MusicPlayer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val musicPlayer: MusicPlayer,
) : BaseViewModel<PlayerUiState, PlayerIntent, PlayerSideEffect>(
    initialState = PlayerUiState(),
) {

    val currentPositionSeconds: StateFlow<Int> = musicPlayer.currentPosition
        .map { (it / 1000).toInt() }
        .stateIn(viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = 0)

    val durationSeconds: StateFlow<Int> = musicPlayer.duration
        .map { (it / 1000).toInt() }
        .stateIn(viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = 0)

    init {
        observeMusicPlayer()
    }

    override fun handleIntent(intent: PlayerIntent) {
        when (intent) {
            is PlayerIntent.TogglePlayPause -> togglePlayPause()
            is PlayerIntent.SeekTo -> seekTo(intent.position)
            is PlayerIntent.SkipToNext -> skipToNext()
            is PlayerIntent.SkipToPrevious -> skipToPrevious()
            is PlayerIntent.ToggleRepeatMode -> toggleRepeatMode()
            is PlayerIntent.ToggleShuffle -> toggleShuffle()
            is PlayerIntent.NavigateBack -> sendSideEffect(PlayerSideEffect.NavigateBack)
        }
    }

    private fun observeMusicPlayer() {
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

        musicPlayer.currentTrack
            .onEach { track ->
                updateState { it.copy(currentTrack = track) }
            }
            .launchIn(viewModelScope)

        musicPlayer.repeatMode
            .onEach { mode ->
                updateState { it.copy(repeatMode = mode) }
            }
            .launchIn(viewModelScope)

        musicPlayer.shuffleMode
            .onEach { mode ->
                updateState { it.copy(shuffleMode = mode) }
            }
            .launchIn(viewModelScope)

        musicPlayer.playerError
            .onEach { error ->
                sendSideEffect(PlayerSideEffect.ShowPlaybackError(error.message))
            }
            .launchIn(viewModelScope)
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
        musicPlayer.skipToNext()
    }

    private fun skipToPrevious() {
        musicPlayer.skipToPrevious()
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
