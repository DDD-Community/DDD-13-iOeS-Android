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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.pickflow.android.R
import com.pickflow.android.common.designsystem.PickflowColors
import com.pickflow.android.common.designsystem.PickflowTypography

/**
 * iOS `SpotActionButtons` 1:1 이식 — 길 안내 + (북마크 | 내 스팟 오픈) + 추천 버튼.
 *
 * iOS `icNearMe` 커스텀 아이콘은 Material `Send`로 치환하고,
 * 북마크는 Figma 에셋(ic_bookmark_border/ic_bookmark_filled)을 그대로 쓴다.
 */
@Composable
fun SpotActionButtons(
    isMine: Boolean,
    isBookmarked: Boolean,
    modifier: Modifier = Modifier,
    isLikeable: Boolean = false,
    isLiked: Boolean = false,
    onRoute: () -> Unit = {},
    onBookmark: () -> Unit = {},
    onOpenSpot: () -> Unit = {},
    onLike: () -> Unit = {},
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val routeHeight = if (isMine) 52.dp else 56.dp
        RouteButton(height = routeHeight, modifier = Modifier.weight(1f), onClick = onRoute)

        if (isMine) {
            // iOS `SpotActionButtons` 1:1 — gray0 배경 + gray80 텍스트/보더.
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(PickflowColors.gray0)
                    .border(1.dp, PickflowColors.gray80, RoundedCornerShape(8.dp))
                    .clickable(onClick = onOpenSpot)
                    .testTag("detail-open-spot"),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "내 스팟 오픈하기",
                    style = PickflowTypography.bodyLargeBold,
                    color = PickflowColors.gray80,
                )
            }
        } else {
            SquareIconButton(
                iconRes = if (isBookmarked) R.drawable.ic_bookmark_filled else R.drawable.ic_bookmark_border,
                contentDescription = if (isBookmarked) "북마크 해제" else "북마크 추가",
                testTag = "detail-bookmark",
                onClick = onBookmark,
            )
        }

        // 추천 가능 여부는 서버 isLikeable 을 그대로 따른다(내 스팟 등은 false).
        if (isLikeable) {
            SquareIconButton(
                iconRes = if (isLiked) R.drawable.ic_thumb_up_filled else R.drawable.ic_thumb_up_border,
                contentDescription = if (isLiked) "추천 취소" else "추천하기",
                testTag = "detail-like",
                onClick = onLike,
            )
        }
    }
}

/** 북마크/추천 공통 56x56 정사각 아이콘 버튼 — gray0 배경 + gray95 아이콘. */
@Composable
private fun SquareIconButton(
    iconRes: Int,
    contentDescription: String,
    testTag: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(PickflowColors.gray0)
            .clickable(onClick = onClick)
            .testTag(testTag),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            tint = PickflowColors.gray95,
            modifier = Modifier.size(24.dp),
        )
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
