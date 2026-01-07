package com.dongchyeon.feature.album.navigation

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navigation
import com.dongchyeon.feature.album.AlbumPlayerSideEffect
import com.dongchyeon.feature.album.AlbumPlayerViewModel
import com.dongchyeon.feature.album.AlbumRoute
import com.dongchyeon.feature.album.PlayerRoute
import kotlinx.coroutines.launch

fun NavGraphBuilder.albumGraph(
    navController: NavController,
    snackbarHostState: SnackbarHostState,
    onNavigateBack: () -> Unit,
) {
    navigation(
        startDestination = AlbumDestination.Album.route,
        route = AlbumDestination.GRAPH_ROUTE,
    ) {
        composable(
            route = AlbumDestination.Album.route,
            arguments = listOf(
                navArgument("albumId") { type = NavType.StringType },
            ),
        ) { backStackEntry ->
            val parentEntry = remember(backStackEntry) {
                navController.getBackStackEntry(AlbumDestination.GRAPH_ROUTE)
            }
            val viewModel: AlbumPlayerViewModel = hiltViewModel(parentEntry)
            val scope = rememberCoroutineScope()

            LaunchedEffect(Unit) {
                viewModel.sideEffect.collect { sideEffect ->
                    when (sideEffect) {
                        is AlbumPlayerSideEffect.NavigateBack -> onNavigateBack()
                        is AlbumPlayerSideEffect.NavigateToPlayer -> {
                            navController.navigate(AlbumDestination.Player.createRoute(sideEffect.track.id))
                        }
                        is AlbumPlayerSideEffect.ShowPlaybackError -> {
                            scope.launch {
                                snackbarHostState.showSnackbar(sideEffect.message)
                            }
                        }
                        is AlbumPlayerSideEffect.ShowErrorAndNavigateBack -> {
                            scope.launch {
                                snackbarHostState.showSnackbar(sideEffect.message)
                                onNavigateBack()
                            }
                        }
                    }
                }
            }

            AlbumRoute(
                viewModel = viewModel,
            )
        }

        composable(
            route = AlbumDestination.Player.route,
            arguments = listOf(
                navArgument("trackId") { type = NavType.StringType },
            ),
        ) { backStackEntry ->
            val parentEntry = remember(backStackEntry) {
                navController.getBackStackEntry(AlbumDestination.GRAPH_ROUTE)
            }
            val viewModel: AlbumPlayerViewModel = hiltViewModel(parentEntry)
            val scope = rememberCoroutineScope()

            LaunchedEffect(Unit) {
                viewModel.sideEffect.collect { sideEffect ->
                    when (sideEffect) {
                        is AlbumPlayerSideEffect.ShowPlaybackError -> {
                            scope.launch {
                                snackbarHostState.showSnackbar(sideEffect.message)
                            }
                        }
                        is AlbumPlayerSideEffect.ShowErrorAndNavigateBack -> {
                            scope.launch {
                                snackbarHostState.showSnackbar(sideEffect.message)
                                onNavigateBack()
                            }
                        }
                        else -> { /* 앨범 화면에서 처리 */ }
                    }
                }
            }

            PlayerRoute(
                viewModel = viewModel,
            )
        }
    }
}

object AlbumDestination {
    const val GRAPH_ROUTE = "album_graph/{albumId}"

    data object Album : Destination("album/{albumId}") {
        fun createRoute(albumId: String) = "album/$albumId"
    }

    data object Player : Destination("player/{trackId}") {
        fun createRoute(trackId: String) = "player/$trackId"
    }

    fun createGraphRoute(albumId: String) = "album_graph/$albumId"
}

sealed class Destination(val route: String)
