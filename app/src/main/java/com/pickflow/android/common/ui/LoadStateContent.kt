package com.pickflow.android.common.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pickflow.android.common.designsystem.PickflowColors
import com.pickflow.android.common.designsystem.PickflowTypography

/**
 * LoadState 5-상태(Idle/Loading/Loaded/Empty/Failed)를 일관된 UI로 렌더.
 * Loaded일 때만 [loaded] 슬롯을 호출한다.
 */
@Composable
fun <T> LoadStateContent(
    state: LoadState<T>,
    modifier: Modifier = Modifier,
    emptyMessage: String = "표시할 내용이 없어요.",
    onRetry: (() -> Unit)? = null,
    loaded: @Composable (T) -> Unit,
) {
    when (state) {
        is LoadState.Idle -> Unit
        is LoadState.Loading -> CenterColumn(modifier, tag = "state-loading") {
            CircularProgressIndicator(color = PickflowColors.sunsetOrange)
        }
        is LoadState.Empty -> CenterColumn(modifier, tag = "state-empty") {
            Text(
                text = emptyMessage,
                style = PickflowTypography.bodyMedium,
                color = PickflowColors.gray40,
                textAlign = TextAlign.Center,
            )
        }
        is LoadState.Failed -> CenterColumn(modifier, tag = "state-failed") {
            Text(
                text = "문제가 발생했어요.\n${state.error.message ?: ""}",
                style = PickflowTypography.bodyMedium,
                color = PickflowColors.gray40,
                textAlign = TextAlign.Center,
            )
            if (onRetry != null) {
                Button(
                    onClick = onRetry,
                    modifier = Modifier.padding(top = 16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PickflowColors.sunsetOrange,
                        contentColor = PickflowColors.gray0,
                    ),
                ) { Text("다시 시도") }
            }
        }
        is LoadState.Loaded -> loaded(state.value)
    }
}

@Composable
private fun CenterColumn(
    modifier: Modifier,
    tag: String,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag(tag),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) { content() }
}
