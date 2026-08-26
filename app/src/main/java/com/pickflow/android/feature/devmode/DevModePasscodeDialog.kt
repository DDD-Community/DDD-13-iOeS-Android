package com.pickflow.android.feature.devmode

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions

/** Dev Mode 진입 코드 알럿. 코드가 맞을 때만 [onUnlocked] 을 호출한다. */
@Composable
fun DevModePasscodeDialog(
    onDismiss: () -> Unit,
    onUnlocked: () -> Unit,
) {
    var code by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("devmode-passcode-dialog"),
        title = { Text("코드를 입력해 주세요") },
        text = {
            OutlinedTextField(
                value = code,
                onValueChange = { code = it },
                singleLine = true,
                placeholder = { Text("코드") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.testTag("devmode-passcode-field"),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (code == DEV_MODE_PASSCODE) onUnlocked() else onDismiss() },
                modifier = Modifier.testTag("devmode-passcode-confirm"),
            ) { Text("확인") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("취소") }
        },
    )
}
