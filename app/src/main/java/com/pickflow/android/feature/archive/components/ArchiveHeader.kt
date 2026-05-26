package com.pickflow.android.feature.archive.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.painter.BitmapPainter
import coil.compose.AsyncImage
import com.pickflow.android.common.designsystem.PickflowColors

/**
 * iOS `ArchiveHeaderView` 1:1 — 보관함 상단 커버 사진 영역.
 *
 * coverImageBytes 가 있으면 그것을 우선 사용하고, 없으면 thumbnailUrl 을 표시.
 * 하단에서 위쪽으로 검정 65% → 투명 그라데이션을 덮어 라지 타이틀 가독성 확보.
 */
@Composable
fun ArchiveHeader(
    thumbnailUrl: String? = null,
    coverImageBytes: ByteArray? = null,
    height: Dp = 240.dp,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(PickflowColors.gray90),
    ) {
        val bitmap: ImageBitmap? = remember(coverImageBytes) {
            coverImageBytes?.let {
                runCatching { BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap() }
                    .getOrNull()
            }
        }
        when {
            bitmap != null -> Image(
                painter = BitmapPainter(bitmap),
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().height(height),
                contentScale = ContentScale.Crop,
            )
            !thumbnailUrl.isNullOrBlank() -> AsyncImage(
                model = thumbnailUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().height(height),
                contentScale = ContentScale.Crop,
            )
        }
        // bottom → top 검정 그라데이션. iOS endPoint(0.5, 0.35) ≈ 상단 35% 지점부터 투명.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .background(
                    Brush.verticalGradient(
                        0.0f to Color.Transparent,
                        0.65f to Color.Transparent,
                        1.0f to Color.Black.copy(alpha = 0.65f),
                    ),
                ),
        )
    }
}
