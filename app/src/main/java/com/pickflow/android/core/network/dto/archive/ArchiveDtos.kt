package com.pickflow.android.core.network.dto.archive

import kotlinx.serialization.Serializable

@Serializable
data class ArchiveImageResponseDto(
    val archiveName: String = "",
    val archiveImageUrl: String? = null,
)
