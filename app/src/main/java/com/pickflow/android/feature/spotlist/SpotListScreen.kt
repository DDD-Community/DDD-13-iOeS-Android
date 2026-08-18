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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
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
import com.pickflow.android.core.services.protocols.Spot
import com.pickflow.android.core.services.protocols.SpotSort
import com.pickflow.android.core.services.protocols.SpotTheme
import com.pickflow.android.feature.map.MoodFilter

@Composable
fun SpotListScreen(
    onOpenSpotDetail: (String) -> Unit,
    onRequireLogin: () -> Unit,
    viewModel: SpotListViewModel = hiltViewModel(),
) {
    val spots by viewModel.spots.collectAsStateWithLifecycle()
    val theme by viewModel.theme.collectAsStateWithLifecycle()
    val sort by viewModel.sort.collectAsStateWithLifecycle()
    val bookmarkedIds by viewModel.bookmarkedIds.collectAsStateWithLifecycle()
    val showLoginPrompt by viewModel.showLoginPrompt.collectAsStateWithLifecycle()
    val toast by viewModel.toast.collectAsStateWithLifecycle()

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

    Box(modifier = Modifier.fillMaxSize()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PickflowColors.gray95)
            .testTag("spotlist-screen"),
    ) {
        SpotListHeader(
            sort = sort,
            onSelectSort = onSelectSort,
        )
        MoodFilterRow(
            selected = theme.toMood(),
            onSelect = { mood ->
                val current = theme.toMood()
                viewModel.selectTheme(if (current == mood) null else mood.toTheme())
            },
        )
        Spacer(Modifier.height(8.dp))
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
            )
        }
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

/** iOS `HomeMapView.headerBar` + `SpotListSortDropdownHeader` 1:1 — 로고 + 정렬 드롭다운. */
@Composable
private fun SpotListHeader(sort: SpotSort, onSelectSort: (SpotSort) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Image(
            painter = painterResource(R.drawable.logo),
            contentDescription = "PICKFLOW",
            modifier = Modifier
                .align(Alignment.CenterStart)
                .height(24.dp),
        )
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
            // Popup 으로 띄워 헤더 레이아웃에 영향을 주지 않도록 한다.
            Popup(
                alignment = Alignment.TopEnd,
                offset = IntOffset(0, 0),
                onDismissRequest = { expanded = false },
                properties = PopupProperties(focusable = true),
            ) {
                Column(
                    modifier = Modifier
                        .padding(top = 44.dp, end = 20.dp)
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

/** iOS `HomeMapView.MoodFilterRow` 와 동일한 무드 캡슐 — 아이콘 + 라벨. */
@Composable
private fun MoodFilterRow(selected: MoodFilter?, onSelect: (MoodFilter) -> Unit) {
    Row(
        modifier = Modifier
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .testTag("spotlist-mood"),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        MoodFilter.entries.forEach { mood ->
            MoodCapsule(mood = mood, selected = selected == mood) { onSelect(mood) }
        }
    }
}

@Composable
private fun MoodCapsule(mood: MoodFilter, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .then(
                if (selected) {
                    Modifier.border(1.dp, PickflowColors.sunsetOrange, RoundedCornerShape(8.dp))
                } else Modifier,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Image(
            painter = painterResource(mood.iconRes),
            contentDescription = mood.displayName,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = mood.displayName,
            style = PickflowTypography.bodyLargeBold,
            color = if (selected) PickflowColors.gray0 else PickflowColors.gray10,
        )
    }
}

@Composable
private fun SpotMasonryGrid(
    spots: List<Spot>,
    bookmarkedIds: Set<String>,
    onClick: (String) -> Unit,
    onBookmark: (String) -> Unit,
    onReachEnd: () -> Unit,
) {
    val gridState = rememberLazyStaggeredGridState()
    val reachedEnd by remember {
        derivedStateOf {
            val last = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            spots.isNotEmpty() && last >= spots.size - 3
        }
    }
    LaunchedEffect(reachedEnd) { if (reachedEnd) onReachEnd() }

    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(2),
        state = gridState,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
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

private fun SpotTheme.iconRes(): Int = when (this) {
    SpotTheme.SUNSET -> R.drawable.ic_sunset
    SpotTheme.YUNSEUL -> R.drawable.ic_reflection
}

fun SpotTheme.label(): String = when (this) {
    SpotTheme.SUNSET -> "노을"
    SpotTheme.YUNSEUL -> "윤슬"
}

private fun SpotTheme?.toMood(): MoodFilter? = when (this) {
    SpotTheme.SUNSET -> MoodFilter.Sunset
    SpotTheme.YUNSEUL -> MoodFilter.Reflection
    null -> null
}

private fun MoodFilter.toTheme(): SpotTheme = when (this) {
    MoodFilter.Sunset -> SpotTheme.SUNSET
    MoodFilter.Reflection -> SpotTheme.YUNSEUL
}

fun SpotSort.displayName(): String = when (this) {
    SpotSort.DISTANCE -> "가까운 순"
    SpotSort.RECOMMENDED -> "추천 순"
}
