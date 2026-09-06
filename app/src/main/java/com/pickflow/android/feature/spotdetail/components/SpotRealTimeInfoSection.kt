package com.pickflow.android.feature.spotdetail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.LocalParking
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pickflow.android.R
import com.pickflow.android.common.designsystem.PickflowColors
import com.pickflow.android.common.designsystem.PickflowTypography

/** iOS `infoRow` / `iconContainer` 높이(54pt) 1:1. */
private val InfoRowHeight = 54.dp

/**
 * iOS `SpotRealTimeInfoSection` 1:1 이식 — 실시간 정보 카드.
 *
 * iOS 자체 자산 → Material Icons 매핑:
 *  - `icSunny` → `Icons.Filled.WbSunny`
 *  - `icTwilight` → `Icons.Filled.WbTwilight`
 *  - `icLocalParking` → `Icons.Filled.LocalParking`
 *  - `icPeople` → `Icons.Filled.People`
 *  - `icHelpOutline` → `R.drawable.ic_help_outline` (figma 1:1, 탭 시 혼잡도 기준 팝업)
 */
@Composable
fun SpotRealTimeInfoSection(spot: SpotDetailData, modifier: Modifier = Modifier) {
    var showCongestionInfo by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = buildAnnotatedString {
                append("공공 API를 활용한 ")
                withStyle(SpanStyle(color = PickflowColors.sunsetOrange)) { append("현재 스팟 상황") }
                append("을 살펴보세요")
            },
            style = PickflowTypography.bodyMedium,
            color = PickflowColors.gray0,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = "지역에 따라 정보 기준이 다르거나\n일부 정보가 제공되지 않을 수 있어요",
            style = PickflowTypography.bodyMedium,
            color = PickflowColors.gray50,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("realtime-info-disclaimer"),
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(PickflowColors.gray90)
                .padding(16.dp),
        ) {
            // 기준 시각을 모르면 "정보 없음 기준 정보입니다." 가 되므로 줄 자체를 감춘다.
            if (spot.sunsetTime != NO_INFO) {
                Text(
                    text = "${spot.sunsetTime} 기준 정보입니다.",
                    style = PickflowTypography.bodySmall,
                    color = PickflowColors.gray50,
                    textAlign = TextAlign.End,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                )
            }
            InfoRow(
                icon = { MaterialGlyph(Icons.Filled.WbSunny) },
                label = "현재 날씨",
                value = spot.weatherCondition,
                // 날씨를 못 받았으면 "강수확률 0%" 를 같이 띄우지 않는다.
                sub = "강수확률 ${spot.precipitationProbability}%"
                    .takeIf { spot.weatherCondition != NO_INFO },
            )
            InfoRow(
                icon = { MaterialGlyph(Icons.Filled.WbTwilight) },
                label = "일몰 시간",
                value = spot.sunsetTime,
                sub = "오차 시간".takeIf { spot.sunsetTime != NO_INFO },
            )
            InfoRow(
                icon = { MaterialGlyph(Icons.Filled.LocalParking) },
                label = "주차 관련",
                value = if (spot.isMine) NO_INFO else (spot.parking ?: NO_INFO),
                sub = null,
            )
            InfoRow(
                icon = { MaterialGlyph(Icons.Filled.People) },
                label = "혼잡도",
                value = if (spot.isMine) NO_INFO else spot.congestion,
                sub = null,
                trailing = {
                    Icon(
                        painter = painterResource(R.drawable.ic_help_outline),
                        contentDescription = "혼잡도 표시 기준",
                        tint = PickflowColors.gray50,
                        modifier = Modifier
                            .size(20.dp)
                            .clickable { showCongestionInfo = true }
                            .testTag("congestion-info-button"),
                    )
                },
            )
        }
    }

    if (showCongestionInfo) {
        CongestionInfoPopup(onDismiss = { showCongestionInfo = false })
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

/** iOS 자체 자산(`icSunny`/`icTwilight`/`icLocalParking`/`icPeople`) → Material Icons 컨테이너. */
@Composable
private fun MaterialGlyph(icon: ImageVector) {
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = PickflowColors.gray0,
        modifier = Modifier.size(28.dp),
    )
}
