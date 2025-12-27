package com.dongchyeon.feature.album

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dongchyeon.core.designsystem.theme.Spacing
import com.dongchyeon.core.ui.components.ErrorMessage
import com.dongchyeon.core.ui.components.LoadingIndicator
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
    album: com.dongchyeon.domain.model.Album,
    onTrackClick: (Track) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(Spacing.medium),
        verticalArrangement = Arrangement.spacedBy(Spacing.medium)
    ) {
        item {
            AlbumHeader(album = album)
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
fun AlbumHeader(
    album: com.dongchyeon.domain.model.Album,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = Spacing.extraSmall)
    ) {
        Column(
            modifier = Modifier.padding(Spacing.large)
        ) {
            Text(
                text = album.title,
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = album.artist,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            album.releaseDate?.let { date ->
                Text(
                    text = date,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
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
                tint = MaterialTheme.colorScheme.primary
            )
            
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = Spacing.medium)
            ) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = track.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Text(
                text = formatDuration(track.duration),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
