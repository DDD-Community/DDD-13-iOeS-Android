package com.pickflow.android.feature.map.clustering

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.pickflow.android.R
import com.pickflow.android.common.designsystem.PickflowColors
import com.pickflow.android.common.designsystem.PickflowTypography

/**
 * iOS `MyClusterPinView` 1:1 이식 — MY 스팟 단일 마커(지름 56).
 *
 * 흰 원 + 검정 0.2 오버레이 + drop shadow / 검정 그라데이션 / 사진 아이콘(`ic_photo` 18dp,
 * opacity 0.5) + "MY". 선택 시 sunsetOrange 4dp 테두리.
 */
@Composable
fun MyClusterPinView(
    isSelected: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.size(56.dp),
        contentAlignment = Alignment.Center,
    ) {
        // circleBackground — 흰 원 + 검정 0.2 오버레이 + shadow
        Box(
            modifier = Modifier
                .fillMaxSize()
                .shadow(4.dp, CircleShape)
                .clip(CircleShape)
                .background(Color.White)
                .background(Color.Black.copy(alpha = 0.2f)),
        )
        // rectangle125 — 30%~100% 검정 그라데이션
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.30f to Color.Black.copy(alpha = 0f),
                            1.0f to Color.Black.copy(alpha = 0.7f),
                        ),
                    ),
                ),
        )
        // content — 사진 아이콘 + "MY"
        Box(contentAlignment = Alignment.BottomCenter) {
            // 아이콘은 iOS `Image`(고정 프레임)처럼 Dynamic Type 비반응.
            Image(
                painter = painterResource(R.drawable.ic_photo),
                contentDescription = null,
                modifier = Modifier
                    .padding(10.dp)
                    .size(18.dp)
                    .alpha(0.5f),
            )
            // iOS는 a11y에서 "MY"가 프레임을 넘쳐 truncation(…)으로 렌더된다.
            // 폭을 아이콘 박스에 맞춰(26dp) 동일하게 a11y 시 ellipsis 처리.
            Text(
                text = "MY",
                style = PickflowTypography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = PickflowColors.gray0,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 26.dp),
            )
        }
        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(4.dp, PickflowColors.sunsetOrange, CircleShape),
            )
        }
    }
}
