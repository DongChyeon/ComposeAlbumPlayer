package com.dongchyeon.feature.home

import com.dongchyeon.domain.model.Album

data class PaginationState(
    val currentPage: Int = 0,
    val isLoadingMore: Boolean = false,
    val hasMoreData: Boolean = true
) {
    companion object {
        const val PAGE_SIZE = 10
    }
}

data class HomeUiState(
    val albums: List<Album> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val paginationState: PaginationState = PaginationState()
)

sealed interface HomeIntent {
    data object LoadAlbums : HomeIntent
    data object LoadMoreAlbums : HomeIntent
    data object Retry : HomeIntent
    data class NavigateToAlbum(val albumId: String) : HomeIntent
}

sealed interface HomeSideEffect {
    data class NavigateToAlbumDetail(val albumId: String) : HomeSideEffect
}
