package com.dongchyeon.feature.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dongchyeon.core.designsystem.theme.Spacing
import com.dongchyeon.domain.model.Track

@Composable
fun PlayerRoute(
    track: Track?,
    onNavigateBack: () -> Unit,
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    
    LaunchedEffect(track) {
        track?.let {
            viewModel.handleIntent(PlayerIntent.InitializePlayer(it))
        }
    }
    
    LaunchedEffect(Unit) {
        viewModel.sideEffect.collect { sideEffect ->
            when (sideEffect) {
                is PlayerSideEffect.NavigateBack -> {
                    onNavigateBack()
                }
                is PlayerSideEffect.ShowError -> {
                    snackbarHostState.showSnackbar(sideEffect.message)
                }
            }
        }
    }
    
    PlayerScreen(
        uiState = uiState,
        onIntent = viewModel::handleIntent,
    )
}

@Composable
fun PlayerScreen(
    uiState: PlayerUiState,
    onIntent: (PlayerIntent) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Spacing.large),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
        uiState.currentTrack?.let { track ->
            // Track Info
            Text(
                text = track.title,
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(Spacing.small))
            Text(
                text = track.artist,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(Spacing.extraLarge))

            // Progress Bar
            Column(modifier = Modifier.fillMaxWidth()) {
                Slider(
                    value = if (uiState.duration > 0) {
                        uiState.currentPosition.toFloat() / uiState.duration.toFloat()
                    } else 0f,
                    onValueChange = { value ->
                        val position = (value * uiState.duration).toLong()
                        onIntent(PlayerIntent.SeekTo(position))
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatDuration(uiState.currentPosition),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = formatDuration(uiState.duration),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.large))

            // Playback Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { onIntent(PlayerIntent.Previous) },
                    modifier = Modifier.size(64.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "이전",
                        modifier = Modifier.size(48.dp)
                    )
                }

                IconButton(
                    onClick = { onIntent(PlayerIntent.PlayPause) },
                    modifier = Modifier.size(80.dp)
                ) {
                    Icon(
                        imageVector = if (uiState.isPlaying) {
                            Icons.Default.Pause
                        } else {
                            Icons.Default.PlayArrow
                        },
                        contentDescription = if (uiState.isPlaying) "일시정지" else "재생",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(
                    onClick = { onIntent(PlayerIntent.Next) },
                    modifier = Modifier.size(64.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "다음",
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
        } ?: run {
            Text(
                text = "재생할 트랙이 없습니다",
                style = MaterialTheme.typography.bodyLarge
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
