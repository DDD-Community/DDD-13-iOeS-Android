package com.pickflow.android.feature.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Shader
import android.graphics.Typeface
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.naver.maps.map.overlay.OverlayImage
import com.pickflow.android.R
import com.pickflow.android.common.designsystem.PickflowColors

/**
 * 지도 마커 아이콘 생성기.
 *
 * iOS 는 SwiftUI 핀 뷰(`ClusterPinView`/`SpotMarkerView`/`MyClusterPinView`)를
 * `ImageRenderer` 로 UIImage 화한다. Android 도 동일 도형이지만 Compose 를 Bitmap
 * 으로 굽는 건 lifecycle/Owner 의존성 때문에 까다로워, 핀의 시각 스펙을 그대로
 * Canvas 로 직접 드로잉한다. 원본 Compose 핀 컴포넌트가 source of truth.
 */
object MapMarkerIcons {

    private val SUNSET_ORANGE = PickflowColors.sunsetOrange.toArgb()

    /**
     * 클러스터 마커 지름(dp).
     * - 2 ~ 15개: 60dp
     * - 16 ~ 999+: 100dp
     */
    fun clusterDiameterDp(count: Int): Int = if (count <= 15) 60 else 100

    /** 큐레이션 클러스터 — sunsetOrange 원 + 흰 개수 텍스트. */
    fun clusterIcon(context: Context, count: Int, isSelected: Boolean = false): OverlayImage =
        OverlayImage.fromBitmap(drawCluster(context, count, isSelected))

    /** 큐레이션 단일 leaf — 검정 그라데이션 원 + 사진 글리프. */
    fun spotIcon(context: Context, isSelected: Boolean = false): OverlayImage =
        OverlayImage.fromBitmap(drawSpot(context, isSelected))

    /** 큐레이션 단일 leaf — viewport 응답 imageUrl 사진을 원형으로 적용한 마커. */
    fun spotPhotoIcon(context: Context, photo: Bitmap, isSelected: Boolean = false): OverlayImage =
        OverlayImage.fromBitmap(drawSpotPhoto(context, photo, isSelected))

    /** 마이스팟 단일 마커 — 56dp 흰 배경 + 검정 그라데이션 + 사진 + "MY". */
    fun mySpotIcon(context: Context, isSelected: Boolean = false): OverlayImage =
        OverlayImage.fromBitmap(drawMySpot(context, isSelected))

    private fun drawCluster(context: Context, count: Int, isSelected: Boolean): Bitmap {
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
            // 클러스터 크기와 무관하게 글씨 크기는 고정(16dp).
            textSize = 16f * density
        }
        val label = if (count > 999) "999+" else count.toString()
        val baseline = r - (text.descent() + text.ascent()) / 2f
        canvas.drawText(label, r, baseline, text)
        if (isSelected) drawSelectionStroke(canvas, size, density)
        return bmp
    }

    private fun drawSpot(context: Context, isSelected: Boolean): Bitmap {
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
        // ic_photo 20dp — iOS SpotMarkerView 의 `Image(.icPhoto).frame(20×20)` 동일.
        drawPhotoIcon(context, canvas, centerX = r, centerY = r, sizeDp = 20, density = density, alpha = 255)
        if (isSelected) drawSelectionStroke(canvas, size, density)
        return bmp
    }

    private fun drawMySpot(context: Context, isSelected: Boolean): Bitmap {
        val density = context.resources.displayMetrics.density
        val size = (56 * density).toInt().coerceAtLeast(1)
        // shadow blur(4dp) 를 위한 외곽 여유 — 좌우/상하 4dp 씩 + y offset 4dp.
        val pad = (4 * density).toInt()
        val totalW = size + pad * 2
        val totalH = size + pad * 2
        val bmp = Bitmap.createBitmap(totalW, totalH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val cx = totalW / 2f
        val cy = totalH / 2f
        val r = size / 2f

        // drop shadow — 검정 alpha 0.2, blur 4dp, offset y +4dp.
        val shadow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x33000000
            maskFilter = BlurMaskFilter(4f * density, BlurMaskFilter.Blur.NORMAL)
        }
        canvas.drawCircle(cx, cy + (4 * density), r, shadow)

        // 흰 원 + 검정 0.2 오버레이.
        canvas.drawCircle(cx, cy, r, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE })
        canvas.drawCircle(cx, cy, r, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x33000000 })

        // 검정 수직 그라데이션 (30% transparent → 100% black 0.7).
        val gradient = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, cy - r, 0f, cy + r,
                intArrayOf(Color.TRANSPARENT, Color.TRANSPARENT, 0xB3000000.toInt()),
                floatArrayOf(0f, 0.30f, 1f),
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawCircle(cx, cy, r, gradient)

        // ic_photo 18dp + alpha 0.5 — iOS MyClusterPinView 의 `Image(.icPhoto).frame(18×18).opacity(0.5)` 동일.
        // iOS 는 위쪽 padding(10) 후 하단에 "MY" 오버레이라 사진이 약간 위쪽으로 치우침.
        drawPhotoIcon(
            context,
            canvas,
            centerX = cx,
            centerY = cy - 8f * density,
            sizeDp = 18,
            density = density,
            alpha = 128,
        )

        val my = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
            textSize = size * 0.22f
        }
        val myBaseline = cy + (12 * density) - (my.descent() + my.ascent()) / 2f
        canvas.drawText("MY", cx, myBaseline, my)

        if (isSelected) {
            val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 4f * density
                color = SUNSET_ORANGE
            }
            canvas.drawCircle(cx, cy, r - stroke.strokeWidth / 2f, stroke)
        }
        return bmp
    }

    /** imageUrl 사진을 원형으로 center-crop 한 leaf 마커. 선택 시 sunsetOrange stroke(4dp). */
    private fun drawSpotPhoto(context: Context, photo: Bitmap, isSelected: Boolean): Bitmap {
        val density = context.resources.displayMetrics.density
        val size = (48 * density).toInt().coerceAtLeast(1)
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val r = size / 2f

        // 원형 클립 후 center-crop 사진 그리기.
        val clip = android.graphics.Path().apply {
            addCircle(r, r, r - 1f * density, android.graphics.Path.Direction.CW)
        }
        canvas.save()
        canvas.clipPath(clip)
        val srcSize = minOf(photo.width, photo.height)
        val srcLeft = (photo.width - srcSize) / 2
        val srcTop = (photo.height - srcSize) / 2
        val src = Rect(srcLeft, srcTop, srcLeft + srcSize, srcTop + srcSize)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(photo, src, Rect(0, 0, size, size), paint)
        canvas.restore()

        // 흰 링(2dp) — 어두운 지도 배경과 분리.
        val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2f * density
            color = Color.WHITE
        }
        canvas.drawCircle(r, r, r - ring.strokeWidth / 2f, ring)

        if (isSelected) drawSelectionStroke(canvas, size, density)
        return bmp
    }

    /** ic_photo drawable 을 Canvas 에 px 크기로 중앙 정렬 그리기. */
    private fun drawPhotoIcon(
        context: Context,
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        sizeDp: Int,
        density: Float,
        alpha: Int,
    ) {
        val drawable = ContextCompat.getDrawable(context, R.drawable.ic_photo) ?: return
        val px = (sizeDp * density).toInt().coerceAtLeast(1)
        val bmp = drawable.toBitmap(px, px)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            this.alpha = alpha
        }
        val left = centerX - px / 2f
        val top = centerY - px / 2f
        canvas.drawBitmap(bmp, null, Rect(left.toInt(), top.toInt(), (left + px).toInt(), (top + px).toInt()), paint)
    }

    private fun drawSelectionStroke(canvas: Canvas, size: Int, density: Float) {
        val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 4f * density
            color = SUNSET_ORANGE
        }
        val r = size / 2f
        canvas.drawCircle(r, r, r - stroke.strokeWidth / 2f, stroke)
    }
}
