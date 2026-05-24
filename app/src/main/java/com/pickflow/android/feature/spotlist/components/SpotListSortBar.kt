package com.pickflow.android.feature.spotlist.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.pickflow.android.common.designsystem.PickflowColors
import com.pickflow.android.common.designsystem.PickflowTypography

/**
 * iOS `SpotListSnapshotTests.sortBar` 1:1 — 정렬 드롭다운 바.
 *
 * 우측 정렬 헤더(텍스트 + chevron) + (펼침 시) 옵션 박스. 배경 gray95.
 */
@Composable
fun SpotListSortBar(
    sort: SpotListSortOption,
    expanded: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PickflowColors.gray95)
            .testTag("spotlist-sortbar"),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            SortDropdownHeader(sort = sort, expanded = expanded)
        }
        if (expanded) {
            SortDropdownOptions(
                current = sort,
                modifier = Modifier.padding(end = 16.dp),
            )
        }
    }
}

@Composable
private fun SortDropdownHeader(sort: SpotListSortOption, expanded: Boolean) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = sort.displayName,
            style = PickflowTypography.bodyLargeBold,
            color = PickflowColors.gray0,
        )
        Icon(
            imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
            contentDescription = null,
            tint = PickflowColors.gray0,
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
private fun SortDropdownOptions(current: SpotListSortOption, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .width(180.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(PickflowColors.gray95)
            .padding(vertical = 4.dp),
    ) {
        SpotListSortOption.entries.forEachIndexed { index, option ->
            if (index > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(PickflowColors.gray90),
                )
            }
            OptionRow(option = option, selected = option == current)
        }
    }
}

@Composable
private fun OptionRow(option: SpotListSortOption, selected: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (selected) PickflowColors.sunsetOrange.copy(alpha = 0.2f) else PickflowColors.gray95,
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = option.displayName,
            style = PickflowTypography.bodyLarge,
            color = if (selected) PickflowColors.sunsetOrange else PickflowColors.gray0,
        )
        Spacer(Modifier.weight(1f))
        if (selected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = PickflowColors.sunsetOrange,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}
