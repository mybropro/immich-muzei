package dev.abdus.apps.immich.data

import kotlinx.serialization.Serializable

@Serializable
data class ImmichPersonUiModel(
    val id: String,
    val name: String,
    val thumbnailUrl: String? = null
)
