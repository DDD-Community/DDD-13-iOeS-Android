package com.pickflow.android.feature.spotlist.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pickflow.android.common.designsystem.PickflowColors
import com.pickflow.android.common.designsystem.PickflowTypography

/**
 * iOS `SpotListCell` 1:1 이식 — 리스트 셀(썸네일 + 메타).
 *
 * 썸네일은 사용자 사진 에셋 자리라 gray90 박스로 자리만 잡고,
 * mood overlay 커스텀 아이콘은 이모지(🌅/🌊), 북마크 아이콘은 Material
 * `Favorite`/`FavoriteBorder`로 치환한다(에셋 치환 규칙).
 */
@Composable
fun SpotListCell(
    item: SpotListGridItem,
    isBookmarked: Boolean,
    bookmarkCount: Int?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ThumbnailBox(item = item)
        MetaRow(item = item, isBookmarked = isBookmarked, bookmarkCount = bookmarkCount)
    }
}

@Composable
private fun ThumbnailBox(item: SpotListGridItem) {
    // iOS: spotId 짝수 → aspect 1.2(세로 김), 홀수 → 0.9. ContentScale 비율 = width/height.
    val ratio = if (item.spotId % 2L == 0L) 1f / 1.2f else 1f / 0.9f
    Box {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(ratio)
                .clip(RoundedCornerShape(12.dp))
                .background(PickflowColors.gray90),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                MoodBadge(item.mood)
                item.distanceKm?.let { DistanceBadge(it) }
            }
        }
    }
}

@Composable
private fun MoodBadge(mood: SpotListMood) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(PickflowColors.gray95),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = mood.emoji, fontSize = 11.sp)
    }
}

@Composable
private fun DistanceBadge(distanceKm: Double) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(PickflowColors.gray95)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Text(
            text = "%.1fkm".format(distanceKm),
            style = PickflowTypography.labelMedium,
            color = PickflowColors.gray10,
        )
    }
}

@Composable
private fun MetaRow(item: SpotListGridItem, isBookmarked: Boolean, bookmarkCount: Int?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = item.name,
                style = PickflowTypography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = PickflowColors.gray0,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = item.mood.displayName,
                    style = PickflowTypography.bodySmall,
                    color = PickflowColors.gray10,
                )
                if (bookmarkCount != null) {
                    Text(text = "·", style = PickflowTypography.bodySmall, color = PickflowColors.gray50)
                    Text(
                        text = "북마크 $bookmarkCount",
                        style = PickflowTypography.bodySmall,
                        color = PickflowColors.gray10,
                    )
                }
            }
        }
        Box(modifier = Modifier.padding(10.dp)) {
            Icon(
                imageVector = if (isBookmarked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                contentDescription = "북마크",
                tint = if (isBookmarked) PickflowColors.gray0 else PickflowColors.gray30,
                modifier = Modifier.size(25.dp),
            )
        }
    }
}
