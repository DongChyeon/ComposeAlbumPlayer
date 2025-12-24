package com.dongchyeon.domain.model

data class Album(
    val id: String,
    val title: String,
    val artist: String,
    val artworkUrl: String?,
    val releaseDate: String?,
    val tracks: List<Track> = emptyList()
)
