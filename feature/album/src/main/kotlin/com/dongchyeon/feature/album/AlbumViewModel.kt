package com.dongchyeon.feature.album

import androidx.lifecycle.SavedStateHandle
import com.dongchyeon.core.ui.base.BaseViewModel
import com.dongchyeon.domain.model.Track
import com.dongchyeon.domain.repository.AlbumRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import javax.inject.Inject

@HiltViewModel
class AlbumViewModel @Inject constructor(
    private val albumRepository: AlbumRepository,
    savedStateHandle: SavedStateHandle,
) : BaseViewModel<AlbumUiState, AlbumIntent, AlbumSideEffect>(
    initialState = AlbumUiState(),
) {
    private val albumId: String = savedStateHandle["albumId"] ?: ""

    init {
        loadAlbumData()
    }

    override fun handleIntent(intent: AlbumIntent) {
        when (intent) {
            is AlbumIntent.Retry -> loadAlbumData()
            is AlbumIntent.PlayTrack -> playTrack(intent.track)
            is AlbumIntent.NavigateBack -> navigateBack()
        }
    }

    private fun loadAlbumData() {
        launchInScope {
            updateState {
                it.copy(
                    isLoading = true,
                    error = null,
                )
            }

            try {
                // 앨범 정보와 트랙 목록을 병렬로 로드
                val albumDeferred = async { albumRepository.getAlbumById(albumId) }
                val tracksDeferred = async { albumRepository.getTracksByAlbumId(albumId) }

                val albumResult = albumDeferred.await()
                val tracksResult = tracksDeferred.await()

                if (albumResult.isSuccess && tracksResult.isSuccess) {
                    val album = albumResult.getOrNull()!!
                    val tracks = tracksResult.getOrNull()!!

                    updateState {
                        it.copy(
                            album = album.copy(tracks = tracks),
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
        sendSideEffect(AlbumSideEffect.NavigateToPlayer(track))
    }

    private fun navigateBack() {
        sendSideEffect(AlbumSideEffect.NavigateBack)
    }
}
