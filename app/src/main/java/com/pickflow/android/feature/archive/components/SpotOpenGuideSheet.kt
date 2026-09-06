package com.pickflow.android.feature.archive.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pickflow.android.common.designsystem.PickflowColors
import com.pickflow.android.common.designsystem.PickflowTypography

/**
 * "스팟 공개 OPEN!" 안내 바텀시트 — 보관함 > 나만의 스팟 탭 최초 1회.
 *
 * TODO(PV-79): **내용은 임시다.** Figma `1201-9416` / `1201-9439` 를 아직 못 읽어
 * 시트 골격(컨테이너·확인 버튼·dismiss 동작)만 맞춰 뒀다. 디자인이 들어오면 이 파일의
 * 본문만 교체하면 되고, 노출 판정([com.pickflow.android.feature.archive.SpotOpenGuideViewModel])과
 * 마운트 지점은 그대로 간다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpotOpenGuideSheet(
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        // 확인으로만 닫는 팝업과 달리 바텀시트는 바깥 탭·스와이프 dismiss 를 막지 않는다.
        // 어느 쪽으로 닫든 "봤다" 로 친다 — 안내를 무한 반복하지 않기 위해서다.
        onDismissRequest = onConfirm,
        sheetState = sheetState,
        containerColor = PickflowColors.spotCardBackground,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        dragHandle = null,
        modifier = modifier.testTag("spot-open-guide-sheet"),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 32.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "스팟 공개 OPEN!",
                style = PickflowTypography.headingMedium,
                color = PickflowColors.gray0,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "이제 내가 기록한 스팟을 다른 사람에게 공개할 수 있어요.",
                style = PickflowTypography.bodyMedium,
                color = PickflowColors.gray20,
                textAlign = TextAlign.Center,
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .height(52.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(PickflowColors.sunsetOrange)
                    .clickable(onClick = onConfirm)
                    .testTag("spot-open-guide-confirm"),
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
}
