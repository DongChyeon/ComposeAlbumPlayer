package com.dongchyeon.feature.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dongchyeon.core.designsystem.theme.AlbumPlayerTheme
import com.dongchyeon.core.ui.components.AlbumItem
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
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        AlbumPlayerTheme.colorScheme.main1.copy(alpha = 0.3f),
                        AlbumPlayerTheme.colorScheme.background
                    ),
                    radius = 1000f
                )
            ),
        contentAlignment = Alignment.Center
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
                Box(modifier = Modifier.fillMaxSize()) {
                    AlbumList(
                        albums = uiState.albums,
                        onAlbumClick = { albumId ->
                            onIntent(HomeIntent.NavigateToAlbum(albumId))
                        },
                        onLoadMore = {
                            onIntent(HomeIntent.LoadMoreAlbums)
                        },
                        onScrollStarted = {
                            if (uiState.showScrollHint) {
                                onIntent(HomeIntent.DismissScrollHint)
                            }
                        }
                    )
                    
                    // 스크롤 힌트 오버레이
                    AnimatedVisibility(
                        visible = uiState.showScrollHint,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier
                            .align(Alignment.Center)
                    ) {
                        ScrollHint()
                    }
                }
            }
        }
    }
}

@Composable
fun ScrollHint() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .background(
                color = AlbumPlayerTheme.colorScheme.background.copy(alpha = 0.8f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 32.dp, vertical = 24.dp)
    ) {
        Icon(
            imageVector = Icons.Default.KeyboardArrowUp,
            contentDescription = null,
            tint = AlbumPlayerTheme.colorScheme.main2,
            modifier = Modifier.size(40.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Scroll & Click",
            style = AlbumPlayerTheme.typography.titleMedium,
            color = AlbumPlayerTheme.colorScheme.gray50,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "to select Album",
            style = AlbumPlayerTheme.typography.bodySmall,
            color = AlbumPlayerTheme.colorScheme.gray400,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Icon(
            imageVector = Icons.Default.KeyboardArrowDown,
            contentDescription = null,
            tint = AlbumPlayerTheme.colorScheme.main2,
            modifier = Modifier.size(40.dp)
        )
    }
}

@Composable
fun AlbumList(
    albums: List<Album>,
    onAlbumClick: (String) -> Unit,
    onLoadMore: () -> Unit,
    onScrollStarted: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (albums.isEmpty()) return

    RotaryWheelPicker(
        items = albums,
        itemContent = { album, isSelected, itemModifier ->
            AlbumItem(
                title = album.title,
                artist = album.artist,
                artworkUrl = album.artworkUrl,
                onClick = { onAlbumClick(album.id) },
                isSelected = isSelected,
                modifier = itemModifier.size(200.dp)
            )
        },
        onLoadMore = onLoadMore,
        onScrollStarted = onScrollStarted,
        modifier = modifier,
    )
}
