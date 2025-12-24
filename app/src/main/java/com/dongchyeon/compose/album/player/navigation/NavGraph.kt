package com.dongchyeon.compose.album.player.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.dongchyeon.domain.model.Track
import com.dongchyeon.feature.album.AlbumRoute
import com.dongchyeon.feature.home.HomeRoute
import com.dongchyeon.feature.player.PlayerRoute

// Shared track data holder for navigation
object TrackNavigationHolder {
    var currentTrack: Track? = null
}

@Composable
fun NavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier
    ) {
        composable(route = Screen.Home.route) {
            HomeRoute(
                onNavigateToAlbum = { albumId ->
                    navController.navigate(Screen.Album.createRoute(albumId))
                }
            )
        }
        
        composable(
            route = Screen.Album.route,
            arguments = listOf(
                navArgument("albumId") { type = NavType.StringType }
            )
        ) {
            AlbumRoute(
                onNavigateBack = { navController.navigateUp() },
                onNavigateToPlayer = { track ->
                    TrackNavigationHolder.currentTrack = track
                    navController.navigate(Screen.Player.route)
                }
            )
        }
        
        composable(route = Screen.Player.route) {
            PlayerRoute(
                track = TrackNavigationHolder.currentTrack,
                onNavigateBack = {
                    TrackNavigationHolder.currentTrack = null
                    navController.navigateUp()
                }
            )
        }
    }
}

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    
    data object Album : Screen("album/{albumId}") {
        fun createRoute(albumId: String) = "album/$albumId"
    }
    
    data object Player : Screen("player")
}
