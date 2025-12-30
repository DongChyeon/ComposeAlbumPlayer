package com.dongchyeon.compose.album.player.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.dongchyeon.feature.album.AlbumRoute
import com.dongchyeon.feature.home.HomeRoute
import com.dongchyeon.feature.player.PlayerRoute

@Composable
fun NavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier,
    ) {
        composable(route = Screen.Home.route) {
            HomeRoute(
                onNavigateToAlbum = { albumId ->
                    navController.navigate(Screen.Album.createRoute(albumId))
                },
            )
        }

        composable(
            route = Screen.Album.route,
            arguments = listOf(
                navArgument("albumId") { type = NavType.StringType },
            ),
        ) {
            AlbumRoute(
                onNavigateBack = { navController.navigateUp() },
                onNavigateToPlayer = { track ->
                    navController.navigate(Screen.Player.createRoute(track.id))
                },
            )
        }

        composable(
            route = Screen.Player.route,
            arguments = listOf(
                navArgument("trackId") { type = NavType.StringType },
            ),
        ) {
            PlayerRoute(
                onNavigateBack = { navController.navigateUp() },
            )
        }
    }
}

sealed class Screen(val route: String) {
    data object Home : Screen("home")

    data object Album : Screen("album/{albumId}") {
        fun createRoute(albumId: String) = "album/$albumId"
    }

    data object Player : Screen("player/{trackId}") {
        fun createRoute(trackId: String) = "player/$trackId"
    }
}
