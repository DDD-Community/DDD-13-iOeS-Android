package com.pickflow.android.feature.spotlist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pickflow.android.common.designsystem.PickflowColors
import com.pickflow.android.common.designsystem.PickflowTypography
import com.pickflow.android.common.ui.LoadStateContent
import com.pickflow.android.core.services.protocols.Spot
import com.pickflow.android.core.services.protocols.SpotTheme

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

    LaunchedEffect(Unit) { viewModel.refresh() }

    if (showLoginPrompt) {
        LoginPromptDialog(
            onConfirm = {
                viewModel.dismissLoginPrompt()
                onRequireLogin()
            },
            onDismiss = viewModel::dismissLoginPrompt,
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PickflowColors.gray95)
            .testTag("spotlist-screen"),
    ) {
        Text(
            text = "저장한 스팟",
            style = PickflowTypography.headingLarge,
            color = PickflowColors.gray0,
            modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 12.dp),
        )
        ThemeFilterRow(selected = theme, onSelect = viewModel::selectTheme)
        SortRow(selected = sort, onSelect = viewModel::selectSort)
        LoadStateContent(
            state = spots,
            emptyMessage = "아직 저장한 스팟이 없어요.",
            onRetry = viewModel::refresh,
        ) { list ->
            SpotGrid(
                spots = list,
                bookmarkedIds = bookmarkedIds,
                onClick = onOpenSpotDetail,
                onBookmark = viewModel::toggleBookmark,
                onReachEnd = viewModel::loadNextPage,
            )
        }
    }
}

@Composable
private fun ThemeFilterRow(selected: SpotTheme?, onSelect: (SpotTheme?) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ThemeChip(label = "전체", active = selected == null) { onSelect(null) }
        SpotTheme.entries.forEach { t ->
            ThemeChip(label = t.label(), active = selected == t) { onSelect(t) }
        }
    }
}

@Composable
private fun SortRow(selected: SpotSort, onSelect: (SpotSort) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .testTag("spotlist-sort"),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SpotSort.entries.forEach { s ->
            ThemeChip(label = s.label, active = selected == s) { onSelect(s) }
        }
    }
}

@Composable
private fun LoginPromptDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("로그인이 필요해요") },
        text = { Text("스팟을 저장하려면 로그인이 필요해요.") },
        confirmButton = {
            TextButton(onClick = onConfirm, modifier = Modifier.testTag("spotlist-login-confirm")) {
                Text("로그인", color = PickflowColors.sunsetOrange)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } },
        containerColor = PickflowColors.gray90,
    )
}

@Composable
private fun ThemeChip(label: String, active: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = active,
        onClick = onClick,
        label = { Text(label, style = PickflowTypography.labelMedium) },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = PickflowColors.surfaceChip,
            labelColor = PickflowColors.gray30,
            selectedContainerColor = PickflowColors.sunsetOrange,
            selectedLabelColor = PickflowColors.gray0,
        ),
    )
}

@Composable
private fun SpotGrid(
    spots: List<Spot>,
    bookmarkedIds: Set<String>,
    onClick: (String) -> Unit,
    onBookmark: (String) -> Unit,
    onReachEnd: () -> Unit,
) {
    val gridState = rememberLazyGridState()
    val reachedEnd by remember {
        derivedStateOf {
            val last = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            last >= spots.size - 3
        }
    }
    LaunchedEffect(reachedEnd) { if (reachedEnd) onReachEnd() }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        state = gridState,
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.testTag("spotlist-grid"),
    ) {
        items(spots, key = { it.id }) { spot ->
            SpotCard(
                spot = spot,
                bookmarked = spot.id in bookmarkedIds,
                onClick = { onClick(spot.id) },
                onBookmark = { onBookmark(spot.id) },
            )
        }
    }
}

@Composable
private fun SpotCard(
    spot: Spot,
    bookmarked: Boolean,
    onClick: () -> Unit,
    onBookmark: () -> Unit,
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(PickflowColors.gray90)
            .clickable(onClick = onClick)
            .padding(12.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(PickflowColors.gray80),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(
                    text = spot.name,
                    style = PickflowTypography.bodyMediumBold,
                    color = PickflowColors.gray0,
                )
                Text(
                    text = spot.theme.label(),
                    style = PickflowTypography.labelSmall,
                    color = PickflowColors.gray40,
                )
            }
            IconButton(onClick = onBookmark) {
                Icon(
                    imageVector = if (bookmarked) Icons.Filled.Favorite
                    else Icons.Filled.FavoriteBorder,
                    contentDescription = "북마크",
                    tint = if (bookmarked) PickflowColors.sunsetOrange else PickflowColors.gray40,
                )
            }
        }
    }
}

fun SpotTheme.label(): String = when (this) {
    SpotTheme.SUNSET -> "노을"
    SpotTheme.YUNSEUL -> "윤슬"
}
