package com.dongchyeon.feature.album

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dongchyeon.core.designsystem.theme.AlbumPlayerTheme
import com.dongchyeon.core.designsystem.theme.Spacing
import com.dongchyeon.core.ui.components.AlbumItem
import com.dongchyeon.core.ui.components.ErrorMessage
import com.dongchyeon.core.ui.components.LoadingIndicator
import com.dongchyeon.domain.model.Album
import com.dongchyeon.domain.model.Track

@Composable
fun AlbumRoute(
    onNavigateBack: () -> Unit,
    onNavigateToPlayer: (Track) -> Unit,
    viewModel: AlbumViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    LaunchedEffect(Unit) {
        viewModel.sideEffect.collect { sideEffect ->
            when (sideEffect) {
                is AlbumSideEffect.NavigateToPlayer -> {
                    onNavigateToPlayer(sideEffect.track)
                }
                is AlbumSideEffect.NavigateBack -> {
                    onNavigateBack()
                }
            }
        }
    }
    
    AlbumScreen(
        uiState = uiState,
        onIntent = viewModel::handleIntent
    )
}

@Composable
fun AlbumScreen(
    uiState: AlbumUiState,
    onIntent: (AlbumIntent) -> Unit
) {
    when {
        uiState.isLoading -> {
            LoadingIndicator()
        }
        uiState.error != null -> {
            ErrorMessage(
                message = uiState.error,
                onRetry = { onIntent(AlbumIntent.Retry) }
            )
        }
        uiState.album != null -> {
            AlbumContent(
                album = uiState.album,
                onTrackClick = { track ->
                    onIntent(AlbumIntent.PlayTrack(track))
                }
            )
        }
    }
}

@Composable
fun AlbumContent(
    album: Album,
    onTrackClick: (Track) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            horizontal = Spacing.medium,
            vertical = Spacing.large
        ),
        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item {
            AlbumItem(
                title = album.title,
                artist = album.artist,
                artworkUrl = album.artworkUrl,
                onClick = { },
                isSelected = false,
                modifier = Modifier
                    .size(200.dp)
            )

            Text(
                text = album.title,
                style = AlbumPlayerTheme.typography.headlineSmall,
                color = AlbumPlayerTheme.colorScheme.gray50,
                modifier = Modifier.padding(top = Spacing.medium)
            )

            Text(
                text = album.artist,
                style = AlbumPlayerTheme.typography.bodyMedium,
                color = AlbumPlayerTheme.colorScheme.gray50,
                modifier = Modifier.padding(top = Spacing.small)
            )
        }
        
        items(album.tracks) { track ->
            TrackItem(
                track = track,
                onClick = { onTrackClick(track) }
            )
        }
    }
}

@Composable
fun TrackItem(
    track: Track,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = Spacing.extraSmall)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.medium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "재생",
                tint = AlbumPlayerTheme.colorScheme.main1
            )
            
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = Spacing.medium)
            ) {
                Text(
                    text = track.title,
                    style = AlbumPlayerTheme.typography.titleMedium
                )
                Text(
                    text = track.artist,
                    style = AlbumPlayerTheme.typography.bodySmall,
                    color = AlbumPlayerTheme.colorScheme.gray400
                )
            }
            
            Text(
                text = formatDuration(track.duration),
                style = AlbumPlayerTheme.typography.bodySmall,
                color = AlbumPlayerTheme.colorScheme.gray400
            )
        }
    }
}

private fun formatDuration(milliseconds: Long): String {
    val totalSeconds = milliseconds / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}

@Preview
@Composable
private fun AlbumScreenPreview() {
    AlbumPlayerTheme {
        AlbumScreen(
            uiState = AlbumUiState(
                album = Album(
                    id = "albumId",
                    title = "Album Title",
                    artist = "Artist Name",
                    artworkUrl = "",
                    releaseDate = null,
                    tracks = listOf(
                        Track(
                            id = "trackId",
                            title = "Track Title",
                            artist = "Artist Name",
                            duration = 180000,
                            streamUrl = "",
                            artworkUrl = ""
                        ),
                        Track(
                            id = "trackId",
                            title = "Track Title",
                            artist = "Artist Name",
                            duration = 180000,
                            streamUrl = "",
                            artworkUrl = ""
                        ),
                        Track(
                            id = "trackId",
                            title = "Track Title",
                            artist = "Artist Name",
                            duration = 180000,
                            streamUrl = "",
                            artworkUrl = ""
                        ),

                    )
                )
            ),
            onIntent = { }
        )
    }
}