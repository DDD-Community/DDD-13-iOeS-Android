package com.pickflow.android.feature.map.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.pickflow.android.common.designsystem.PickflowColors
import com.pickflow.android.common.designsystem.PickflowTypography

/**
 * V2 업데이트 안내 팝업 — NEW 배지 + 변경점 2줄 + 단일 확인 버튼.
 * 컨테이너(반투명 오버레이)는 호출 측에서 제공. 확인 외 dismiss 는 없다.
 */
@Composable
fun V2NoticePopup(
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(PickflowColors.gray90)
            .padding(24.dp)
            .testTag("v2-notice-popup"),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "NEW",
            style = PickflowTypography.labelSmall,
            color = PickflowColors.gray0,
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(PickflowColors.sunsetOrange)
                .padding(horizontal = 10.dp, vertical = 5.dp),
        )
        Text(
            text = "V2 업데이트 안내",
            style = PickflowTypography.headingMedium,
            color = PickflowColors.gray0,
            textAlign = TextAlign.Center,
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = highlighted("1. ", "햇살 · 야경 필터", "가 추가되었어요"),
                style = PickflowTypography.bodyMedium,
                color = PickflowColors.gray20,
                textAlign = TextAlign.Center,
            )
            Text(
                text = highlighted("2. 이제 내 ", "스팟을 공개", "할 수 있어요"),
                style = PickflowTypography.bodyMedium,
                color = PickflowColors.gray20,
                textAlign = TextAlign.Center,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .height(52.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(PickflowColors.sunsetOrange)
                .clickable(onClick = onConfirm)
                .testTag("v2-notice-confirm"),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "확인했어요",
                style = PickflowTypography.bodyLargeBold,
                color = PickflowColors.gray0,
            )
        }
    }
}

private fun highlighted(prefix: String, bold: String, suffix: String): AnnotatedString =
    buildAnnotatedString {
        append(prefix)
        withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = PickflowColors.gray0)) {
            append(bold)
        }
        append(suffix)
    }
