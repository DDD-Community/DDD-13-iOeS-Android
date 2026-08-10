package com.pickflow.android.feature.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.pickflow.android.common.designsystem.PickflowColors
import com.pickflow.android.common.designsystem.PickflowTypography

import com.pickflow.android.core.services.protocols.ReviewDecision

/** PV-41 공유 Stub backend의 상태·실패·경합을 재현하는 DEBUG 진입점. */
@Composable
fun DebugRouteScreen(
    onBack: () -> Unit,
    onOpenSpotDetail: (Long) -> Unit = {},
    viewModel: DebugViewModel = hiltViewModel(),
) {
    val message by viewModel.message.collectAsStateWithLifecycle()
    DebugScreen(
        onBack = onBack,
        onOpenSpotDetail = onOpenSpotDetail,
        message = message,
        fixtures = viewModel.fixtures,
        onReset = viewModel::reset,
        onFailNextOpenRequest = viewModel::failNextOpenRequest,
        onDelayNextOpenRequest = viewModel::delayNextOpenRequest,
        onRaceNextWithdrawal = viewModel::raceNextWithdrawal,
        onApprovePending = viewModel::approvePending,
        onRejectPending = viewModel::rejectPending,
    )
}

@Composable
fun DebugScreen(
    onBack: () -> Unit,
    onOpenSpotDetail: (Long) -> Unit = {},
    message: String = "초기 성공 fixture",
    fixtures: List<SpotOpenDebugFixture> = emptyList(),
    onReset: () -> Unit = {},
    onFailNextOpenRequest: () -> Unit = {},
    onDelayNextOpenRequest: () -> Unit = {},
    onRaceNextWithdrawal: (ReviewDecision) -> Unit = {},
    onApprovePending: () -> Unit = {},
    onRejectPending: () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PickflowColors.gray95)
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
            .testTag("debug-screen"),
    ) {
        Text(
            text = "Debug",
            style = PickflowTypography.headingLarge,
            color = PickflowColors.gray0,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "PV-41 Stub fixture · 다음 요청 시나리오",
            style = PickflowTypography.bodyMedium,
            color = PickflowColors.gray30,
        )
        Text(
            text = message,
            style = PickflowTypography.bodySmall,
            color = PickflowColors.sunsetOrange,
            modifier = Modifier.padding(top = 8.dp),
        )
        Spacer(Modifier.height(16.dp))
        fixtures.forEach { fixture ->
            Button(onClick = { onOpenSpotDetail(fixture.spotId) }) { Text(fixture.label) }
        }
        Spacer(Modifier.height(16.dp))
        Button(onClick = onReset) { Text("fixture 초기화") }
        Button(onClick = onFailNextOpenRequest) { Text("다음 신청 실패") }
        Button(onClick = onDelayNextOpenRequest) { Text("다음 신청 1초 지연") }
        Button(onClick = { onRaceNextWithdrawal(ReviewDecision.APPROVED) }) {
            Text("다음 철회 ↔ 승인 경합")
        }
        Button(onClick = { onRaceNextWithdrawal(ReviewDecision.REJECTED) }) {
            Text("다음 철회 ↔ 반려 경합")
        }
        Button(onClick = onApprovePending) { Text("PENDING 승인 결과 생성") }
        Button(onClick = onRejectPending) { Text("PENDING 반려 결과 생성") }
        Spacer(Modifier.height(24.dp))
        Button(onClick = onBack) { Text("닫기") }
    }
}
