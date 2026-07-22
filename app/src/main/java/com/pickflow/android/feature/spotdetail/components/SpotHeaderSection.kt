package com.pickflow.android.feature.spotdetail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pickflow.android.common.designsystem.PickflowColors
import com.pickflow.android.common.designsystem.PickflowTypography

/**
 * iOS `SpotHeaderSection` 1:1 이식 — 이름/MY 배지 + 테마·북마크 + 코멘트 박스.
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
                // iOS `SpotHeaderSection` MY 배지 1:1 — sunsetOrange 보더 + 텍스트, 배경 없음.
                Text(
                    text = "MY 스팟",
                    style = PickflowTypography.labelMedium,
                    color = PickflowColors.sunsetOrange,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .border(1.dp, PickflowColors.sunsetOrange, RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 3.dp),
                )
            }
        }

        Text(
            text = if (spot.isMine) spot.theme.displayName
            else "${spot.theme.displayName} · 북마크 ${spot.bookmarkCount}",
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
