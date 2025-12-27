package com.dongchyeon.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ArtworkDto(
    @SerialName("150x150")
    val small: String? = null,
    @SerialName("480x480")
    val medium: String? = null,
    @SerialName("1000x1000")
    val large: String? = null
)
