package com.dongchyeon.feature.album

import androidx.lifecycle.viewModelScope
import com.dongchyeon.core.ui.base.BaseViewModel
import com.dongchyeon.domain.model.Track
import com.dongchyeon.domain.player.MusicPlayer
import com.dongchyeon.domain.repository.AlbumRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = AlbumViewModel.Factory::class)
class AlbumViewModel @AssistedInject constructor(
    @Assisted private val albumId: String,
    private val albumRepository: AlbumRepository,
    private val musicPlayer: MusicPlayer,
) : BaseViewModel<AlbumUiState, AlbumIntent, AlbumSideEffect>(
    initialState = AlbumUiState(isLoading = true),
) {

    @AssistedFactory
    interface Factory {
        fun create(albumId: String): AlbumViewModel
    }

    init {
        loadAlbumData()
    }

    override fun handleIntent(intent: AlbumIntent) {
        when (intent) {
            is AlbumIntent.Retry -> loadAlbumData()
            is AlbumIntent.NavigateBack -> sendSideEffect(AlbumSideEffect.NavigateBack)
            is AlbumIntent.NavigateToPlayer -> {
                playTrack(intent.track)
                sendSideEffect(AlbumSideEffect.NavigateToPlayer)
            }
        }
    }

    private fun loadAlbumData() {
        launchInScope {
            updateState { it.copy(isLoading = true, error = null) }

            try {
                val albumDeferred = async { albumRepository.getAlbumById(albumId) }
                val tracksDeferred = async { albumRepository.getTracksByAlbumId(albumId) }

                val albumResult = albumDeferred.await()
                val tracksResult = tracksDeferred.await()

                if (albumResult.isSuccess && tracksResult.isSuccess) {
                    val albumData = albumResult.getOrNull()!!
                    val tracksList = tracksResult.getOrNull()!!

                    val playableTracks = tracksList.filter {
                        it.isStreamable && it.streamUrl.isNotBlank()
                    }

                    if (playableTracks.isEmpty()) {
                        updateState { it.copy(isLoading = false) }
                        sendSideEffect(
                            AlbumSideEffect.ShowErrorAndNavigateBack(
                                "No playable tracks available in this album",
                            ),
                        )
                        return@launchInScope
                    }

                    updateState {
                        it.copy(
                            album = albumData.copy(tracks = playableTracks),
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

    private fun playTrack(track: Track) {
        viewModelScope.launch {
            val tracksList = currentState.album?.tracks ?: emptyList()
            if (tracksList.isNotEmpty()) {
                musicPlayer.setPlaylist(tracksList)
                musicPlayer.play(track)
            }
        }
    }
}
