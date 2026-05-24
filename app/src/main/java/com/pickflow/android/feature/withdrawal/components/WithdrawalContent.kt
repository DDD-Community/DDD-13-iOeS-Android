package com.pickflow.android.feature.withdrawal.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pickflow.android.common.designsystem.PickflowColors
import com.pickflow.android.common.designsystem.PickflowTypography
import com.pickflow.android.feature.withdrawal.model.WithdrawalReason

/**
 * iOS `WithdrawalPreviewView` 1:1 이식 — 회원탈퇴 화면.
 *
 * navBar + (유의사항 박스 / 사유 선택 / 동의 체크 / 제출 버튼) 스크롤 영역.
 */
@Composable
fun WithdrawalContent(
    selectedReason: WithdrawalReason? = null,
    isDropdownOpen: Boolean = false,
    otherFeedback: String = "",
    didAgree: Boolean = false,
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
            .testTag("withdrawal-content"),
    ) {
        NavBar()

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 24.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            CautionBox()
            ReasonSection(
                selectedReason = selectedReason,
                isDropdownOpen = isDropdownOpen,
                otherFeedback = otherFeedback,
            )
            AgreementRow(didAgree = didAgree)
            SubmitButton(canSubmit = canSubmit)
        }
    }
}

@Composable
private fun NavBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(44.dp), contentAlignment = Alignment.Center) {
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
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(PickflowColors.gray80)
            .border(
                width = 1.dp,
                color = PickflowColors.sunsetOrange.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp),
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "탈퇴 전 꼭 확인해주세요",
            style = PickflowTypography.bodyMediumBold,
            color = PickflowColors.sunsetOrange,
        )
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            CautionRow("탈퇴 시 저장한 스팟, 활동 기록이 모두 삭제돼요.")
            CautionRow("삭제된 데이터는 복구할 수 없어요.")
            CautionRow("동일한 소셜 계정으로 재가입할 수 있어요.")
        }
    }
}

@Composable
private fun CautionRow(text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(text = "•", style = PickflowTypography.bodySmall, color = PickflowColors.gray40)
        Text(text = text, style = PickflowTypography.bodySmall, color = PickflowColors.gray40)
    }
}

@Composable
private fun ReasonSection(
    selectedReason: WithdrawalReason?,
    isDropdownOpen: Boolean,
    otherFeedback: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "어떤 점이 아쉬우셨나요?",
            style = PickflowTypography.headingSmall,
            color = PickflowColors.gray0,
        )
        WithdrawalReasonDropdown(selectedReason = selectedReason, isOpen = isDropdownOpen)

        if (selectedReason == WithdrawalReason.Other) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(PickflowColors.gray80)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                Text(
                    text = otherFeedback.ifEmpty { "의견을 자유롭게 남겨주세요" },
                    style = PickflowTypography.bodyMedium,
                    color = if (otherFeedback.isEmpty()) PickflowColors.gray50 else PickflowColors.gray0,
                )
            }
        }
    }
}

@Composable
private fun AgreementRow(didAgree: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
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
            text = "위 유의사항을 모두 확인했으며 동의합니다.",
            style = PickflowTypography.bodyMedium,
            color = PickflowColors.gray20,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun SubmitButton(canSubmit: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (canSubmit) PickflowColors.sunsetOrange else PickflowColors.gray70),
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
