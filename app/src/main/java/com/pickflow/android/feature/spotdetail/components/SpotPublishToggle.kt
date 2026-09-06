package com.pickflow.android.feature.spotdetail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.pickflow.android.common.designsystem.PickflowColors
import com.pickflow.android.common.designsystem.PickflowTypography

/**
 * 공개된 MY 스팟의 공개 여부 스위치.
 *
 * 끄면 오픈 취소(공개 해제)다 — 서버 상태를 바꾸는 동작이라 화면이 확인 시트를 먼저 띄운다.
 * 그래서 이 컴포넌트는 상태를 스스로 갖지 않고 [isPublished] 를 그대로 그린다.
 */
@Composable
fun SpotPublishToggle(
    isPublished: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(PickflowColors.gray90)
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag("detail-publish-toggle"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = buildAnnotatedString {
                    append("스팟 공개 ")
                    withStyle(
                        SpanStyle(
                            color = if (isPublished) {
                                PickflowColors.sunsetOrange
                            } else {
                                PickflowColors.gray50
                            },
                        ),
                    ) {
                        append(if (isPublished) "ON" else "OFF")
                    }
                },
                style = PickflowTypography.bodyLargeBold,
                color = PickflowColors.gray0,
            )
            Text(
                text = if (isPublished) {
                    "다른 사용자에게 MY 스팟을 공개합니다."
                } else {
                    "다른 사용자에게 MY 스팟이 노출되지 않습니다."
                },
                style = PickflowTypography.bodySmall,
                color = PickflowColors.gray30,
            )
        }
        Switch(
            checked = isPublished,
            onCheckedChange = onToggle,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = PickflowColors.gray0,
                checkedTrackColor = PickflowColors.sunsetOrange,
                uncheckedThumbColor = PickflowColors.gray0,
                uncheckedTrackColor = PickflowColors.gray60,
            ),
            modifier = Modifier.testTag("detail-publish-switch"),
        )
    }
}
