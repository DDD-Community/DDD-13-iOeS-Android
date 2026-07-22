package com.pickflow.android.feature.archive.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pickflow.android.common.designsystem.PickflowColors
import com.pickflow.android.common.designsystem.PickflowTypography

private const val MAX_NAME_LENGTH = 15

/**
 * iOS `ArchiveRenameDialog` 1:1 — 보관함 이름 변경 모달 다이얼로그.
 *
 * 가운데 정렬, 최대 15자 입력, 우측 카운터, 취소/저장 2-버튼.
 */
@Composable
fun ArchiveRenameDialog(
    initialName: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    // 다이얼로그 기본 폭 제한을 풀어 딤 배경이 좌우/상하 풀스크린으로 깔리도록 한다.
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        ArchiveRenameDialogContent(
            initialName = initialName,
            onDismiss = onDismiss,
            onSave = onSave,
        )
    }
}

/** Paparazzi 스냅샷용 stateless 본체. */
@Composable
fun ArchiveRenameDialogContent(
    initialName: String = "나의 보관함",
    onDismiss: () -> Unit = {},
    onSave: (String) -> Unit = {},
) {
    var name by remember(initialName) { mutableStateOf(initialName) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            // 콘텐츠가 풀스크린을 덮으므로 딤 영역 탭으로도 닫히게 한다.
            .clickable(onClick = onDismiss)
            // 키보드가 올라오면 남은 영역 기준으로 카드를 중앙 정렬(딤은 풀스크린 유지).
            .imePadding()
            .testTag("archive-rename-dialog"),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                // 카드 내부 탭이 딤 dismiss로 전파되지 않도록 소비.
                .clickable(enabled = false) {}
                .clip(RoundedCornerShape(16.dp))
                .background(PickflowColors.gray90)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "보관함 이름 변경",
                style = PickflowTypography.bodyLargeBold,
                color = PickflowColors.gray0,
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(PickflowColors.gray80)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    BasicTextField(
                        value = name,
                        onValueChange = { new ->
                            name = if (new.length > MAX_NAME_LENGTH) new.take(MAX_NAME_LENGTH) else new
                        },
                        textStyle = PickflowTypography.bodyMedium.copy(color = PickflowColors.gray0),
                        cursorBrush = SolidColor(PickflowColors.gray0),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("archive-rename-field"),
                        singleLine = true,
                    )
                    if (name.isEmpty()) {
                        Text(
                            text = "보관함 이름",
                            style = PickflowTypography.bodyMedium,
                            color = PickflowColors.gray50,
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "${name.length}/$MAX_NAME_LENGTH",
                    style = PickflowTypography.bodySmall,
                    color = PickflowColors.gray50,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                DialogButton(
                    label = "취소",
                    background = PickflowColors.gray0,
                    foreground = PickflowColors.gray95,
                    enabled = true,
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).testTag("archive-rename-cancel"),
                )
                // 이름을 실제로 변경했을 때만 저장 활성화(sunsetOrange), 아니면 gray70 비활성.
                val enabled = name.trim().isNotEmpty() && name != initialName
                DialogButton(
                    label = "저장",
                    background = if (enabled) PickflowColors.sunsetOrange else PickflowColors.gray70,
                    foreground = if (enabled) PickflowColors.gray0 else PickflowColors.gray50,
                    enabled = enabled,
                    onClick = { onSave(name); onDismiss() },
                    modifier = Modifier.weight(1f).testTag("archive-rename-save"),
                )
            }
        }
    }
}

@Composable
private fun DialogButton(
    label: String,
    background: Color,
    foreground: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            // 비활성 색은 호출 측에서 명시적으로 전달한다(디자인 시안의 solid gray70).
            .background(background)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = label, style = PickflowTypography.bodyMediumBold, color = foreground)
    }
}
