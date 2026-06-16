package com.pickflow.android.feature.withdrawal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pickflow.android.common.designsystem.PickflowColors
import com.pickflow.android.common.designsystem.PickflowTypography
import com.pickflow.android.feature.withdrawal.components.WithdrawalCompletedContent
import com.pickflow.android.feature.withdrawal.components.WithdrawalContent

/**
 * iOS `WithdrawalView` 1:1 — step 별 분기 + ViewModel 액션 라우팅.
 * 탈퇴 성공 시 `onWithdrawn` 으로 LoginScreen 까지 popBackStack.
 */
@Composable
fun WithdrawalScreen(
    onBack: () -> Unit,
    onWithdrawn: () -> Unit,
    viewModel: WithdrawalViewModel = hiltViewModel(),
) {
    val step by viewModel.step.collectAsStateWithLifecycle()
    val selectedReason by viewModel.selectedReason.collectAsStateWithLifecycle()
    val isDropdownOpen by viewModel.isDropdownOpen.collectAsStateWithLifecycle()
    val otherFeedback by viewModel.otherFeedback.collectAsStateWithLifecycle()
    val didAgree by viewModel.didAgree.collectAsStateWithLifecycle()

    // 완료 상태에서는 back 도 로그인 화면으로 이동.
    if (step is WithdrawalViewModel.Step.Done) {
        BackHandler { onWithdrawn() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PickflowColors.gray95)
            .testTag("withdrawal-screen"),
    ) {
        when (val s = step) {
            WithdrawalViewModel.Step.Input -> WithdrawalContent(
                selectedReason = selectedReason,
                isDropdownOpen = isDropdownOpen,
                otherFeedback = otherFeedback,
                didAgree = didAgree,
                onBack = onBack,
                onToggleDropdown = viewModel::toggleDropdown,
                onSelectReason = viewModel::selectReason,
                onOtherFeedbackChange = viewModel::updateOtherFeedback,
                onToggleAgree = viewModel::toggleAgreement,
                onSubmit = viewModel::submitWithdrawal,
            )
            WithdrawalViewModel.Step.Processing -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator(color = PickflowColors.gray0) }
            WithdrawalViewModel.Step.Done -> WithdrawalCompletedContent(onGoToLogin = onWithdrawn)
            is WithdrawalViewModel.Step.Failed -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = null,
                    tint = PickflowColors.sunsetOrange,
                )
                Text(
                    text = s.message,
                    style = PickflowTypography.bodyMedium,
                    color = PickflowColors.gray30,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
