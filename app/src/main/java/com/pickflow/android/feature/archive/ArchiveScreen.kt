package com.pickflow.android.feature.archive

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pickflow.android.common.designsystem.PickflowColors
import com.pickflow.android.common.designsystem.PickflowTypography
import com.pickflow.android.common.ui.LoadState
import com.pickflow.android.core.services.protocols.MySpot
import com.pickflow.android.core.services.protocols.MySpotStatus
import com.pickflow.android.core.services.protocols.SavedSpot
import com.pickflow.android.core.services.protocols.SpotTheme
import com.pickflow.android.feature.archive.components.ArchiveEmptyContent
import com.pickflow.android.feature.archive.components.ArchiveHeader
import com.pickflow.android.feature.archive.components.ArchiveMySpotPlaceholderContent
import com.pickflow.android.feature.archive.components.ArchiveRenameDialog
import com.pickflow.android.feature.archive.components.ArchiveSignedOutContent
import com.pickflow.android.feature.archive.components.ArchiveTabBar
import com.pickflow.android.feature.archive.components.rememberCoverImagePickerLauncher
import com.pickflow.android.feature.spotlist.components.SpotListCell
import com.pickflow.android.feature.spotlist.components.SpotListGridItem
import com.pickflow.android.feature.spotlist.components.SpotListMood

private val HeaderHeight = 240.dp

/** SAVED 탭 진입점. iOS `ArchiveView` 1:1 대응. */
@Composable
fun ArchiveScreen(
    onOpenSpotDetail: (String) -> Unit,
    onRequireLogin: () -> Unit,
    onExploreClick: () -> Unit = {},
    onOpenRegistration: () -> Unit = {},
    initialTab: ArchiveTab? = null,
    onInitialTabConsumed: () -> Unit = {},
    viewModel: ArchiveViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val archiveName by viewModel.archiveName.collectAsStateWithLifecycle()
    val archiveImageUrl by viewModel.archiveImageUrl.collectAsStateWithLifecycle()
    val coverImageBytes by viewModel.coverImageBytes.collectAsStateWithLifecycle()
    val toast by viewModel.toast.collectAsStateWithLifecycle()
    val mySpotState by viewModel.mySpots.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.onAppear() }
    // 마이페이지 카드에서 특정 탭으로 진입 요청 시 1회 적용.
    LaunchedEffect(initialTab) {
        initialTab?.let {
            viewModel.tabChanged(it)
            onInitialTabConsumed()
        }
    }

    var showRenameDialog by remember { mutableStateOf(false) }
    val pickCover = rememberCoverImagePickerLauncher(onPicked = viewModel::updateCoverImage)

    ArchiveScreenContent(
        state = state,
        selectedTab = selectedTab,
        archiveName = archiveName,
        archiveImageUrl = archiveImageUrl,
        coverImageBytes = coverImageBytes,
        toast = toast,
        mySpotState = mySpotState,
        onTabChange = viewModel::tabChanged,
        onKakaoLogin = onRequireLogin,
        onAppleLogin = onRequireLogin,
        onExploreClick = onExploreClick,
        onRegisterClick = onOpenRegistration,
        onCellClick = { id -> onOpenSpotDetail(id.toString()) },
        onBookmarkTap = viewModel::bookmarkTapped,
        onCellAppear = viewModel::loadNextPageIfNeeded,
        onMyCellAppear = viewModel::loadNextMySpotPageIfNeeded,
        onRenameClick = { showRenameDialog = true },
        onCoverImageClick = pickCover,
    )

    if (showRenameDialog) {
        ArchiveRenameDialog(
            initialName = archiveName,
            onDismiss = { showRenameDialog = false },
            onSave = viewModel::renameArchive,
        )
    }
}

/**
 * Stateless 본체 — Paparazzi / Compose UI 테스트에서 직접 호출.
 *
 * iOS 의 picture-stick parallax 는 Android 컨벤션상 일반 스크롤 + TopAppBar
 * 타이틀 fade-in 으로 단순화한다. 중첩 스크롤 제약상 헤더/탭바/그리드는
 * 단일 `LazyVerticalStaggeredGrid` 안에서 FullLine span 으로 구성한다.
 */
@Composable
fun ArchiveScreenContent(
    state: ArchiveLoadState,
    selectedTab: ArchiveTab,
    archiveName: String,
    archiveImageUrl: String? = null,
    coverImageBytes: ByteArray? = null,
    toast: String? = null,
    mySpotState: LoadState<List<MySpot>> = LoadState.Idle,
    onTabChange: (ArchiveTab) -> Unit = {},
    onKakaoLogin: () -> Unit = {},
    onAppleLogin: () -> Unit = {},
    onExploreClick: () -> Unit = {},
    onRegisterClick: () -> Unit = {},
    onCellClick: (Long) -> Unit = {},
    onBookmarkTap: (Long) -> Unit = {},
    onCellAppear: (SavedSpot) -> Unit = {},
    onMyCellAppear: (MySpot) -> Unit = {},
    onRenameClick: () -> Unit = {},
    onCoverImageClick: () -> Unit = {},
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PickflowColors.gray95)
            .testTag("archive-screen"),
    ) {
        when (state) {
            is ArchiveLoadState.SignedOut -> ArchiveSignedOutContent(
                onKakaoLogin = onKakaoLogin,
                onAppleLogin = onAppleLogin,
            )
            else -> ArchiveScrollableContent(
                state = state,
                selectedTab = selectedTab,
                archiveName = archiveName,
                archiveImageUrl = archiveImageUrl,
                coverImageBytes = coverImageBytes,
                mySpotState = mySpotState,
                onTabChange = onTabChange,
                onExploreClick = onExploreClick,
                onRegisterClick = onRegisterClick,
                onCellClick = onCellClick,
                onBookmarkTap = onBookmarkTap,
                onCellAppear = onCellAppear,
                onMyCellAppear = onMyCellAppear,
                onRenameClick = onRenameClick,
                onCoverImageClick = onCoverImageClick,
            )
        }

        toast?.let { ToastOverlay(it) }
    }
}

@Composable
private fun ArchiveScrollableContent(
    state: ArchiveLoadState,
    selectedTab: ArchiveTab,
    archiveName: String,
    archiveImageUrl: String?,
    coverImageBytes: ByteArray?,
    mySpotState: LoadState<List<MySpot>>,
    onTabChange: (ArchiveTab) -> Unit,
    onExploreClick: () -> Unit,
    onRegisterClick: () -> Unit,
    onCellClick: (Long) -> Unit,
    onBookmarkTap: (Long) -> Unit,
    onCellAppear: (SavedSpot) -> Unit,
    onMyCellAppear: (MySpot) -> Unit,
    onRenameClick: () -> Unit,
    onCoverImageClick: () -> Unit,
) {
    val gridState = rememberLazyStaggeredGridState()
    val navTitleVisible by remember {
        derivedStateOf {
            gridState.firstVisibleItemIndex > 0 || gridState.firstVisibleItemScrollOffset > 400
        }
    }
    // 커버(헤더, index 0)를 지나치면 2탭바를 상단(타이틀 아래)에 고정.
    val tabBarPinned by remember {
        derivedStateOf { gridState.firstVisibleItemIndex >= 1 }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        ArchiveTopBar(
            title = archiveName,
            titleVisible = navTitleVisible,
            onRenameClick = onRenameClick,
            onCoverImageClick = onCoverImageClick,
        )

        // 스티키 탭바 — 커버가 스크롤로 사라지면 상단에 고정 노출.
        if (tabBarPinned) {
            ArchiveTabBar(selectedTab = selectedTab, onTabChange = onTabChange)
        }

        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Fixed(2),
            state = gridState,
            contentPadding = PaddingValues(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalItemSpacing = 12.dp,
            modifier = Modifier
                .fillMaxSize()
                .testTag("archive-scroll"),
        ) {
            item(key = "header", span = StaggeredGridItemSpan.FullLine) {
                ArchiveHeaderSection(
                    archiveName = archiveName,
                    archiveImageUrl = archiveImageUrl,
                    coverImageBytes = coverImageBytes,
                )
            }
            item(key = "tabbar", span = StaggeredGridItemSpan.FullLine) {
                ArchiveTabBar(selectedTab = selectedTab, onTabChange = onTabChange)
            }

            when (selectedTab) {
                ArchiveTab.SavedSpots -> savedSpotsItems(
                    state = state,
                    onCellClick = onCellClick,
                    onBookmarkTap = onBookmarkTap,
                    onCellAppear = onCellAppear,
                    onExploreClick = onExploreClick,
                )
                ArchiveTab.MySpots -> mySpotsItems(
                    state = mySpotState,
                    onCellClick = onCellClick,
                    onCellAppear = onMyCellAppear,
                    onRegisterClick = onRegisterClick,
                )
            }
        }
    }
}

/**
 * "나만의 스팟" 탭의 그리드 아이템들. 빈 상태(Empty)에서만 등록 CTA placeholder 표시.
 * 셀에는 상태 배지(PENDING/REJECTED) overlay. PUBLISHED 는 배지 없음. 셀 탭 → 기존 SpotDetail.
 */
private fun androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridScope.mySpotsItems(
    state: LoadState<List<MySpot>>,
    onCellClick: (Long) -> Unit,
    onCellAppear: (MySpot) -> Unit,
    onRegisterClick: () -> Unit,
) {
    when (state) {
        is LoadState.Idle,
        is LoadState.Loading -> item(key = "my-loading", span = StaggeredGridItemSpan.FullLine) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .testTag("archive-my-loading"),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = PickflowColors.sunsetOrange)
            }
        }
        is LoadState.Empty -> item(key = "my-empty", span = StaggeredGridItemSpan.FullLine) {
            Box(modifier = Modifier.fillMaxWidth().height(360.dp)) {
                ArchiveMySpotPlaceholderContent(onRegisterClick = onRegisterClick)
            }
        }
        is LoadState.Failed -> item(key = "my-failed", span = StaggeredGridItemSpan.FullLine) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .padding(24.dp)
                    .testTag("archive-my-failed"),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "문제가 발생했어요.\n${state.error.message ?: ""}",
                    style = PickflowTypography.bodyMedium,
                    color = PickflowColors.gray40,
                )
            }
        }
        is LoadState.Loaded -> {
            item(key = "my-grid-padding-top", span = StaggeredGridItemSpan.FullLine) {
                Spacer(Modifier.height(16.dp))
            }
            items(state.value, key = { it.id }) { my ->
                LaunchedEffect(my.id) { onCellAppear(my) }
                Box(
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onCellClick(my.id) }
                        .testTag("archive-my-cell-${my.id}"),
                ) {
                    SpotListCell(
                        item = SpotListGridItem(
                            spotId = my.id,
                            name = my.name,
                            mood = my.theme.toMood(),
                            hasThumbnail = !my.imageUrl.isNullOrBlank(),
                            distanceKm = my.distanceKm,
                            imageUrl = my.imageUrl,
                        ),
                    )
                    MySpotStatusBadge(
                        status = my.status,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun MySpotStatusBadge(status: MySpotStatus, modifier: Modifier = Modifier) {
    val (label, bg) = when (status) {
        MySpotStatus.PENDING -> "검토중" to PickflowColors.gray80
        MySpotStatus.REJECTED -> "반려됨" to PickflowColors.sunsetOrange
        MySpotStatus.PUBLISHED -> return // 배지 없음
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .testTag("archive-my-badge-${status.name.lowercase()}"),
    ) {
        Text(
            text = label,
            style = PickflowTypography.labelSmall,
            color = PickflowColors.gray0,
        )
    }
}

private fun androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridScope.savedSpotsItems(
    state: ArchiveLoadState,
    onCellClick: (Long) -> Unit,
    onBookmarkTap: (Long) -> Unit,
    onCellAppear: (SavedSpot) -> Unit,
    onExploreClick: () -> Unit,
) {
    when (state) {
        is ArchiveLoadState.Loading -> item(
            key = "loading",
            span = StaggeredGridItemSpan.FullLine,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .testTag("archive-loading"),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = PickflowColors.sunsetOrange)
            }
        }
        is ArchiveLoadState.Empty -> item(
            key = "empty",
            span = StaggeredGridItemSpan.FullLine,
        ) {
            Box(modifier = Modifier.fillMaxWidth().height(360.dp)) {
                ArchiveEmptyContent(onExploreClick = onExploreClick)
            }
        }
        is ArchiveLoadState.Failed -> item(
            key = "failed",
            span = StaggeredGridItemSpan.FullLine,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .padding(24.dp)
                    .testTag("archive-failed"),
                contentAlignment = Alignment.Center,
            ) {
                androidx.compose.material3.Text(
                    text = "문제가 발생했어요.\n${state.message}",
                    style = PickflowTypography.bodyMedium,
                    color = PickflowColors.gray40,
                )
            }
        }
        is ArchiveLoadState.Loaded -> {
            item(key = "grid-padding-top", span = StaggeredGridItemSpan.FullLine) {
                Spacer(Modifier.height(16.dp))
            }
            items(state.items, key = { it.id }) { saved ->
                LaunchedEffect(saved.id) { onCellAppear(saved) }
                Box(
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onCellClick(saved.id) }
                        .testTag("archive-cell-${saved.id}"),
                ) {
                    SpotListCell(
                        item = SpotListGridItem(
                            spotId = saved.id,
                            name = saved.name,
                            mood = saved.theme.toMood(),
                            hasThumbnail = !saved.imageUrl.isNullOrBlank(),
                            distanceKm = saved.distanceKm,
                            imageUrl = saved.imageUrl,
                            isBookmarked = true,
                        ),
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(32.dp)
                            .clickable { onBookmarkTap(saved.id) }
                            .testTag("archive-bookmark-${saved.id}"),
                    )
                }
            }
        }
        ArchiveLoadState.SignedOut -> Unit
    }
}

@Composable
private fun ArchiveHeaderSection(
    archiveName: String,
    archiveImageUrl: String?,
    coverImageBytes: ByteArray?,
) {
    Box(modifier = Modifier.fillMaxWidth().height(HeaderHeight)) {
        ArchiveHeader(
            thumbnailUrl = archiveImageUrl,
            coverImageBytes = coverImageBytes,
            height = HeaderHeight,
        )
        androidx.compose.material3.Text(
            text = archiveName,
            style = PickflowTypography.headingLarge,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .testTag("archive-large-title"),
        )
    }
}

@Composable
private fun ArchiveTopBar(
    title: String,
    titleVisible: Boolean,
    onRenameClick: () -> Unit,
    onCoverImageClick: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .testTag("archive-topbar"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(Modifier.size(20.dp))
        AnimatedVisibility(
            visible = titleVisible,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            androidx.compose.material3.Text(
                text = title,
                style = PickflowTypography.bodyLargeBold,
                color = Color.White,
            )
        }
        Spacer(Modifier.weight(1f))
        Box {
            IconButton(
                onClick = { menuOpen = true },
                modifier = Modifier.testTag("archive-menu-button"),
            ) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = "메뉴",
                    tint = Color.White,
                )
            }
            DropdownMenu(
                expanded = menuOpen,
                onDismissRequest = { menuOpen = false },
                modifier = Modifier.background(PickflowColors.gray90),
            ) {
                DropdownMenuItem(
                    text = { Text("보관함 이름 변경", color = PickflowColors.gray0) },
                    onClick = { menuOpen = false; onRenameClick() },
                    modifier = Modifier.testTag("archive-menu-rename"),
                )
                DropdownMenuItem(
                    text = { Text("커버 이미지 변경", color = PickflowColors.gray0) },
                    onClick = { menuOpen = false; onCoverImageClick() },
                    modifier = Modifier.testTag("archive-menu-cover"),
                )
            }
        }
    }
}

@Composable
private fun ToastOverlay(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 80.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(PickflowColors.gray0)
                .padding(horizontal = 20.dp, vertical = 14.dp)
                .testTag("archive-toast"),
        ) {
            Text(
                text = message,
                style = PickflowTypography.bodyMediumBold,
                color = PickflowColors.gray95,
            )
        }
    }
}

private fun SpotTheme.toMood(): SpotListMood = when (this) {
    SpotTheme.SUNSET -> SpotListMood.Sunset
    SpotTheme.YUNSEUL -> SpotListMood.Reflection
}
