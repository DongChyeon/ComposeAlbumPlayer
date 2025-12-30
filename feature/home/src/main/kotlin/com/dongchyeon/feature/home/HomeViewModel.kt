package com.dongchyeon.feature.home

import com.dongchyeon.core.ui.base.BaseViewModel
import com.dongchyeon.domain.repository.AlbumRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val albumRepository: AlbumRepository,
) : BaseViewModel<HomeUiState, HomeIntent, HomeSideEffect>(
    initialState = HomeUiState(),
) {
    init {
        handleIntent(HomeIntent.LoadAlbums)
    }

    override fun handleIntent(intent: HomeIntent) {
        when (intent) {
            is HomeIntent.LoadAlbums -> loadAlbums()
            is HomeIntent.LoadMoreAlbums -> loadMoreAlbums()
            is HomeIntent.Retry -> retry()
            is HomeIntent.DismissScrollHint -> dismissScrollHint()
            is HomeIntent.NavigateToAlbum -> navigateToAlbum(intent.albumId)
        }
    }

    private fun dismissScrollHint() {
        updateState { it.copy(showScrollHint = false) }
    }

    private fun loadAlbums() {
        launchInScope {
            updateState { it.copy(isLoading = true, error = null) }

            albumRepository.getAlbums(page = 0, limit = PaginationState.PAGE_SIZE)
                .onSuccess { albums ->
                    updateState {
                        it.copy(
                            albums = albums,
                            isLoading = false,
                            error = null,
                            paginationState =
                            PaginationState(
                                currentPage = 0,
                                isLoadingMore = false,
                                hasMoreData = albums.size >= PaginationState.PAGE_SIZE,
                            ),
                        )
                    }
                }
                .onFailure { exception ->
                    updateState {
                        it.copy(
                            isLoading = false,
                            error = exception.message ?: "알 수 없는 오류가 발생했습니다",
                        )
                    }
                }
        }
    }

    private fun loadMoreAlbums() {
        val pagination = currentState.paginationState

        // 이미 로딩 중이거나 더 이상 데이터가 없으면 리턴
        if (pagination.isLoadingMore || !pagination.hasMoreData) return

        launchInScope {
            val nextPage = pagination.currentPage + 1
            updateState {
                it.copy(
                    paginationState = pagination.copy(isLoadingMore = true),
                )
            }

            albumRepository.getAlbums(page = nextPage, limit = PaginationState.PAGE_SIZE)
                .onSuccess { newAlbums ->
                    updateState {
                        it.copy(
                            albums = it.albums + newAlbums,
                            paginationState =
                            pagination.copy(
                                currentPage = nextPage,
                                isLoadingMore = false,
                                hasMoreData = newAlbums.size >= PaginationState.PAGE_SIZE,
                            ),
                        )
                    }
                }
                .onFailure { exception ->
                    updateState {
                        it.copy(
                            paginationState = pagination.copy(isLoadingMore = false),
                            error = exception.message ?: "알 수 없는 오류가 발생했습니다",
                        )
                    }
                }
        }
    }

    private fun retry() {
        updateState {
            it.copy(
                albums = emptyList(),
                paginationState = PaginationState(),
            )
        }
        loadAlbums()
    }

    private fun navigateToAlbum(albumId: String) {
        sendSideEffect(HomeSideEffect.NavigateToAlbumDetail(albumId))
    }
}
