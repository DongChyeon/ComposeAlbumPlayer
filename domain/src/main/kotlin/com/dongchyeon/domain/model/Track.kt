package com.dongchyeon.domain.model

data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val duration: Long,
    val streamUrl: String,
    val artworkUrl: String,
    val albumId: String? = null,
) {
    companion object {
        val Empty =
            Track(
                id = "",
                title = "",
                artist = "",
                duration = 0L,
                streamUrl = "",
                artworkUrl = "",
                albumId = null,
            )
    }
}
