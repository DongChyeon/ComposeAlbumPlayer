package com.dongchyeon.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PlaylistResponse(
    @SerialName("data")
    val data: List<PlaylistDto>? = null,
)

@Serializable
data class AlbumResponse(
    @SerialName("data")
    val data: List<PlaylistDto>? = null,
)

@Serializable
data class PlaylistDto(
    @SerialName("id")
    val id: String,
    @SerialName("playlist_name")
    val playlistName: String,
    @SerialName("playlist_contents")
    val playlistContents: List<PlaylistContentDto>? = null,
    @SerialName("user")
    val user: UserDto? = null,
    @SerialName("artwork")
    val artwork: ArtworkDto? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("description")
    val description: String? = null,
    @SerialName("track_count")
    val trackCount: Int? = null,
    @SerialName("tracks")
    val tracks: List<TrackDto>? = null,
)

@Serializable
data class PlaylistContentDto(
    @SerialName("track_id")
    val trackId: String,
    @SerialName("timestamp")
    val timestamp: Long,
    @SerialName("metadata_timestamp")
    val metadataTimestamp: Long,
)
