package com.dongchyeon.compose.album.player

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.dongchyeon.compose.album.player.navigation.NavGraph
import com.dongchyeon.core.designsystem.theme.AlbumPlayerTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumPlayerApp(modifier: Modifier = Modifier) {
    val navController = rememberNavController()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = AlbumPlayerTheme.colorScheme.background,
    ) { paddingValues ->
        NavGraph(
            navController = navController,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        )
    }
}
