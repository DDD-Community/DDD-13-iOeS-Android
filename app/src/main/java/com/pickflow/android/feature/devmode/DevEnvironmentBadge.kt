package com.pickflow.android.feature.devmode

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pickflow.android.common.designsystem.PickflowColors
import com.pickflow.android.common.designsystem.PickflowTypography

/**
 * Dev Mode 에서 켠 환경 배지 — 홈 3탭 어디서나 좌측 하단에 떠 있고, 탭하면 Dev Mode 로 들어간다.
 * 토글이 꺼져 있으면 아무것도 그리지 않는다.
 */
@Composable
fun DevEnvironmentBadge(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DevModeViewModel = hiltViewModel(),
) {
    val enabled by viewModel.badgeEnabled.collectAsStateWithLifecycle()
    if (!enabled) return
    val environment by viewModel.apiEnvironment.collectAsStateWithLifecycle()

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(PickflowColors.gray90)
            .border(1.dp, PickflowColors.sunsetOrange, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp)
            .testTag("dev-environment-badge"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(PickflowColors.sunsetOrange),
        )
        Text(
            text = environment.shortLabel.uppercase(),
            style = PickflowTypography.bodyMediumBold,
            color = PickflowColors.gray0,
        )
    }
}
