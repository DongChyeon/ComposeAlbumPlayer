package com.dongchyeon.feature.album

import com.dongchyeon.domain.model.Album
import com.dongchyeon.domain.model.Track

data class AlbumUiState(
    val album: Album? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

sealed interface AlbumIntent {
    data object Retry : AlbumIntent
    data class PlayTrack(val track: Track) : AlbumIntent
    data object NavigateBack : AlbumIntent
}

sealed interface AlbumSideEffect {
    data class NavigateToPlayer(val track: Track) : AlbumSideEffect
    data object NavigateBack : AlbumSideEffect
}
