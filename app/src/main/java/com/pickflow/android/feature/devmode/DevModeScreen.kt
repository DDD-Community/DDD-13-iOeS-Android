package com.pickflow.android.feature.devmode

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import com.pickflow.android.BuildConfig
import com.pickflow.android.common.designsystem.PickflowColors
import com.pickflow.android.common.designsystem.PickflowTypography
import com.pickflow.android.core.services.protocols.ApiEnvironment

/**
 * 개발자 전용 설정 화면 — 마이페이지 앱 버전 연타 + 코드 입력, 또는 환경 배지 탭으로 진입한다.
 * 여기서 고른 API 환경은 [com.pickflow.android.core.network.ApiEnvironmentInterceptor] 가 즉시 반영한다.
 */
@Composable
fun DevModeScreen(
    onBack: () -> Unit,
    viewModel: DevModeViewModel = hiltViewModel(),
) {
    val environment by viewModel.apiEnvironment.collectAsStateWithLifecycle()
    val badgeEnabled by viewModel.badgeEnabled.collectAsStateWithLifecycle()
    val touchIndicator by viewModel.touchIndicatorEnabled.collectAsStateWithLifecycle()
    val pendingEnvironment by viewModel.pendingEnvironment.collectAsStateWithLifecycle()
    val onboardingCompleted by viewModel.onboardingCompleted.collectAsStateWithLifecycle()
    val guestEntered by viewModel.guestEntered.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PickflowColors.gray95)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 40.dp)
            .testTag("devmode-screen"),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Dev Mode",
                style = PickflowTypography.headingSmall,
                color = PickflowColors.gray0,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "닫기",
                style = PickflowTypography.bodyMedium,
                color = PickflowColors.gray20,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(PickflowColors.gray80)
                    .clickable(onClick = onBack)
                    .padding(horizontal = 20.dp, vertical = 10.dp)
                    .testTag("devmode-close"),
            )
        }

        SectionTitle("API 환경")
        Text(
            text = "기본값은 ${ApiEnvironment.DEFAULT.label} 이에요.\n" +
                "여기서 고른 환경은 앱을 껐다 켜도 그대로 유지돼요.",
            style = PickflowTypography.bodySmall,
            color = PickflowColors.gray40,
        )
        Spacer(Modifier.height(12.dp))
        ApiEnvironment.entries.forEach { candidate ->
            EnvironmentRow(
                environment = candidate,
                selected = candidate == environment,
                onClick = { viewModel.selectEnvironment(candidate) },
            )
            Spacer(Modifier.height(8.dp))
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = "현재 요청 주소",
            style = PickflowTypography.bodySmallBold,
            color = PickflowColors.gray30,
        )
        Text(
            text = environment.baseUrl,
            style = PickflowTypography.bodySmall,
            color = PickflowColors.sunsetOrange,
            modifier = Modifier.testTag("devmode-current-base-url"),
        )

        SectionTitle("표시")
        ToggleRow(
            title = "환경 배지 띄우기",
            description = "화면 위에 현재 환경을 항상 표시해요.",
            checked = badgeEnabled,
            onCheckedChange = viewModel::setBadgeEnabled,
            tag = "devmode-badge-toggle",
        )
        Spacer(Modifier.height(12.dp))
        ToggleRow(
            title = "터치 표시하기",
            description = "손가락이 닿는 자리에 원을 그려요. 화면 녹화에도 함께 찍혀요.",
            checked = touchIndicator,
            onCheckedChange = viewModel::setTouchIndicatorEnabled,
            tag = "devmode-touch-toggle",
        )

        SectionTitle("진입")
        ToggleRow(
            title = "온보딩 확인여부",
            description = "off 로 내리면 앱을 껐다 켤 때 온보딩이 다시 나와요.",
            checked = onboardingCompleted,
            onCheckedChange = viewModel::setOnboardingCompleted,
            tag = "devmode-onboarding-toggle",
        )
        Spacer(Modifier.height(12.dp))
        ToggleRow(
            title = "비회원 진입 이력",
            description = "on 이면 앱을 켤 때 로그인 화면을 건너뛰고 탐색 탭으로 바로 가요.",
            checked = guestEntered,
            onCheckedChange = viewModel::setGuestEntered,
            tag = "devmode-guest-toggle",
        )

        SectionTitle("앱 정보")
        InfoRow("버전", BuildConfig.VERSION_NAME)
        InfoRow("빌드", BuildConfig.VERSION_CODE.toString())
        InfoRow("패키지", BuildConfig.APPLICATION_ID)
    }

    pendingEnvironment?.let { target ->
        EnvironmentSwitchedDialog(
            environment = target,
            onConfirm = viewModel::confirmEnvironmentChange,
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Spacer(Modifier.height(28.dp))
    Text(
        text = text,
        style = PickflowTypography.headingSmall,
        color = PickflowColors.gray0,
    )
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun EnvironmentRow(
    environment: ApiEnvironment,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(PickflowColors.gray90)
            .then(
                if (selected) {
                    Modifier.border(1.dp, PickflowColors.sunsetOrange, RoundedCornerShape(12.dp))
                } else {
                    Modifier
                },
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag("devmode-env-${environment.shortLabel}"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = environment.label,
                    style = PickflowTypography.bodyLargeBold,
                    color = PickflowColors.gray0,
                )
                if (environment == ApiEnvironment.DEFAULT) {
                    Spacer(Modifier.padding(horizontal = 4.dp))
                    Text(
                        text = "기본",
                        style = PickflowTypography.bodySmall,
                        color = PickflowColors.gray40,
                    )
                }
            }
            Text(
                text = environment.baseUrl,
                style = PickflowTypography.bodySmall,
                color = PickflowColors.gray40,
            )
        }
        if (selected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = "선택됨",
                tint = PickflowColors.sunsetOrange,
            )
        }
    }
}

@Composable
private fun ToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    tag: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(PickflowColors.gray90)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = PickflowTypography.bodyLargeBold,
                color = PickflowColors.gray0,
            )
            Text(
                text = description,
                style = PickflowTypography.bodySmall,
                color = PickflowColors.gray40,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = PickflowColors.gray0,
                checkedTrackColor = PickflowColors.sunsetOrange,
                uncheckedThumbColor = PickflowColors.gray0,
                uncheckedTrackColor = PickflowColors.gray60,
            ),
            modifier = Modifier.testTag(tag),
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Box(Modifier.padding(vertical = 6.dp)) {
        Column {
            Text(
                text = label,
                style = PickflowTypography.bodySmall,
                color = PickflowColors.gray40,
            )
            Text(
                text = value,
                style = PickflowTypography.bodyMedium,
                color = PickflowColors.gray0,
            )
        }
    }
}
