package com.pickflow.android.feature.spotdetail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pickflow.android.common.designsystem.PickflowColors
import com.pickflow.android.common.designsystem.PickflowTypography

/** iOS `infoRow` / `iconContainer` 높이(54pt) 1:1. */
private val InfoRowHeight = 54.dp

/**
 * iOS `SpotRealTimeInfoSection` 1:1 이식 — 실시간 정보 카드.
 *
 * iOS `icSunny`/`icTwilight`/`icLocalParking`/`icPeople` 커스텀 아이콘은
 * 이모지·raw 텍스트로, `icHelpOutline`은 Material `Info`로 치환한다.
 */
@Composable
fun SpotRealTimeInfoSection(spot: SpotDetailData, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = buildAnnotatedString {
                append("공공 API를 활용한 ")
                withStyle(SpanStyle(color = PickflowColors.sunsetOrange)) { append("실시간 정보") }
                append("를 확인해 보세요")
            },
            style = PickflowTypography.bodyMedium,
            color = PickflowColors.gray0,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(PickflowColors.gray90)
                .padding(16.dp),
        ) {
            Text(
                text = "${spot.sunsetTime} 기준 정보입니다.",
                style = PickflowTypography.bodySmall,
                color = PickflowColors.gray50,
                textAlign = TextAlign.End,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
            )
            InfoRow(
                icon = { IconGlyph("☀️") },
                label = "현재 날씨",
                value = spot.weatherCondition,
                sub = "강수확률 ${spot.precipitationProbability}%",
            )
            InfoRow(
                icon = { IconGlyph("🌅") },
                label = "일몰 시간",
                value = spot.sunsetTime,
                sub = "오차 시간",
            )
            InfoRow(
                icon = { ParkingGlyph() },
                label = "주차 관련",
                value = if (spot.isMine) "-" else (spot.parking ?: "-"),
                sub = null,
            )
            InfoRow(
                icon = { IconGlyph("👥") },
                label = "혼잡도",
                value = if (spot.isMine) "-" else spot.congestion,
                sub = null,
                trailing = {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = null,
                        tint = PickflowColors.gray50,
                        modifier = Modifier.size(18.dp),
                    )
                },
            )
        }
    }
}

@Composable
private fun InfoRow(
    icon: @Composable () -> Unit,
    label: String,
    value: String,
    sub: String?,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(InfoRowHeight),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(InfoRowHeight), contentAlignment = Alignment.Center) { icon() }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = label,
                style = PickflowTypography.bodySmall,
                color = PickflowColors.gray50,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = value,
                    style = PickflowTypography.headingLarge,
                    color = PickflowColors.gray0,
                )
                if (sub != null) {
                    Text(
                        text = sub,
                        style = PickflowTypography.bodyMedium,
                        color = PickflowColors.gray50,
                    )
                }
                trailing?.invoke()
            }
        }
        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun IconGlyph(emoji: String) {
    Text(text = emoji, fontSize = 28.sp)
}

/** iOS `icLocalParking`(P 아이콘) 자리 — raw 텍스트 placeholder. */
@Composable
private fun ParkingGlyph() {
    Text(
        text = "P",
        style = PickflowTypography.displayMedium,
        color = PickflowColors.gray0,
    )
}
