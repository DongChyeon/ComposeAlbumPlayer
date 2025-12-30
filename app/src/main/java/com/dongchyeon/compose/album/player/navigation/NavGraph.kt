package com.dongchyeon.compose.album.player.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.dongchyeon.feature.album.navigation.AlbumDestination
import com.dongchyeon.feature.album.navigation.albumGraph
import com.dongchyeon.feature.home.HomeRoute

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
        // Home Screen
        composable(route = Screen.Home.route) {
            HomeRoute(
                onNavigateToAlbum = { albumId ->
                    navController.navigate(AlbumDestination.createGraphRoute(albumId))
                },
            )
        }

        // Album Graph (Album + Player)
        // PlaybackViewModel을 Album과 Player 화면에서 공유
        albumGraph(
            navController = navController,
            onNavigateBack = { navController.navigateUp() },
        )
    }
}

sealed class Screen(val route: String) {
    data object Home : Screen("home")
}
