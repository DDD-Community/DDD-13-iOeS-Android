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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pickflow.android.common.designsystem.PickflowColors

/**
 * iOS `SpotMarkerView` 1:1 이식 — 단일 스팟 마커(지름 44).
 *
 * 검정 그라데이션 원 + 사진 아이콘. iOS `icPhoto` 커스텀 에셋은 이모지(🖼️)로 치환.
 * 선택 시 sunsetOrange 4dp 테두리.
 */
@Composable
fun SpotMarkerView(
    isSelected: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color.Black.copy(alpha = 0f), Color.Black.copy(alpha = 0.7f)),
                ),
            )
            .then(
                if (isSelected) {
                    Modifier.border(4.dp, PickflowColors.sunsetOrange, CircleShape)
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = "🖼️", fontSize = 18.sp)
    }
}
