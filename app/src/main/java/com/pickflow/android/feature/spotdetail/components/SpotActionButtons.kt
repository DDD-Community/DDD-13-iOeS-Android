package com.pickflow.android.feature.spotdetail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
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
 * iOS `SpotActionButtons` 1:1 이식 — 길 안내 + (북마크 | 내 스팟 오픈) 버튼.
 *
 * iOS `icNearMe` 커스텀 아이콘은 Material `Send`, 북마크 아이콘은
 * `Favorite`/`FavoriteBorder`로 치환한다.
 */
@Composable
fun SpotActionButtons(
    isMine: Boolean,
    isBookmarked: Boolean,
    modifier: Modifier = Modifier,
    onRoute: () -> Unit = {},
    onBookmark: () -> Unit = {},
    onOpenSpot: () -> Unit = {},
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val routeHeight = if (isMine) 52.dp else 56.dp
        RouteButton(height = routeHeight, modifier = Modifier.weight(1f), onClick = onRoute)

        if (isMine) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(PickflowColors.gray95)
                    .border(1.dp, PickflowColors.gray80, RoundedCornerShape(8.dp))
                    .clickable(onClick = onOpenSpot)
                    .testTag("detail-open-spot"),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "내 스팟 오픈하기",
                    style = PickflowTypography.bodyLargeBold,
                    color = PickflowColors.gray0,
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(PickflowColors.gray0)
                    .clickable(onClick = onBookmark)
                    .testTag("detail-bookmark"),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (isBookmarked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = "북마크",
                    tint = PickflowColors.gray95,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}

@Composable
private fun RouteButton(
    height: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(8.dp))
            .background(PickflowColors.sunsetOrange)
            .clickable(onClick = onClick)
            .testTag("detail-route"),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = null,
                tint = PickflowColors.gray0,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = "길 안내 받기",
                style = PickflowTypography.bodyLargeBold,
                color = PickflowColors.gray0,
            )
        }
    }
}
