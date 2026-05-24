package com.pickflow.android.core.services.protocols

data class Spot(
    val id: String,
    val name: String,
    val theme: SpotTheme,
    val latitude: Double,
    val longitude: Double,
    val imageUrl: String? = null,
    val address: String = "",
)

enum class SpotTheme { CAFE, RESTAURANT, BAR, ACTIVITY, NATURE }

data class SpotPage(
    val items: List<Spot>,
    val nextCursor: String?,
)
