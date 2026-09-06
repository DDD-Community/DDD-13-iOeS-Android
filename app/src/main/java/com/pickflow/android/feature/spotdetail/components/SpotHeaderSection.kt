package com.pickflow.android.feature.spotdetail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.pickflow.android.common.designsystem.PickflowColors
import com.pickflow.android.core.services.protocols.MySpotStatus
import com.pickflow.android.common.designsystem.PickflowTypography

/**
 * iOS `SpotHeaderSection` 1:1 이식 — 이름/MY 배지 + 테마·추천 수 + 코멘트 박스.
 */
@Composable
fun SpotHeaderSection(spot: SpotDetailData, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = spot.name,
                style = PickflowTypography.headingLarge,
                color = PickflowColors.gray0,
            )
            if (spot.isMine) {
                MySpotBadge(status = spot.mySpotStatus)
            } else if (spot.isUserRegistered) {
                UserRegisteredBadge()
            }
        }

        Text(
            text = if (spot.isMine) spot.theme.displayName
            else "${spot.theme.displayName} · 추천 ${spot.likeCount}",
            style = PickflowTypography.bodySmall,
            color = PickflowColors.gray30,
        )

        Text(
            text = spot.comment,
            style = PickflowTypography.bodyMedium,
            color = PickflowColors.gray0,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(PickflowColors.gray90)
                .padding(16.dp),
        )
    }
}

/** Figma 1014:18974 — 남이 등록한 유저 스팟임을 알리는 보더 배지. */
@Composable
private fun UserRegisteredBadge() {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .border(1.dp, PickflowColors.userSpotAmber, RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .testTag("detail-user-spot-badge"),
    ) {
        Text(
            text = "유저 등록",
            style = PickflowTypography.bodySmallBold,
            color = PickflowColors.userSpotAmber,
        )
    }
}

/**
 * MY 배지. 상태마다 문구뿐 아니라 스타일도 다르다.
 *
 * - 신청 전·공개 완료: `MY 스팟` — sunsetOrange 보더 + 텍스트 (배경 없음)
 * - 검수 대기: `검수 중` — gray80 채움 + gray30 텍스트 (보더 없음)
 * - 반려: `오픈 반려` — gray50 보더 + gray30 텍스트 (배경 없음)
 */
@Composable
internal fun MySpotBadge(status: MySpotStatus?) {
    val style = status.badgeStyle()
    val shape = RoundedCornerShape(4.dp)
    Text(
        text = style.label,
        style = PickflowTypography.labelMedium,
        color = style.contentColor,
        modifier = Modifier
            .clip(shape)
            .then(
                if (style.backgroundColor != null) {
                    Modifier.background(style.backgroundColor, shape)
                } else {
                    Modifier
                },
            )
            .then(
                if (style.borderColor != null) {
                    Modifier.border(1.dp, style.borderColor, shape)
                } else {
                    Modifier
                },
            )
            .padding(horizontal = 6.dp, vertical = 3.dp)
            .testTag(status.badgeTestTag()),
    )
}

internal data class MySpotBadgeStyle(
    val label: String,
    val contentColor: Color,
    val backgroundColor: Color? = null,
    val borderColor: Color? = null,
)

internal fun MySpotStatus?.badgeStyle(): MySpotBadgeStyle = when (this) {
    MySpotStatus.PENDING, MySpotStatus.RE_REVIEW_PENDING -> MySpotBadgeStyle(
        label = "검수 중",
        contentColor = PickflowColors.gray30,
        backgroundColor = PickflowColors.gray80,
    )
    MySpotStatus.REJECTED -> MySpotBadgeStyle(
        label = "오픈 반려",
        contentColor = PickflowColors.gray30,
        borderColor = PickflowColors.gray50,
    )
    else -> MySpotBadgeStyle(
        label = "MY 스팟",
        contentColor = PickflowColors.sunsetOrange,
        borderColor = PickflowColors.sunsetOrange,
    )
}

private fun MySpotStatus?.badgeTestTag(): String = when (this) {
    MySpotStatus.PENDING, MySpotStatus.RE_REVIEW_PENDING -> "spot-status-pending"
    MySpotStatus.REJECTED -> "spot-status-rejected"
    else -> "spot-status-my"
}
