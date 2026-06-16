package com.pickflow.android.core.services.impl

import com.pickflow.android.core.network.api.ArchiveApi
import com.pickflow.android.core.network.dto.archive.UpdateArchiveNameRequest
import com.pickflow.android.core.network.mapper.toArchive
import com.pickflow.android.core.network.toMultipartPart
import com.pickflow.android.core.network.unwrap
import com.pickflow.android.core.services.protocols.Archive
import com.pickflow.android.core.services.protocols.ArchiveService
import com.pickflow.android.core.services.protocols.ImagePayload
import javax.inject.Inject

class DefaultArchiveService @Inject constructor(
    private val archiveApi: ArchiveApi,
) : ArchiveService {
    override suspend fun fetch(): Archive =
        archiveApi.getArchive().unwrap().toArchive()

    override suspend fun updateImage(image: ImagePayload): Archive {
        val part = image.toMultipartPart(PART_NAME)
        return archiveApi.updateArchiveImage(part).unwrap().toArchive()
    }

    override suspend fun updateName(name: String): Archive =
        archiveApi.updateArchiveName(UpdateArchiveNameRequest(archiveName = name))
            .unwrap()
            .toArchive()

    private companion object {
        const val PART_NAME = "archiveImage"
    }
}
