package com.dongchyeon.domain.model

data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val duration: Long, // in milliseconds
    val streamUrl: String,
    val artworkUrl: String?,
    val albumId: String? = null
) {
    companion object {
        // Default empty track for serialization
        val Empty = Track(
            id = "",
            title = "",
            artist = "",
            duration = 0L,
            streamUrl = "",
            artworkUrl = null,
            albumId = null
        )
    }
}
