package com.pickflow.android.feature.notice

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
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
            // targetSdk 35(edge-to-edge 강제)에서 하단 콘텐츠가 내비게이션 바에 가려지는 문제.
            // 스팟 상세(24e8390)와 동일 원인 — 같은 방식으로 루트에서 inset 을 먹인다.
            .navigationBarsPadding()
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
