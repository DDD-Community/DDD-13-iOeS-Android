package com.pickflow.android.feature.spotlist.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.pickflow.android.common.designsystem.PickflowColors

/**
 * iOS `SpotListScreenContent`(loaded) 1:1 — ScrollView + `MasonryTwoColumn`.
 *
 * iOS Masonry는 인덱스 짝/홀로 두 컬럼에 나눠 배치한다. 짝수 인덱스 → 좌측 컬럼,
 * 홀수 인덱스 → 우측 컬럼. 컬럼 간격 12, 좌우 16 / 하단 24 패딩.
 */
@Composable
fun SpotListLoadedGrid(
    items: List<SpotListGridItem>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PickflowColors.gray95)
            .verticalScroll(rememberScrollState())
            .testTag("spotlist-loaded"),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MasonryColumn(
                items = items.filterIndexed { index, _ -> index % 2 == 0 },
                modifier = Modifier.weight(1f),
            )
            MasonryColumn(
                items = items.filterIndexed { index, _ -> index % 2 == 1 },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun MasonryColumn(items: List<SpotListGridItem>, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items.forEach { item ->
            SpotListCell(item = item)
        }
    }
}

/** iOS `SpotListLoadingView` 1:1 — 6개 스켈레톤 placeholder(2열 비대칭). */
@Composable
fun SpotListLoadingContent(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxSize()
            .background(PickflowColors.gray95)
            .padding(horizontal = 16.dp)
            .testTag("spotlist-loading"),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        SkeletonColumn(aspects = listOf(1.2f, 0.9f, 1.1f), modifier = Modifier.weight(1f))
        SkeletonColumn(aspects = listOf(0.9f, 1.2f, 0.9f), modifier = Modifier.weight(1f))
    }
}

@Composable
private fun SkeletonColumn(aspects: List<Float>, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        aspects.forEach { aspect ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f / aspect)
                    .clip(RoundedCornerShape(12.dp))
                    .background(PickflowColors.gray90),
            )
        }
    }
}
