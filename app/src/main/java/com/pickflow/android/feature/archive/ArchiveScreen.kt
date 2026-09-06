package com.pickflow.android.feature.archive

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pickflow.android.common.designsystem.PickflowColors
import com.pickflow.android.common.designsystem.PickflowTypography
import com.pickflow.android.common.ui.LoadState
import com.pickflow.android.core.services.protocols.MySpot
import com.pickflow.android.core.services.protocols.MySpotStatus
import com.pickflow.android.core.services.protocols.SavedSpot
import com.pickflow.android.core.services.protocols.SavedSpotAvailability
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
    /**
     * "나만의 스팟" 탭 전용 진입. 이 탭은 정의상 전부 내 스팟이라 목록 단계에서
     * 소유가 확정되므로 상세를 거치지 않고 바로 오픈 관리 화면으로 보낸다.
     * "저장된 스팟" 탭은 남의 스팟이 대부분이라 [onOpenSpotDetail] 을 쓴다
     * (`SavedSpotItem` 에는 `isMySpot` 이 없다 — 소유는 상세 응답에서만 알 수 있다).
     */
    onOpenMySpot: (Long) -> Unit,
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
        onMyCellClick = onOpenMySpot,
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
    onMyCellClick: (Long) -> Unit = {},
    onBookmarkTap: (Long) -> Unit = {},
    onCellAppear: (SavedSpot) -> Unit = {},
    onMyCellAppear: (MySpot) -> Unit = {},
    onRenameClick: () -> Unit = {},
    onCoverImageClick: () -> Unit = {},
) {
    var privateSpotToDelete by remember { mutableStateOf<Long?>(null) }
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
                onMyCellClick = onMyCellClick,
                onBookmarkTap = onBookmarkTap,
                onPrivateSpotClick = { privateSpotToDelete = it },
                onCellAppear = onCellAppear,
                onMyCellAppear = onMyCellAppear,
                onRenameClick = onRenameClick,
                onCoverImageClick = onCoverImageClick,
            )
        }

        toast?.let { ToastOverlay(it) }
    }

    privateSpotToDelete?.let { spotId ->
        ArchivePrivateDeleteDialog(
            onDismiss = { privateSpotToDelete = null },
            onConfirm = {
                privateSpotToDelete = null
                onBookmarkTap(spotId)
            },
        )
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
    onMyCellClick: (Long) -> Unit,
    onBookmarkTap: (Long) -> Unit,
    onPrivateSpotClick: (Long) -> Unit,
    onCellAppear: (SavedSpot) -> Unit,
    onMyCellAppear: (MySpot) -> Unit,
    onRenameClick: () -> Unit,
    onCoverImageClick: () -> Unit,
) {
    val gridState = rememberLazyStaggeredGridState()
    // 커버(헤더, index 0)를 지나치면 2탭바를 상단(타이틀 아래)에 고정.
    val tabBarPinned by remember {
        derivedStateOf { gridState.firstVisibleItemIndex >= 1 }
    }
    // 상단바 타이틀도 같은 시점에 띄운다. 예전엔 400px 만 넘으면 떠서, 커버 하단의
    // 라지 타이틀이 아직 화면에 있는 구간(400~커버높이)에 보관함 이름이 두 번 보였다.
    val navTitleVisible = tabBarPinned

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
            // 고정 노출 중에는 자리를 비운다. 상단 스티키 바와 이 아이템이 동시에 그려져
            // 탭바가 두 번 보이던 버그. 높이 0 이 되는 만큼 위에 붙은 스티키 바가 차지하므로
            // 스크롤 위치는 그대로 이어진다.
            item(key = "tabbar", span = StaggeredGridItemSpan.FullLine) {
                if (!tabBarPinned) {
                    ArchiveTabBar(selectedTab = selectedTab, onTabChange = onTabChange)
                }
            }

            when (selectedTab) {
                ArchiveTab.SavedSpots -> savedSpotsItems(
                    state = state,
                    onCellClick = onCellClick,
                    onBookmarkTap = onBookmarkTap,
                    onPrivateSpotClick = onPrivateSpotClick,
                    onCellAppear = onCellAppear,
                    onExploreClick = onExploreClick,
                )
                ArchiveTab.MySpots -> mySpotsItems(
                    state = mySpotState,
                    onCellClick = onMyCellClick,
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
                    val badge = my.badge()
                    SpotListCell(
                        item = SpotListGridItem(
                            spotId = my.id,
                            name = my.name,
                            mood = my.theme.toMood(),
                            hasThumbnail = !my.imageUrl.isNullOrBlank(),
                            distanceKm = my.distanceKm,
                            imageUrl = my.imageUrl,
                            // 내 스팟은 내가 북마크하는 대상이 아니라 아이콘을 달지 않는다.
                            isBookmarked = null,
                            // 공개·비공개만 추천 수를 노출한다(비공개는 공개였던 이력이 있는 스팟).
                            likeCount = my.likeCount?.takeIf { badge?.showsLikeCount == true },
                        ),
                        thumbnailOverlay = {
                            badge?.let {
                                MySpotStatusBadge(
                                    badge = it,
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(8.dp),
                                )
                            }
                        },
                    )
                }
            }
        }
    }
}

/**
 * 나만의 스팟 셀의 검수 상태 배지. 오픈 신청 전(DRAFT & 공개 이력 없음)은 배지가 없다.
 * 반려만 외곽선형이고 나머지는 채움형이다.
 */
private enum class MySpotBadge(
    val label: String,
    val tag: String,
    val outlined: Boolean,
    val showsLikeCount: Boolean,
) {
    IN_REVIEW("검수 중", "in-review", outlined = false, showsLikeCount = false),
    REJECTED("오픈 반려", "rejected", outlined = true, showsLikeCount = false),
    PUBLIC("공개", "public", outlined = false, showsLikeCount = true),
    PRIVATE("비공개", "private", outlined = false, showsLikeCount = true),
}

private fun MySpot.badge(): MySpotBadge? = when (status) {
    MySpotStatus.PENDING, MySpotStatus.RE_REVIEW_PENDING -> MySpotBadge.IN_REVIEW
    MySpotStatus.REJECTED -> MySpotBadge.REJECTED
    MySpotStatus.PUBLISHED -> MySpotBadge.PUBLIC
    // 해제 후 상태는 항상 DRAFT — 공개 이력이 있어야 "비공개"다.
    MySpotStatus.DRAFT -> MySpotBadge.PRIVATE.takeIf { wasPublished }
}

@Composable
private fun MySpotStatusBadge(badge: MySpotBadge, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(6.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(if (badge.outlined) PickflowColors.gray95 else PickflowColors.gray80)
            .then(
                if (badge.outlined) Modifier.border(1.dp, PickflowColors.gray0, shape) else Modifier,
            )
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Text(
            text = badge.label,
            style = PickflowTypography.labelMedium,
            color = if (badge.outlined) PickflowColors.gray0 else PickflowColors.gray20,
            modifier = Modifier.testTag("archive-my-badge-${badge.tag}"),
        )
    }
}

private fun androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridScope.savedSpotsItems(
    state: ArchiveLoadState,
    onCellClick: (Long) -> Unit,
    onBookmarkTap: (Long) -> Unit,
    onPrivateSpotClick: (Long) -> Unit,
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
                val isPrivate = saved.availability == SavedSpotAvailability.AUTHOR_PRIVATE
                Box(
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            if (isPrivate) onPrivateSpotClick(saved.id) else onCellClick(saved.id)
                        }
                        .testTag(
                            if (isPrivate) "archive-private-${saved.id}" else "archive-cell-${saved.id}",
                        ),
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
                        modifier = Modifier.alpha(if (isPrivate) 0.28f else 1f),
                    )
                    if (isPrivate) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .fillMaxWidth()
                                .aspectRatio(if (saved.id % 2L == 0L) 1f / 1.2f else 1f / 0.9f),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "등록한 유저가\n비공개로 전환하였어요",
                                style = PickflowTypography.bodySmallBold,
                                color = PickflowColors.gray20,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .semantics { contentDescription = "비공개로 전환됨" },
                            )
                        }
                    } else {
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
        }
        ArchiveLoadState.SignedOut -> Unit
    }
}

@Composable
private fun ArchivePrivateDeleteDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        ArchivePrivateDeleteDialogContent(onConfirm = onConfirm)
    }
}

@Composable
fun ArchivePrivateDeleteDialogContent(
    onConfirm: () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(PickflowColors.gray90)
            .padding(24.dp)
            .testTag("archive-private-modal"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "비공개로 전환된 스팟이에요",
            style = PickflowTypography.headingSmall,
            color = PickflowColors.gray0,
        )
        Text(
            text = "작성자가 스팟을 비공개로 전환했어요.\n목록에서 삭제할 수 있어요.",
            style = PickflowTypography.bodyMedium,
            color = PickflowColors.gray30,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(PickflowColors.sunsetOrange)
                .clickable(onClick = onConfirm)
                .testTag("archive-private-delete-confirm"),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "목록에서 삭제",
                style = PickflowTypography.bodyLargeBold,
                color = PickflowColors.gray0,
            )
        }
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
    SpotTheme.SUNLIGHT -> SpotListMood.Sunlight
    SpotTheme.YUNSEUL -> SpotListMood.Reflection
    SpotTheme.SUNSET -> SpotListMood.Sunset
    SpotTheme.NIGHT_VIEW -> SpotListMood.Night
}
