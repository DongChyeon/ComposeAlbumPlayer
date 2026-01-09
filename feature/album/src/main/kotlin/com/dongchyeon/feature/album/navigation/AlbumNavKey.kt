package com.dongchyeon.feature.album.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data class AlbumNavKey(val albumId: String) : NavKey
