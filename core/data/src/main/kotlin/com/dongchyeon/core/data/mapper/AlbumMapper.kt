package com.dongchyeon.core.data.mapper

import com.dongchyeon.core.network.model.PlaylistDto
import com.dongchyeon.domain.model.Album

fun PlaylistDto.toDomain(): Album {
    return Album(
        id = id,
        title = playlistName,
        artist = user?.name ?: "Unknown Artist",
        artworkUrl = artwork?.large ?: artwork?.medium ?: artwork?.small,
        releaseDate = createdAt,
        tracks = tracks?.toDomain(albumId = id) ?: emptyList()
    )
}

fun List<PlaylistDto>.toDomain(): List<Album> {
    return map { it.toDomain() }
}
