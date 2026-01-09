package com.dongchyeon.compose.album.player

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.dongchyeon.core.designsystem.theme.AlbumPlayerTheme
import com.dongchyeon.feature.album.AlbumRoute
import com.dongchyeon.feature.album.navigation.AlbumNavKey
import com.dongchyeon.feature.home.HomeRoute
import com.dongchyeon.feature.home.navigation.HomeNavKey
import com.dongchyeon.feature.player.PlayerRoute
import com.dongchyeon.feature.player.navigation.PlayerNavKey

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumPlayerApp(modifier: Modifier = Modifier) {
    val backStack = rememberSaveable { mutableStateListOf<Any>(HomeNavKey) }
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = AlbumPlayerTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { paddingValues ->
        NavDisplay(
            backStack = backStack,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            onBack = { backStack.removeLastOrNull() },
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
            entryProvider = entryProvider {
                entry<HomeNavKey> {
                    HomeRoute(
                        onNavigateToAlbum = { albumId ->
                            backStack.add(AlbumNavKey(albumId = albumId))
                        },
                    )
                }

                entry<AlbumNavKey> { destination ->
                    AlbumRoute(
                        albumId = destination.albumId,
                        snackbarHostState = snackbarHostState,
                        onNavigateToPlayer = {
                            backStack.add(PlayerNavKey)
                        },
                        onNavigateBack = { backStack.removeLastOrNull() },
                    )
                }

                entry<PlayerNavKey> {
                    PlayerRoute(
                        snackbarHostState = snackbarHostState,
                        onNavigateBack = { backStack.removeLastOrNull() },
                    )
                }
            },
        )
    }
}
