package com.pickflow.android.feature.devmode

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.window.DialogProperties
import com.pickflow.android.core.services.protocols.ApiEnvironment

/**
 * 환경 전환 확인 — 확인을 누르면 로그아웃하고 새 환경을 적용한다.
 *
 * 되돌릴 선택지가 없으므로 버튼은 확인 하나뿐이고, 바깥 터치/백버튼으로도 닫히지 않는다.
 * 실행 중인 화면들이 이전 서버 데이터를 들고 있어 재시작을 안내한다.
 */
@Composable
fun EnvironmentSwitchedDialog(
    environment: ApiEnvironment,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
        ),
        modifier = Modifier.testTag("devmode-restart-dialog"),
        title = { Text("앱을 다시 실행해 주세요") },
        text = {
            Text(
                "서버가 바뀌어 로그아웃했어요. 앱을 완전히 종료했다가 다시 열면 " +
                    "${environment.label} 으로 연결돼요.",
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                modifier = Modifier.testTag("devmode-restart-confirm"),
            ) { Text("확인") }
        },
    )
}
