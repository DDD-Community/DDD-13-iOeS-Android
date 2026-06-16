package com.pickflow.android.feature.notice

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pickflow.android.common.designsystem.PickflowColors
import com.pickflow.android.common.ui.LoadStateContent
import com.pickflow.android.core.services.protocols.BoardPostDetail
import com.pickflow.android.feature.notice.components.NoticeDetailContent

/** 공지사항 상세 — `BoardService.detail(masterId, postId)` 1회 호출. */
@Composable
fun NoticeDetailScreen(
    postId: Long,
    onBack: () -> Unit,
    viewModel: NoticeDetailViewModel = hiltViewModel(),
) {
    val post by viewModel.post.collectAsStateWithLifecycle()

    LaunchedEffect(postId) { viewModel.load() }

    NoticeDetailContentScreen(state = post, onBack = onBack, onRetry = viewModel::load)
}

/** Stateless 본체. */
@Composable
fun NoticeDetailContentScreen(
    state: com.pickflow.android.common.ui.LoadState<BoardPostDetail>,
    onBack: () -> Unit = {},
    onRetry: () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PickflowColors.gray95)
            .statusBarsPadding()
            .testTag("notice-detail-screen"),
    ) {
        NoticeTopBar(title = "공지사항", onBack = onBack)
        LoadStateContent(
            state = state,
            emptyMessage = "내용이 없어요.",
            onRetry = onRetry,
        ) { detail ->
            NoticeDetailContent(detail = detail)
        }
    }
}
