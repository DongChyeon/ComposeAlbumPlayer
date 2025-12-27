package com.dongchyeon.core.data.mapper

import com.dongchyeon.core.network.model.TrackDto
import com.dongchyeon.domain.model.Track

fun TrackDto.toDomain(albumId: String? = null): Track {
    return Track(
        id = id,
        title = title,
        artist = user?.name ?: "Unknown Artist",
        duration = duration.toLong() * 1000, // Convert seconds to milliseconds
        streamUrl = streamUrl ?: "",
        artworkUrl = artwork?.large ?: artwork?.medium ?: artwork?.small,
        albumId = albumId
    )
}

fun List<TrackDto>.toDomain(albumId: String? = null): List<Track> {
    return map { it.toDomain(albumId) }
}
