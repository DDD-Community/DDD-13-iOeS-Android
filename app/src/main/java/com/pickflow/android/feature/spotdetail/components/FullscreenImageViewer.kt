package com.pickflow.android.feature.spotdetail.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

/**
 * 원본 이미지 풀스크린 뷰어 — 핀치투줌 + 더블탭 + 우상단 close.
 *
 * - 핀치(`detectTransformGestures`)로 1x~5x 스케일, 같이 들어온 pan으로 위치 이동
 * - 더블탭(`detectTapGestures`)으로 1x ↔ 2.5x 토글 (애니메이션)
 * - 닫기는 우상단 `Close` 아이콘 또는 시스템 Back
 */
@Composable
fun FullscreenImageViewer(
    imageUrl: String,
    contentDescription: String?,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        val scale = remember { Animatable(1f) }
        val offsetX = remember { Animatable(0f) }
        val offsetY = remember { Animatable(0f) }
        val scope = rememberCoroutineScope()

        // 스케일이 1로 돌아오면 위치도 초기화.
        LaunchedEffect(scale.value) {
            if (scale.value <= 1f) {
                offsetX.snapTo(0f)
                offsetY.snapTo(0f)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .testTag("fullscreen-image-viewer"),
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(
                model = imageUrl,
                contentDescription = contentDescription,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale.value,
                        scaleY = scale.value,
                        translationX = offsetX.value,
                        translationY = offsetY.value,
                    )
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scope.launch {
                                val next = (scale.value * zoom).coerceIn(1f, 5f)
                                scale.snapTo(next)
                                if (next > 1f) {
                                    offsetX.snapTo(offsetX.value + pan.x)
                                    offsetY.snapTo(offsetY.value + pan.y)
                                }
                            }
                        }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = {
                                scope.launch {
                                    val target = if (scale.value > 1f) 1f else 2.5f
                                    scale.animateTo(target, tween(200))
                                    if (target == 1f) {
                                        offsetX.animateTo(0f, tween(200))
                                        offsetY.animateTo(0f, tween(200))
                                    }
                                }
                            },
                        )
                    },
            )

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(8.dp)
                    .testTag("fullscreen-image-close"),
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "닫기",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp),
                )
            }
        }
    }
}

/** dummy reference to avoid unused-import warnings if all gesture utilities collapse. */
@Suppress("unused")
private val gestureCenterAnchor: Offset = Offset.Zero
