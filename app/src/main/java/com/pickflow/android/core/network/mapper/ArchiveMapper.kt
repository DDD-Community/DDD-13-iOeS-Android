package com.pickflow.android.core.network.mapper

import com.pickflow.android.core.network.dto.archive.ArchiveImageResponseDto
import com.pickflow.android.core.services.protocols.Archive

fun ArchiveImageResponseDto.toArchive(): Archive = Archive(
    name = archiveName,
    imageUrl = archiveImageUrl?.takeIf { it.isNotBlank() },
)
