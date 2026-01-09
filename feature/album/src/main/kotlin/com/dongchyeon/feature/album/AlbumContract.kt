package com.dongchyeon.feature.album

import com.dongchyeon.domain.model.Album
import com.dongchyeon.domain.model.Track

data class AlbumUiState(
    val album: Album? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
)

sealed interface AlbumIntent {
    data object Retry : AlbumIntent
    data object NavigateBack : AlbumIntent
    data class NavigateToPlayer(val track: Track) : AlbumIntent
}

sealed interface AlbumSideEffect {
    data object NavigateBack : AlbumSideEffect
    data object NavigateToPlayer : AlbumSideEffect
    data class ShowErrorAndNavigateBack(val message: String) : AlbumSideEffect
}
