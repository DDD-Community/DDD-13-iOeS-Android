package com.pickflow.android.feature.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import androidx.compose.ui.graphics.toArgb
import com.naver.maps.map.overlay.OverlayImage
import com.pickflow.android.common.designsystem.PickflowColors

/**
 * 지도 마커 아이콘 생성기.
 *
 * iOS는 SwiftUI 핀 뷰(`ClusterPinView`/`SpotMarkerView`)를 `ImageRenderer`로
 * UIImage 화하지만, Android에서 단순 도형 핀은 Canvas 직접 드로잉이 lifecycle
 * 의존성 없이 견고하다. 치수·색은 스냅샷 검증된 Compose 핀 컴포넌트 스펙을
 * 그대로 미러링한다(원형 컴포넌트가 source of truth).
 */
object MapMarkerIcons {

    /** iOS `ClusterPinView` sunsetOrange — Canvas Paint 용 ARGB Int. */
    private val SUNSET_ORANGE = PickflowColors.sunsetOrange.toArgb()

    /** iOS `ClusterPinView.diameter(forCount:)` 1:1. */
    fun clusterDiameterDp(count: Int): Int = when {
        count < 10 -> 44
        count < 50 -> 54
        count < 100 -> 64
        else -> 74
    }

    /** 개수 ≥ 2 — sunsetOrange 원 + 흰 개수 텍스트. */
    fun clusterIcon(context: Context, count: Int): OverlayImage =
        OverlayImage.fromBitmap(drawCluster(context, count))

    /** 개수 == 1 — 검정 그라데이션 원 + 사진 글리프. */
    fun spotIcon(context: Context): OverlayImage =
        OverlayImage.fromBitmap(drawSpot(context))

    private fun drawCluster(context: Context, count: Int): Bitmap {
        val density = context.resources.displayMetrics.density
        val size = (clusterDiameterDp(count) * density).toInt().coerceAtLeast(1)
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val r = size / 2f
        canvas.drawCircle(r, r, r, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = SUNSET_ORANGE })
        val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
            textSize = size * 0.34f
        }
        val baseline = r - (text.descent() + text.ascent()) / 2f
        canvas.drawText(count.toString(), r, baseline, text)
        return bmp
    }

    private fun drawSpot(context: Context): Bitmap {
        val density = context.resources.displayMetrics.density
        val size = (44 * density).toInt().coerceAtLeast(1)
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val r = size / 2f
        val circle = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, 0f, size.toFloat(),
                Color.TRANSPARENT,
                0xB3000000.toInt(), // 검정 alpha 0.7
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawCircle(r, r, r, circle)
        val glyph = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            textSize = size * 0.42f
        }
        val baseline = r - (glyph.descent() + glyph.ascent()) / 2f
        canvas.drawText("🖼", r, baseline, glyph) // 🖼 — iOS icPhoto 치환
        return bmp
    }
}
