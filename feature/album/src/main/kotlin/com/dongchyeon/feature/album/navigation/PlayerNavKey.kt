package com.dongchyeon.feature.album.navigation

import kotlinx.serialization.Serializable

@Serializable
data class PlayerNavKey(val albumId: String, val trackId: String)
