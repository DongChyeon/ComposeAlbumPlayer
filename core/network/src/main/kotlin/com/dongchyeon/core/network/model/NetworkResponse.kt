package com.dongchyeon.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NetworkResponse<T>(
    @SerialName("data")
    val data: T? = null,
    @SerialName("error")
    val error: String? = null,
)
