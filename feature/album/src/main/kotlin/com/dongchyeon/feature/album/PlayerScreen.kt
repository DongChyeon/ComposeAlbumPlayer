package com.dongchyeon.feature.album

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dongchyeon.core.designsystem.theme.AlbumPlayerTheme
import com.dongchyeon.core.designsystem.theme.Spacing
import com.dongchyeon.core.ui.components.AlbumItem
import com.dongchyeon.core.ui.components.LoadingIndicator
import com.dongchyeon.domain.model.RepeatMode
import com.dongchyeon.domain.model.ShuffleMode
import com.dongchyeon.domain.model.Track

@Composable
fun PlayerRoute(
    viewModel: AlbumPlayerViewModel,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    PlayerScreen(
        uiState = uiState,
        onIntent = viewModel::handleIntent,
    )
}

@Composable
fun PlayerScreen(
    uiState: AlbumPlayerUiState,
    onIntent: (AlbumPlayerIntent) -> Unit,
) {
    when {
        uiState.isLoading -> {
            LoadingIndicator()
        }
        uiState.error != null -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(Spacing.large),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = uiState.error,
                    style = AlbumPlayerTheme.typography.bodyLarge,
                    color = AlbumPlayerTheme.colorScheme.gray400,
                    textAlign = TextAlign.Center,
                )
            }
        }
        uiState.currentTrack != null -> {
            PlayerContent(
                uiState = uiState,
                onIntent = onIntent,
            )
        }
        else -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(Spacing.large),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "There is no track to play.",
                    style = AlbumPlayerTheme.typography.bodyLarge,
                    color = AlbumPlayerTheme.colorScheme.gray400,
                )
            }
        }
    }
}

@Composable
private fun PlayerContent(
    uiState: AlbumPlayerUiState,
    onIntent: (AlbumPlayerIntent) -> Unit,
) {
    val track = uiState.currentTrack ?: return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Spacing.large),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        AlbumItem(
            title = track.title,
            artist = track.artist,
            artworkUrl = track.artworkUrl,
            onClick = { },
            isSelected = false,
            modifier = Modifier.size(300.dp),
        )
        Spacer(modifier = Modifier.height(Spacing.medium))

        // Track Info
        Text(
            text = track.title,
            style = AlbumPlayerTheme.typography.headlineMedium,
            color = AlbumPlayerTheme.colorScheme.gray50,
            textAlign = TextAlign.Center,
            modifier = Modifier.basicMarquee(),
        )
        Spacer(modifier = Modifier.height(Spacing.small))
        Text(
            text = track.artist,
            style = AlbumPlayerTheme.typography.bodyLarge,
            color = AlbumPlayerTheme.colorScheme.gray400,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(Spacing.extraLarge))

        ProgressBar(
            currentPositionSeconds = uiState.currentPositionSeconds,
            durationSeconds = uiState.durationSeconds,
            durationMs = uiState.duration,
            onSeek = { position ->
                onIntent(AlbumPlayerIntent.SeekTo(position))
            },
        )

        Spacer(modifier = Modifier.height(Spacing.large))

        // Playback Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = { onIntent(AlbumPlayerIntent.ToggleShuffle) },
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Shuffle,
                    contentDescription = "셔플",
                    tint = if (uiState.shuffleMode == ShuffleMode.ON) {
                        AlbumPlayerTheme.colorScheme.main1
                    } else {
                        AlbumPlayerTheme.colorScheme.gray400
                    },
                    modifier = Modifier.size(32.dp),
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = { onIntent(AlbumPlayerIntent.SkipToPrevious) },
                modifier = Modifier.size(80.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.SkipPrevious,
                    contentDescription = "이전",
                    modifier = Modifier.size(48.dp),
                )
            }

            IconButton(
                onClick = { onIntent(AlbumPlayerIntent.TogglePlayPause) },
                modifier = Modifier.size(80.dp),
            ) {
                Icon(
                    imageVector = if (uiState.isPlaying) {
                        Icons.Default.Pause
                    } else {
                        Icons.Default.PlayArrow
                    },
                    contentDescription = if (uiState.isPlaying) "일시정지" else "재생",
                    modifier = Modifier.size(64.dp),
                    tint = AlbumPlayerTheme.colorScheme.main1,
                )
            }

            IconButton(
                onClick = { onIntent(AlbumPlayerIntent.SkipToNext) },
                modifier = Modifier.size(64.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = "다음",
                    modifier = Modifier.size(48.dp),
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = { onIntent(AlbumPlayerIntent.ToggleRepeatMode) },
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    imageVector = when (uiState.repeatMode) {
                        RepeatMode.ONE -> Icons.Default.RepeatOne
                        else -> Icons.Default.Repeat
                    },
                    contentDescription = "반복",
                    tint = if (uiState.repeatMode != RepeatMode.NONE) {
                        AlbumPlayerTheme.colorScheme.main1
                    } else {
                        AlbumPlayerTheme.colorScheme.gray400
                    },
                    modifier = Modifier.size(32.dp),
                )
            }
        }
    }
}

@Composable
private fun ProgressBar(
    currentPositionSeconds: Int,
    durationSeconds: Int,
    durationMs: Long,
    onSeek: (Long) -> Unit,
) {
    // progress 계산
    val progress = if (durationSeconds > 0) {
        currentPositionSeconds.toFloat() / durationSeconds.toFloat()
    } else {
        0f
    }

    val onValueChange = remember(durationMs, onSeek) {
        { value: Float ->
            onSeek((value * durationMs).toLong())
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Slider(
            value = progress,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = formatDuration(currentPositionSeconds * 1000L),
                style = AlbumPlayerTheme.typography.bodySmall,
                color = AlbumPlayerTheme.colorScheme.gray400,
            )
            Text(
                text = formatDuration(durationSeconds * 1000L),
                style = AlbumPlayerTheme.typography.bodySmall,
                color = AlbumPlayerTheme.colorScheme.gray400,
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
private fun PlayerScreenPreview() {
    AlbumPlayerTheme {
        PlayerScreen(
            uiState = AlbumPlayerUiState(
                currentTrack = Track(
                    id = "track123",
                    title = "Sample Track Title",
                    artist = "Sample Artist",
                    duration = 180000,
                    streamUrl = "https://example.com/stream",
                    artworkUrl = "https://example.com/artwork.jpg",
                    albumId = "album123",
                ),
                isPlaying = true,
                currentPosition = 60000,
                duration = 180000,
                currentPositionSeconds = 60,
                durationSeconds = 180,
            ),
            onIntent = { },
        )
    }
}

@Preview
@Composable
private fun PlayerScreenPausedPreview() {
    AlbumPlayerTheme {
        PlayerScreen(
            uiState = AlbumPlayerUiState(
                currentTrack = Track(
                    id = "track123",
                    title = "Sample Track Title",
                    artist = "Sample Artist",
                    duration = 180000,
                    streamUrl = "https://example.com/stream",
                    artworkUrl = "https://example.com/artwork.jpg",
                    albumId = "album123",
                ),
                isPlaying = false,
                currentPosition = 90000,
                duration = 180000,
                currentPositionSeconds = 90,
                durationSeconds = 180,
            ),
            onIntent = { },
        )
    }
}

@Preview
@Composable
private fun PlayerScreenEmptyPreview() {
    AlbumPlayerTheme {
        PlayerScreen(
            uiState = AlbumPlayerUiState(
                currentTrack = null,
            ),
            onIntent = { },
        )
    }
}
