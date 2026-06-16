package com.pickflow.android.feature.withdrawal.components

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pickflow.android.common.designsystem.PickflowColors
import com.pickflow.android.common.designsystem.PickflowTypography
import com.pickflow.android.feature.withdrawal.model.WithdrawalReason

/**
 * iOS `WithdrawalView` 1:1 — 회원탈퇴 화면 본체 (stateless).
 *
 * navBar + (유의사항 박스 / 사유 dropdown / Other 입력 / 동의 체크 / 제출 버튼).
 */
@Composable
fun WithdrawalContent(
    selectedReason: WithdrawalReason? = null,
    isDropdownOpen: Boolean = false,
    otherFeedback: String = "",
    didAgree: Boolean = false,
    onBack: () -> Unit = {},
    onToggleDropdown: () -> Unit = {},
    onSelectReason: (WithdrawalReason) -> Unit = {},
    onOtherFeedbackChange: (String) -> Unit = {},
    onToggleAgree: () -> Unit = {},
    onSubmit: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val canSubmit = run {
        val reason = selectedReason ?: return@run false
        if (!didAgree) return@run false
        if (reason == WithdrawalReason.Other && otherFeedback.trim().isEmpty()) return@run false
        true
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PickflowColors.gray95)
            .statusBarsPadding()
            .testTag("withdrawal-content"),
    ) {
        NavBar(onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 8.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            CautionBox()
            ReasonSection(
                selectedReason = selectedReason,
                isDropdownOpen = isDropdownOpen,
                otherFeedback = otherFeedback,
                onToggleDropdown = onToggleDropdown,
                onSelectReason = onSelectReason,
                onOtherFeedbackChange = onOtherFeedbackChange,
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AgreementRow(didAgree = didAgree, onToggle = onToggleAgree)
            SubmitButton(canSubmit = canSubmit, onSubmit = onSubmit)
        }
    }
}

@Composable
private fun NavBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clickable(onClick = onBack)
                .testTag("withdrawal-back"),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.KeyboardArrowLeft,
                contentDescription = "뒤로",
                tint = PickflowColors.gray0,
                modifier = Modifier.size(28.dp),
            )
        }
        Spacer(Modifier.weight(1f))
        Text(
            text = "회원탈퇴",
            style = PickflowTypography.headingSmall,
            color = PickflowColors.gray0,
        )
        Spacer(Modifier.weight(1f))
        Spacer(Modifier.size(44.dp))
    }
}

@Composable
private fun CautionBox() {
    Column(
        modifier = Modifier
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "탈퇴 유의사항 안내",
            style = PickflowTypography.headingSmall,
            color = PickflowColors.gray0,
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(PickflowColors.gray80)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "계정을 삭제하면 저장한 스팟과 기록이 모두 삭제됩니다.",
                style = PickflowTypography.bodyMedium,
                color = PickflowColors.gray0,
            )
            Text(
                text = "삭제된 정보는 복구할 수 없으니 신중하게 결정해주세요.",
                style = PickflowTypography.bodyMedium,
                color = PickflowColors.sunsetOrange,
            )
        }
    }
}

@Composable
private fun ReasonSection(
    selectedReason: WithdrawalReason?,
    isDropdownOpen: Boolean,
    otherFeedback: String,
    onToggleDropdown: () -> Unit,
    onSelectReason: (WithdrawalReason) -> Unit,
    onOtherFeedbackChange: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "어떤 점이 아쉬우셨나요?",
                style = PickflowTypography.headingSmall,
                color = PickflowColors.gray0,
            )
            Text(
                text = "소중한 의견을 바탕으로\n더 나은 서비스 경험을 만들어가겠습니다.",
                style = PickflowTypography.bodySmall,
                color = PickflowColors.gray40,
            )
        }

        WithdrawalReasonDropdown(
            selectedReason = selectedReason,
            isOpen = isDropdownOpen,
            onToggle = onToggleDropdown,
            onSelect = onSelectReason,
        )

        if (selectedReason == WithdrawalReason.Other) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 100.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(PickflowColors.gray80)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                if (otherFeedback.isEmpty()) {
                    Text(
                        text = "사용하시면서 아쉬웠던 점을 알려주세요",
                        style = PickflowTypography.bodyMedium,
                        color = PickflowColors.gray50,
                    )
                }
                BasicTextField(
                    value = otherFeedback,
                    onValueChange = onOtherFeedbackChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("withdrawal-other-feedback"),
                    textStyle = TextStyle(
                        color = PickflowColors.gray0,
                        fontSize = PickflowTypography.bodyMedium.fontSize,
                    ),
                    cursorBrush = SolidColor(PickflowColors.sunsetOrange),
                )
            }
        }
    }
}

@Composable
private fun AgreementRow(didAgree: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .testTag("withdrawal-agree-row"),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(RoundedCornerShape(4.dp))
                .then(
                    if (didAgree) {
                        Modifier.background(PickflowColors.sunsetOrange)
                    } else {
                        Modifier.border(1.5.dp, PickflowColors.gray50, RoundedCornerShape(4.dp))
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (didAgree) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = PickflowColors.gray0,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
        Text(
            text = "안내사항을 모두 확인하였으며 이에 동의합니다.",
            style = PickflowTypography.bodyMedium,
            color = PickflowColors.gray20,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun SubmitButton(canSubmit: Boolean, onSubmit: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (canSubmit) PickflowColors.sunsetOrange else PickflowColors.gray70)
            .clickable(enabled = canSubmit, onClick = onSubmit)
            .testTag("withdrawal-submit"),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "탈퇴하기",
            style = PickflowTypography.bodyLargeBold,
            color = PickflowColors.gray0,
            textAlign = TextAlign.Center,
        )
    }
}
