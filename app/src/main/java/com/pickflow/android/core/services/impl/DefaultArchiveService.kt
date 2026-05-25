package com.pickflow.android.core.services.impl

import com.pickflow.android.core.network.api.ArchiveApi
import com.pickflow.android.core.network.mapper.toArchive
import com.pickflow.android.core.network.unwrap
import com.pickflow.android.core.services.protocols.Archive
import com.pickflow.android.core.services.protocols.ArchiveService
import javax.inject.Inject

class DefaultArchiveService @Inject constructor(
    private val archiveApi: ArchiveApi,
) : ArchiveService {
    override suspend fun fetch(): Archive =
        archiveApi.getArchive().unwrap().toArchive()
}
