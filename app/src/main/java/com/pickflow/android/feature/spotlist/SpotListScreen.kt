package com.pickflow.android.feature.spotlist

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.pickflow.android.R
import com.pickflow.android.common.designsystem.PickflowColors
import com.pickflow.android.common.designsystem.PickflowTypography
import com.pickflow.android.common.ui.LoadStateContent
import com.pickflow.android.core.services.protocols.Region
import com.pickflow.android.core.services.protocols.Spot
import com.pickflow.android.core.services.protocols.SpotSort
import com.pickflow.android.core.services.protocols.SpotTheme
import com.pickflow.android.feature.map.MoodFilter
import com.pickflow.android.feature.map.NewFeatureBadgeViewModel
import com.pickflow.android.feature.map.components.MoodFilterRow
import com.pickflow.android.feature.map.components.RegionHeader
import com.pickflow.android.feature.map.components.RegionPickerSheet
import com.pickflow.android.feature.map.toMood
import com.pickflow.android.feature.map.toTheme

@Composable
fun SpotListScreen(
    onOpenSpotDetail: (String) -> Unit,
    onRequireLogin: () -> Unit,
    viewModel: SpotListViewModel = hiltViewModel(),
    newFeatureBadgeViewModel: NewFeatureBadgeViewModel = hiltViewModel(),
) {
    val spots by viewModel.spots.collectAsStateWithLifecycle()
    val themes by viewModel.themes.collectAsStateWithLifecycle()
    val sort by viewModel.sort.collectAsStateWithLifecycle()
    val bookmarkedIds by viewModel.bookmarkedIds.collectAsStateWithLifecycle()
    val showLoginPrompt by viewModel.showLoginPrompt.collectAsStateWithLifecycle()
    val toast by viewModel.toast.collectAsStateWithLifecycle()
    val region by viewModel.region.collectAsStateWithLifecycle()
    val regions by viewModel.regions.collectAsStateWithLifecycle()
    val showNewBadge by newFeatureBadgeViewModel.newBadgeVisible.collectAsStateWithLifecycle()

    var showRegionPicker by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.refresh() }

    val toastContext = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(toast) {
        toast?.let {
            android.widget.Toast.makeText(toastContext, it, android.widget.Toast.LENGTH_SHORT).show()
            viewModel.consumeToast()
        }
    }

    // 가까운 순(DISTANCE)은 위치 권한이 필요. 권한 없이 선택 시 설정 이동 팝업 표시.
    var showLocationDeniedPopup by remember { mutableStateOf(false) }
    val onSelectSort: (SpotSort) -> Unit = { option ->
        if (option == SpotSort.DISTANCE && !toastContext.hasLocationPermission()) {
            showLocationDeniedPopup = true
        } else {
            viewModel.selectSort(option)
        }
    }

    // 헤더 3층은 그리드 위에 겹쳐 그린다(collapsing). 각 층 높이는 폰트 배율·기기 폭에 따라
    // 달라지므로 하드코딩하지 않고 실제 측정값(px)을 쓴다.
    var regionHeightPx by remember { mutableStateOf(0) }
    var moodHeightPx by remember { mutableStateOf(0) }
    var sortHeightPx by remember { mutableStateOf(0) }
    val headerHeightPx = regionHeightPx + moodHeightPx + sortHeightPx
    val density = LocalDensity.current
    val gridState = rememberLazyStaggeredGridState()

    // 접히는 건 무드 행 위(로고)와 아래(정렬) 두 층뿐 — 무드 행은 끝까지 상단에 남는다.
    // 스크롤량은 firstVisibleItemScrollOffset 으로 읽는다. contentPadding 을 포함해 세는 값이라
    // 헤더가 측정되며 패딩이 커져도 0 에서 시작한다(offset.y 로 계산하면 첫 프레임에
    // 접힌 상태로 시작해버린다). 첫 행이 화면을 벗어나면 이미 다 접힌 것이므로 상한 고정.
    val collapseRangePx = regionHeightPx + sortHeightPx
    val measuredCollapsePx by remember(collapseRangePx) {
        derivedStateOf {
            when {
                // 재조회 중(LoadState.Loading)에는 그리드가 컴포지션에서 빠져 스크롤을 읽을 수
                // 없다. 이때 0 으로 떨어뜨리면 헤더가 펼쳐졌다 접히며 튄다.
                gridState.layoutInfo.totalItemsCount == 0 -> null
                gridState.firstVisibleItemIndex > 1 -> collapseRangePx
                else -> gridState.firstVisibleItemScrollOffset.coerceAtMost(collapseRangePx)
            }
        }
    }
    // 마지막으로 확인된 접힘량 — 그리드가 잠시 사라진 동안 헤더 위치를 붙잡아 둔다.
    var keptCollapsePx by remember { mutableStateOf(0) }
    LaunchedEffect(measuredCollapsePx) {
        measuredCollapsePx?.let { keptCollapsePx = it }
    }
    val collapsedPx = measuredCollapsePx ?: keptCollapsePx

    // 무드/정렬/지역을 바꾸면 목록이 새로 로드되면서 스크롤이 0 으로 돌아간다. 목록 자체는
    // 처음부터 보여주되, 접혀 있던 헤더까지 같이 펼쳐지지는 않게 접힘 지점으로 되돌린다.
    // 다음 페이지 append 때는 첫 아이템 오프셋이 이미 접힘량 이상이라 발동하지 않는다.
    LaunchedEffect(spots) {
        if (spots is com.pickflow.android.common.ui.LoadState.Loaded &&
            keptCollapsePx > 0 &&
            gridState.firstVisibleItemIndex == 0 &&
            gridState.firstVisibleItemScrollOffset < keptCollapsePx
        ) {
            gridState.scrollToItem(0, keptCollapsePx)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PickflowColors.gray95)
            .testTag("spotlist-screen"),
    ) {
        LoadStateContent(
            state = spots,
            emptyMessage = "아직 저장한 스팟이 없어요.",
            onRetry = viewModel::refresh,
        ) { list ->
            SpotMasonryGrid(
                spots = list,
                bookmarkedIds = bookmarkedIds,
                onClick = onOpenSpotDetail,
                onBookmark = viewModel::toggleBookmark,
                onReachEnd = viewModel::loadNextPage,
                gridState = gridState,
                topPadding = with(density) { headerHeightPx.toDp() },
            )
        }

        // 로고 행 — 스크롤과 함께 위로 사라진다.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(0, -collapsedPx) }
                .background(PickflowColors.gray95)
                .onSizeChanged { regionHeightPx = it.height },
        ) {
            SpotListRegionBar(region = region, onRegionClick = { showRegionPicker = true })
        }

        // 정렬 바 — 무드 행 아래에 놓이고, 스크롤하면 무드 행 뒤로 밀려 들어간다.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(0, regionHeightPx + moodHeightPx - collapsedPx) }
                .background(PickflowColors.gray95)
                .onSizeChanged { sortHeightPx = it.height },
        ) {
            SpotListSortSelector(sort = sort, onSelectSort = onSelectSort)
        }

        // 무드 행 — 유일한 sticky 요소. 정렬 바를 가려야 하므로 마지막에(=위에) 그린다.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(0, (regionHeightPx - collapsedPx).coerceAtLeast(0)) }
                .background(PickflowColors.gray95)
                .onSizeChanged { moodHeightPx = it.height },
        ) {
            MoodFilterRow(
                selected = themes.mapTo(mutableSetOf()) { it.toMood() },
                onSelect = { mood -> viewModel.toggleTheme(mood.toTheme()) },
                testTag = "spotlist-mood",
                showNewBadge = showNewBadge,
            )
        }
    }

        if (showRegionPicker) {
            RegionPickerSheet(
                applied = region,
                regions = regions,
                onApply = {
                    viewModel.applyRegion(it)
                    showRegionPicker = false
                },
                // 취소·바깥 탭·드래그 dismiss — 변경 사항을 버린다.
                onDismiss = { showRegionPicker = false },
            )
        }

        if (showLoginPrompt) {
            // iOS `SpotListView` overlay + `LoginPromptPopup` 1:1.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.5f))
                    .clickable { viewModel.dismissLoginPrompt() }
                    .testTag("spotlist-login-overlay"),
            ) {
                com.pickflow.android.feature.spotdetail.components.LoginPromptPopup(
                    onCancel = viewModel::dismissLoginPrompt,
                    onLogin = {
                        viewModel.dismissLoginPrompt()
                        onRequireLogin()
                    },
                    isClosable = true,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 32.dp)
                        .clickable(enabled = false) {},
                )
            }
        }

        if (showLocationDeniedPopup) {
            // iOS `LocationPermissionDeniedPopup` 1:1 — 가까운 순 선택 시 권한 없으면 설정 이동 유도.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.5f))
                    .clickable { showLocationDeniedPopup = false }
                    .testTag("spotlist-location-denied-overlay"),
            ) {
                com.pickflow.android.feature.map.components.LocationPermissionDeniedPopup(
                    onCancel = { showLocationDeniedPopup = false },
                    onOpenSettings = {
                        showLocationDeniedPopup = false
                        val intent = android.content.Intent(
                            android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            android.net.Uri.fromParts("package", toastContext.packageName, null),
                        ).apply { addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK) }
                        toastContext.startActivity(intent)
                    },
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 32.dp)
                        .clickable(enabled = false) {},
                )
            }
        }
    }
}

/** 위치 권한(정밀/대략 중 하나라도) 보유 여부. */
private fun android.content.Context.hasLocationPermission(): Boolean =
    androidx.core.content.ContextCompat.checkSelfPermission(
        this, android.Manifest.permission.ACCESS_FINE_LOCATION,
    ) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
        androidx.core.content.ContextCompat.checkSelfPermission(
            this, android.Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

/** 로고 + 지역 선택 행 — 지도 모드 헤더와 같은 높이/타이포를 쓴다. */
@Composable
private fun SpotListRegionBar(
    region: Region,
    onRegionClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        RegionHeader(
            region = region,
            onClick = onRegionClick,
            modifier = Modifier.align(Alignment.CenterStart),
            testTag = "spotlist-region",
        )
    }
}

/**
 * 정렬 드롭다운 — 무드 필터 **아래**, 그리드 바로 위 우측에 놓인다(PV-86).
 *
 * 예전에는 로고 행 우측에 있었으나, 스크롤 시 무드 행만 상단에 고정되고 정렬은 목록과 함께
 * 밀려 올라가야 해서 두 요소를 분리했다.
 */
@Composable
private fun SpotListSortSelector(
    sort: SpotSort,
    onSelectSort: (SpotSort) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    // 드롭다운을 정렬 바 '아래'에 띄우려면 바 높이를 알아야 한다(Popup 은 부모 bounds 기준).
    var barHeightPx by remember { mutableStateOf(0) }
    val density = LocalDensity.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .onSizeChanged { barHeightPx = it.height },
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .clip(RoundedCornerShape(8.dp))
                .clickable { expanded = !expanded }
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .testTag("spotlist-sort-toggle"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = sort.displayName(),
                style = PickflowTypography.bodyLargeBold,
                color = PickflowColors.gray0,
            )
            Icon(
                imageVector = if (expanded) Icons.Filled.KeyboardArrowUp
                else Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                tint = PickflowColors.gray0,
                modifier = Modifier.size(18.dp),
            )
        }
        if (expanded) {
            // Popup 으로 띄워 정렬 바 높이가 그리드 위치에 영향을 주지 않게 한다.
            Popup(
                alignment = Alignment.TopEnd,
                offset = IntOffset(0, 0),
                onDismissRequest = { expanded = false },
                properties = PopupProperties(focusable = true),
            ) {
                Column(
                    modifier = Modifier
                        .padding(top = with(density) { barHeightPx.toDp() }, end = 16.dp)
                        .width(180.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(PickflowColors.gray90)
                        .testTag("spotlist-sort-options"),
                ) {
                    SORT_OPTIONS.forEachIndexed { index, option ->
                        if (index > 0) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(PickflowColors.gray80),
                            )
                        }
                        SortRow(
                            option = option,
                            selected = option == sort,
                            onClick = {
                                onSelectSort(option)
                                expanded = false
                            },
                        )
                    }
                }
            }
        }
    }
}

/** 그리드 콘텐츠 위/아래 여백 — 겹쳐 그리는 헤더 높이에 이 값이 더해진다. */
private val GRID_VERTICAL_PADDING = 8.dp

// iOS 매핑: "추천 순" 의 서버 코드는 RECOMMENDED. 서버 정렬 기준은 like_count 다.
private val SORT_OPTIONS = listOf(SpotSort.DISTANCE, SpotSort.RECOMMENDED)

@Composable
private fun SortRow(option: SpotSort, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(
                if (selected) PickflowColors.sunsetOrange.copy(alpha = 0.2f)
                else PickflowColors.gray90,
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = option.displayName(),
            style = PickflowTypography.bodyLarge,
            color = if (selected) PickflowColors.sunsetOrange else PickflowColors.gray0,
            modifier = Modifier.weight(1f),
        )
        if (selected) {
            Text(
                text = "✓",
                style = PickflowTypography.bodyLargeBold,
                color = PickflowColors.sunsetOrange,
            )
        }
    }
}

@Composable
private fun SpotMasonryGrid(
    spots: List<Spot>,
    bookmarkedIds: Set<String>,
    onClick: (String) -> Unit,
    onBookmark: (String) -> Unit,
    onReachEnd: () -> Unit,
    gridState: LazyStaggeredGridState,
    topPadding: androidx.compose.ui.unit.Dp,
) {
    // 키 없는 remember 는 첫 컴포지션의 `spots` 를 그대로 붙잡는다. 그래서 목록이 늘어나도
    // spots.size 는 1페이지 크기에 멈춰 있고, 한 번 true 가 된 reachedEnd 가 계속 true 라
    // LaunchedEffect 가 다시 뜨지 않아 2페이지 이후로 로드가 끊겼다(= 무한 스크롤 없음).
    // layoutInfo.totalItemsCount 는 스냅샷 상태라 캡처 없이 매번 최신값을 읽는다.
    val reachedEnd by remember {
        derivedStateOf {
            val info = gridState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: -1
            info.totalItemsCount > 0 && last >= info.totalItemsCount - 3
        }
    }
    LaunchedEffect(reachedEnd) { if (reachedEnd) onReachEnd() }

    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(2),
        state = gridState,
        // 상단 패딩만큼 헤더가 겹쳐 있다 — 스크롤하면 콘텐츠가 그 아래로 지나간다.
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = topPadding + GRID_VERTICAL_PADDING,
            bottom = GRID_VERTICAL_PADDING,
        ),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalItemSpacing = 12.dp,
        modifier = Modifier
            .fillMaxSize()
            .testTag("spotlist-grid"),
    ) {
        items(spots, key = { it.id }) { spot ->
            SpotListCell(
                spot = spot,
                bookmarked = spot.id in bookmarkedIds,
                onClick = { onClick(spot.id) },
                onBookmark = { onBookmark(spot.id) },
            )
        }
    }
}

/** iOS `SpotListCell` 1:1 — 썸네일 + 오버레이 배지(무드/거리) + 메타 행. */
@Composable
private fun SpotListCell(
    spot: Spot,
    bookmarked: Boolean,
    onClick: () -> Unit,
    onBookmark: () -> Unit,
) {
    // 짝/홀 spotId 로 종횡비 분기 → masonry 효과.
    val idHash = spot.id.hashCode()
    val aspect = if (idHash % 2 == 0) 1.2f else 0.9f

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f / aspect)
                .clip(RoundedCornerShape(12.dp))
                .background(PickflowColors.gray90),
        ) {
            if (!spot.imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = spot.imageUrl,
                    contentDescription = spot.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MoodBadge(theme = spot.theme)
                spot.distanceKm?.let { DistanceBadge(it) }
            }
        }
        MetaRow(spot = spot, bookmarked = bookmarked, onBookmark = onBookmark)
    }
}

@Composable
private fun MoodBadge(theme: SpotTheme) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(PickflowColors.gray95)
            .padding(4.dp),
    ) {
        Image(
            painter = painterResource(theme.iconRes()),
            contentDescription = null,
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
private fun DistanceBadge(km: Double) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(PickflowColors.gray95)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Text(
            text = "%.1fkm".format(km),
            style = PickflowTypography.labelMedium,
            color = PickflowColors.gray10,
        )
    }
}

@Composable
private fun MetaRow(
    spot: Spot,
    bookmarked: Boolean,
    onBookmark: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = spot.name,
                style = PickflowTypography.bodyMediumBold,
                color = PickflowColors.gray0,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = spot.theme.label(),
                    style = PickflowTypography.labelSmall,
                    color = PickflowColors.gray10,
                )
                Text(
                    text = "·",
                    style = PickflowTypography.labelSmall,
                    color = PickflowColors.gray50,
                )
                Text(
                    text = "추천 ${spot.likeCount}",
                    style = PickflowTypography.labelSmall,
                    color = PickflowColors.gray10,
                )
            }
        }
        IconButton(onClick = onBookmark) {
            Image(
                painter = painterResource(
                    if (bookmarked) R.drawable.ic_bookmark_selected
                    else R.drawable.ic_bookmark,
                ),
                contentDescription = if (bookmarked) "북마크 해제" else "북마크 추가",
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

// 아이콘·라벨은 MoodFilter 를 단일 출처로 삼는다. 무드가 추가돼도 여기는 손댈 곳이 없다.
private fun SpotTheme.iconRes(): Int = toMood().iconRes

fun SpotTheme.label(): String = toMood().displayName

fun SpotSort.displayName(): String = when (this) {
    SpotSort.DISTANCE -> "가까운 순"
    SpotSort.RECOMMENDED -> "추천 순"
}
