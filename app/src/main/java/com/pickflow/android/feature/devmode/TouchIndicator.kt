package com.pickflow.android.feature.devmode

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pickflow.android.common.designsystem.PickflowColors

/**
 * Dev Mode "터치 표시하기" — 손가락이 닿은 자리에 원을 그린다. 화면 녹화에도 함께 찍힌다.
 *
 * 터치는 Initial 패스에서 **관찰만** 하고 소비하지 않으므로 아래 UI 동작에는 영향이 없다.
 *
 * ponytail: Activity 창에만 그려져 Dialog/시스템 팝업 위에는 안 찍힌다.
 * 그것까지 필요하면 SYSTEM_ALERT_WINDOW 오버레이로 올려야 한다.
 */
@Composable
fun TouchIndicator(
    modifier: Modifier = Modifier,
    viewModel: DevModeViewModel = hiltViewModel(),
    content: @Composable () -> Unit,
) {
    val enabled by viewModel.touchIndicatorEnabled.collectAsStateWithLifecycle()
    if (!enabled) {
        content()
        return
    }

    val touches = remember { mutableStateMapOf<PointerId, Offset>() }
    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent(PointerEventPass.Initial).changes.forEach { change ->
                            if (change.pressed) {
                                touches[change.id] = change.position
                            } else {
                                touches.remove(change.id)
                            }
                        }
                    }
                }
            }
            .drawWithContent {
                drawContent()
                val radius = 22.dp.toPx()
                touches.values.forEach { position ->
                    drawCircle(
                        color = PickflowColors.sunsetOrange.copy(alpha = 0.35f),
                        radius = radius,
                        center = position,
                    )
                    drawCircle(
                        color = PickflowColors.sunsetOrange,
                        radius = radius,
                        center = position,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx()),
                    )
                }
            },
    ) {
        content()
    }
}
