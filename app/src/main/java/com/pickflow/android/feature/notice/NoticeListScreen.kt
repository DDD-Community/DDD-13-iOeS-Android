package com.pickflow.android.feature.notice

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pickflow.android.common.designsystem.PickflowColors
import com.pickflow.android.common.designsystem.PickflowTypography
import com.pickflow.android.common.ui.LoadStateContent
import com.pickflow.android.core.services.protocols.BoardPost
import com.pickflow.android.feature.notice.components.NoticeRow

/** 공지사항 목록 — `BoardService.posts(masterId)` 페이지 누적. */
@Composable
fun NoticeListScreen(
    onBack: () -> Unit,
    onOpenDetail: (Long) -> Unit,
    viewModel: NoticeListViewModel = hiltViewModel(),
) {
    val posts by viewModel.posts.collectAsStateWithLifecycle()
    val isLoadingNextPage by viewModel.isLoadingNextPage.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.refresh() }

    NoticeListContent(
        state = posts,
        isLoadingNextPage = isLoadingNextPage,
        onBack = onBack,
        onOpenDetail = onOpenDetail,
        onReachEnd = viewModel::loadNextPage,
        onRetry = viewModel::refresh,
    )
}

/** Stateless 본체 — UI 테스트/Paparazzi 직접 호출. */
@Composable
fun NoticeListContent(
    state: com.pickflow.android.common.ui.LoadState<List<BoardPost>>,
    isLoadingNextPage: Boolean = false,
    onBack: () -> Unit = {},
    onOpenDetail: (Long) -> Unit = {},
    onReachEnd: () -> Unit = {},
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
            .testTag("notice-list-screen"),
    ) {
        NoticeTopBar(title = "공지사항", onBack = onBack)
        LoadStateContent(
            state = state,
            emptyMessage = "등록된 공지사항이 없어요.",
            onRetry = onRetry,
        ) { items ->
            NoticeList(
                items = items,
                isLoadingNextPage = isLoadingNextPage,
                onOpenDetail = onOpenDetail,
                onReachEnd = onReachEnd,
            )
        }
    }
}

@Composable
private fun NoticeList(
    items: List<BoardPost>,
    isLoadingNextPage: Boolean,
    onOpenDetail: (Long) -> Unit,
    onReachEnd: () -> Unit,
) {
    val listState = rememberLazyListState()
    // SpotListScreen 과 동일한 버그 — 키 없는 remember 가 첫 `items` 를 붙잡아 페이지가 늘어도
    // 임계값이 갱신되지 않는다. totalItemsCount 로 읽어 캡처를 없앤다.
    val reachedEnd by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: -1
            info.totalItemsCount > 0 && last >= info.totalItemsCount - 3
        }
    }
    LaunchedEffect(reachedEnd) { if (reachedEnd) onReachEnd() }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .testTag("notice-list"),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        items(items, key = { it.postId }) { post ->
            NoticeRow(post = post, onClick = { onOpenDetail(post.postId) })
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .height(1.dp)
                    .background(PickflowColors.gray80),
            )
        }
        if (isLoadingNextPage) {
            item(key = "notice-paging-loading") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                        .testTag("notice-paging-loading"),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = PickflowColors.gray0)
                }
            }
        }
    }
}

@Composable
internal fun NoticeTopBar(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .testTag("notice-topbar"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clickable(onClick = onBack)
                .testTag("notice-back"),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "뒤로가기",
                tint = PickflowColors.gray0,
                modifier = Modifier.size(24.dp),
            )
        }
        Text(
            text = title,
            style = PickflowTypography.bodyLargeBold,
            color = PickflowColors.gray0,
        )
    }
}
