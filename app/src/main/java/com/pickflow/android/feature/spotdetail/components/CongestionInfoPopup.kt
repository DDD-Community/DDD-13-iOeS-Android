package com.pickflow.android.feature.spotdetail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pickflow.android.common.designsystem.PickflowColors
import com.pickflow.android.common.designsystem.PickflowTypography

/** iOS `CongestionInfoPopup` 범위 배지 배경 `Color(0.29, 0.10, 0.04)`. */
private val RangeBadgeBackground = Color(0xFF4A1A0A)

private data class CongestionLevelInfo(
    val name: String,
    val range: String,
    val description: String,
)

private val Levels = listOf(
    CongestionLevelInfo("여유", "50% 이하", "인구가 평소와 비교하여 적음"),
    CongestionLevelInfo("보통", "50~75%", "인구가 평소와 비교하여 비슷함"),
    CongestionLevelInfo("약간 붐빔", "75~100%", "인구가 평소와 비교하여 많음"),
    CongestionLevelInfo("붐빔", "100% 초과", "인구가 평소와 비교하여 매우 많음"),
)

/**
 * iOS `CongestionInfoPopup.swift` 1:1 — 혼잡도 표시 기준 안내 팝업.
 * 딤 배경 탭 또는 X 버튼으로 닫는다.
 */
@Composable
fun CongestionInfoPopup(onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(PickflowColors.gray90)
                .padding(24.dp)
                .testTag("congestion-info-popup"),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 28.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spacer(Modifier.size(24.dp))
                Text(
                    text = "혼잡도 표시 기준",
                    style = PickflowTypography.bodyLargeBold,
                    color = PickflowColors.gray0,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                )
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "닫기",
                    tint = PickflowColors.gray0,
                    modifier = Modifier
                        .size(24.dp)
                        .clickable(onClick = onDismiss)
                        .testTag("congestion-info-close"),
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                Levels.forEach { item ->
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = item.name,
                                style = PickflowTypography.headingLarge,
                                color = PickflowColors.gray0,
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(RangeBadgeBackground)
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                            ) {
                                Text(
                                    text = item.range,
                                    style = PickflowTypography.bodySmallBold,
                                    color = PickflowColors.sunsetOrange,
                                )
                            }
                        }
                        Text(
                            text = item.description,
                            style = PickflowTypography.bodyMedium,
                            color = PickflowColors.gray50,
                        )
                    }
                }
            }
        }
    }
}
