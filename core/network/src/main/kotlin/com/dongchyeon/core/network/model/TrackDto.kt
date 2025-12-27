package com.dongchyeon.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TracksResponse(
    @SerialName("data")
    val data: List<TrackDto>
)

@Serializable
data class TrackDto(
    @SerialName("id")
    val id: String,
    @SerialName("title")
    val title: String,
    @SerialName("user")
    val user: UserDto? = null,
    @SerialName("duration")
    val duration: Int,
    @SerialName("artwork")
    val artwork: ArtworkDto? = null,
    @SerialName("permalink")
    val permalink: String? = null,
    @SerialName("stream")
    val stream: StreamDto? = null
)

@Serializable
data class StreamDto(
    @SerialName("url")
    val url: String
)
