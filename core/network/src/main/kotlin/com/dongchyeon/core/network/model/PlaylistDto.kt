package com.dongchyeon.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PlaylistResponse(
    @SerialName("data")
    val data: List<PlaylistDto>? = null
)

@Serializable
data class AlbumResponse(
    @SerialName("data")
    val data: PlaylistDto? = null
)

@Serializable
data class PlaylistDto(
    @SerialName("id")
    val id: String,
    @SerialName("playlist_name")
    val playlistName: String,
    @SerialName("playlist_contents")
    val playlistContents: PlaylistContents? = null,
    @SerialName("user")
    val user: UserDto? = null,
    @SerialName("artwork")
    val artwork: ArtworkDto? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("description")
    val description: String? = null
)

@Serializable
data class PlaylistContents(
    @SerialName("track_ids")
    val trackIds: List<TrackIdDto>? = null
)

@Serializable
data class TrackIdDto(
    @SerialName("track")
    val track: String,
    @SerialName("time")
    val time: Long? = null
)
