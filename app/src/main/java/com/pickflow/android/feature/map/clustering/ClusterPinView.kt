package com.pickflow.android.feature.map.clustering

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pickflow.android.common.designsystem.PickflowColors
import com.pickflow.android.common.designsystem.PickflowTypography

/**
 * iOS `ClusterPinView` 1:1 이식 — 큐레이션 클러스터 핀.
 *
 * sunsetOrange 원 + 개수 텍스트. 개수에 따라 지름이 4단계로 커진다.
 * 선택 시 sunsetOrange 4dp 테두리(원 색과 동일해 사실상 비가시 — iOS 동일).
 */
@Composable
fun ClusterPinView(
    count: Int,
    isSelected: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val diameter = clusterPinDiameter(count)
    Box(
        modifier = modifier
            .size(diameter.dp)
            .clip(CircleShape)
            .background(PickflowColors.sunsetOrange)
            .then(
                if (isSelected) {
                    Modifier.border(4.dp, PickflowColors.sunsetOrange, CircleShape)
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "$count",
            style = PickflowTypography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
            color = PickflowColors.gray0,
            maxLines = 1,
        )
    }
}

/** iOS `ClusterPinView.diameter(forCount:)` 1:1. */
fun clusterPinDiameter(count: Int): Int = when {
    count < 10 -> 44
    count < 50 -> 54
    count < 100 -> 64
    else -> 74
}
