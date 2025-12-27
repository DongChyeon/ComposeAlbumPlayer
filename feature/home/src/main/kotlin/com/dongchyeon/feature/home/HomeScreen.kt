package com.dongchyeon.feature.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.SubcomposeAsyncImage
import com.dongchyeon.core.designsystem.theme.AlbumPlayerTheme
import com.dongchyeon.core.designsystem.theme.Spacing
import com.dongchyeon.core.ui.components.ErrorMessage
import com.dongchyeon.core.ui.components.LoadingIndicator
import com.dongchyeon.domain.model.Album
import com.dongchyeon.feature.home.component.RotaryWheelPicker

@Composable
fun HomeRoute(
    onNavigateToAlbum: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    LaunchedEffect(Unit) {
        viewModel.sideEffect.collect { sideEffect ->
            when (sideEffect) {
                is HomeSideEffect.NavigateToAlbumDetail -> {
                    onNavigateToAlbum(sideEffect.albumId)
                }
            }
        }
    }
    
    HomeScreen(
        uiState = uiState,
        onIntent = viewModel::handleIntent
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onIntent: (HomeIntent) -> Unit
) {
    when {
        uiState.isLoading -> {
            LoadingIndicator()
        }
        uiState.error != null -> {
            ErrorMessage(
                message = uiState.error,
                onRetry = { onIntent(HomeIntent.Retry) }
            )
        }
        else -> {
            AlbumList(
                albums = uiState.albums,
                onAlbumClick = { albumId ->
                    onIntent(HomeIntent.NavigateToAlbum(albumId))
                },
                onLoadMore = {
                    onIntent(HomeIntent.LoadMoreAlbums)
                }
            )
        }
    }
}

@Composable
fun AlbumList(
    albums: List<Album>,
    onAlbumClick: (String) -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (albums.isEmpty()) return

    RotaryWheelPicker(
        items = albums,
        itemContent = { album, isSelected, itemModifier ->
            AlbumItem(
                album = album,
                onClick = { onAlbumClick(album.id) },
                isSelected = isSelected,
                modifier = itemModifier
            )
        },
        onLoadMore = onLoadMore,
        modifier = modifier,
    )
}

@Composable
fun AlbumItem(
    album: Album,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false
) {
    val fallbackContent = remember(album, isSelected) {
        @Composable {
            Column(
                modifier = Modifier.padding(Spacing.medium)
            ) {
                Text(
                    text = album.title,
                    style = AlbumPlayerTheme.typography.titleLarge,
                    color = if (isSelected)
                        AlbumPlayerTheme.colorScheme.gray50
                    else
                        AlbumPlayerTheme.colorScheme.gray200
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = album.artist,
                    style = AlbumPlayerTheme.typography.bodyMedium,
                    color = if (isSelected)
                        AlbumPlayerTheme.colorScheme.gray100
                    else
                        AlbumPlayerTheme.colorScheme.gray400
                )
            }
        }
    }
    
    Box(
        modifier = modifier
            .size(200.dp)
            .border(
                border = BorderStroke(
                    width = 1.dp,
                    color = Color.Gray
                ),
                shape = RoundedCornerShape(4.dp)
            )
            .clip(
                shape = RoundedCornerShape(4.dp)
            )
            .clickable(onClick = onClick)
    ) {
        if (album.artworkUrl.isNotEmpty()) {
            SubcomposeAsyncImage(
                model = album.artworkUrl,
                contentDescription = album.title,
                modifier = Modifier.size(200.dp),
                error = { fallbackContent() }
            )
        } else {
            fallbackContent()
        }

        Box(
            modifier = Modifier
                .size(200.dp)
                .background(
                    color = Color.White.copy(alpha = 0.2f)
                )
                .clip(
                    shape = RoundedCornerShape(4.dp)
                )
        )
    }
}
