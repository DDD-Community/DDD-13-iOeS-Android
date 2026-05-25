package com.pickflow.android.core.services.impl

import com.pickflow.android.core.network.api.BoardApi
import com.pickflow.android.core.network.mapper.toBoardPostPage
import com.pickflow.android.core.network.unwrap
import com.pickflow.android.core.services.protocols.BoardPostPage
import com.pickflow.android.core.services.protocols.BoardService
import javax.inject.Inject

class DefaultBoardService @Inject constructor(
    private val boardApi: BoardApi,
) : BoardService {
    override suspend fun posts(masterId: Long, page: Int): BoardPostPage =
        boardApi.getPosts(masterId = masterId, page = page).unwrap().toBoardPostPage()
}
