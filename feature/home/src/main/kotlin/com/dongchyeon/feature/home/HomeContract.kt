package com.dongchyeon.feature.home

import com.dongchyeon.domain.model.Album

data class HomeUiState(
    val albums: List<Album> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

sealed interface HomeIntent {
    data object LoadAlbums : HomeIntent
    data object Retry : HomeIntent
    data class NavigateToAlbum(val albumId: String) : HomeIntent
}

sealed interface HomeSideEffect {
    data class NavigateToAlbumDetail(val albumId: String) : HomeSideEffect
}
