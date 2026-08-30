package com.pickflow.android.feature.map.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pickflow.android.common.designsystem.PickflowColors
import com.pickflow.android.common.designsystem.PickflowTypography
import com.pickflow.android.feature.map.Region

/**
 * 지역 선택 바텀시트.
 *
 * 선택은 [적용하기] 를 눌러야 확정된다. 취소·바깥 탭·드래그 dismiss 는 전부 [onDismiss] 로
 * 흘러 변경이 버려진다. pending 상태를 시트 안에 `remember` 로 두므로 재호출 시 자동으로
 * 현재 적용 중인 [applied] 가 선택된 상태로 다시 뜬다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegionPickerSheet(
    applied: Region,
    onApply: (Region) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = PickflowColors.gray95,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
    ) {
        RegionPickerContent(applied = applied, onApply = onApply, onCancel = onDismiss)
    }
}

/** 시트 본문 — 모달 창 없이도 렌더/테스트할 수 있도록 분리. */
@Composable
internal fun RegionPickerContent(
    applied: Region,
    onApply: (Region) -> Unit,
    onCancel: () -> Unit,
) {
    var pending by remember { mutableStateOf(applied) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp)
            .testTag("region-picker"),
    ) {
        Text(
            text = "어느 지역을 둘러볼까요?",
            style = PickflowTypography.headingSmall,
            color = PickflowColors.gray0,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "선택한 지역을 기준으로 스팟을 보여드려요.",
            style = PickflowTypography.bodyMedium,
            color = PickflowColors.gray40,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(24.dp))

        Region.entries.forEach { region ->
            RegionRow(
                region = region,
                selected = region == pending,
                onClick = { pending = region },
            )
            Spacer(Modifier.height(12.dp))
        }

        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SheetButton(
                text = "취소",
                background = PickflowColors.gray0,
                textColor = PickflowColors.gray95,
                onClick = onCancel,
                modifier = Modifier
                    .weight(1f)
                    .testTag("region-picker-cancel"),
            )
            SheetButton(
                text = "적용하기",
                background = PickflowColors.sunsetOrange,
                textColor = PickflowColors.gray0,
                onClick = { onApply(pending) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("region-picker-apply"),
            )
        }
    }
}

@Composable
private fun RegionRow(region: Region, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .clip(shape)
            .background(PickflowColors.gray90)
            .border(1.dp, if (selected) PickflowColors.sunsetOrange else Color.Transparent, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = region.displayName,
            style = PickflowTypography.bodyLargeBold,
            color = if (selected) PickflowColors.sunsetOrange else PickflowColors.gray0,
            modifier = Modifier.weight(1f),
        )
        if (selected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = "선택됨",
                tint = PickflowColors.sunsetOrange,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun SheetButton(
    text: String,
    background: Color,
    textColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(background)
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = text, style = PickflowTypography.bodyLargeBold, color = textColor)
    }
}
