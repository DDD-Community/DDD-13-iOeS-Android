package com.pickflow.android.feature.withdrawal.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.pickflow.android.common.designsystem.PickflowColors
import com.pickflow.android.common.designsystem.PickflowTypography
import com.pickflow.android.feature.withdrawal.model.WithdrawalReason

/**
 * iOS `WithdrawalReasonDropdown` 1:1 이식 — 탈퇴 사유 선택 드롭다운.
 *
 * 헤더(선택값/placeholder + chevron) + (열림 시) 7개 사유 행 목록.
 * 배경 gray90, corner 10, 행 사이 gray70 1px 구분선.
 */
@Composable
fun WithdrawalReasonDropdown(
    selectedReason: WithdrawalReason?,
    isOpen: Boolean,
    onToggle: () -> Unit = {},
    onSelect: (WithdrawalReason) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(PickflowColors.gray90),
    ) {
        // header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggle() }
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .testTag("withdrawal-dropdown-header"),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = selectedReason?.displayText ?: "탈퇴 사유를 선택해주세요",
                style = PickflowTypography.bodyMedium,
                color = if (selectedReason != null) PickflowColors.gray0 else PickflowColors.gray50,
            )
            Spacer(Modifier.weight(1f))
            Icon(
                imageVector = if (isOpen) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                tint = PickflowColors.gray40,
                modifier = Modifier.size(18.dp),
            )
        }

        if (isOpen) {
            Divider()
            WithdrawalReason.entries.forEachIndexed { index, reason ->
                ReasonRow(
                    reason = reason,
                    selected = reason == selectedReason,
                    onClick = { onSelect(reason) },
                )
                if (index != WithdrawalReason.entries.lastIndex) Divider()
            }
        }
    }
}

@Composable
private fun ReasonRow(
    reason: WithdrawalReason,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag("withdrawal-reason-${reason.name}"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = reason.displayText,
            style = PickflowTypography.bodyMedium,
            color = if (selected) PickflowColors.sunsetOrange else PickflowColors.gray0,
        )
        Spacer(Modifier.weight(1f))
        if (selected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = PickflowColors.sunsetOrange,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun Divider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(PickflowColors.gray70),
    )
}
