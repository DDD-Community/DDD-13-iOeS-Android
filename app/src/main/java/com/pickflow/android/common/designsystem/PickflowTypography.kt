package com.pickflow.android.common.designsystem

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * iOS PickflowTypography 1:1 매핑.
 * Pretendard ttf 미반영 — 현재 FontFamily.Default 사용(폰트 에셋 합류 시 교체).
 */
object PickflowTypography {
    private val family = FontFamily.Default

    private fun style(size: Int, lineHeight: Double, weight: FontWeight, letter: Double = 0.0) =
        TextStyle(
            fontFamily = family,
            fontSize = size.sp,
            lineHeight = lineHeight.sp,
            fontWeight = weight,
            letterSpacing = (letter / size).em,
            lineHeightStyle = LineHeightStyle(
                alignment = LineHeightStyle.Alignment.Center,
                trim = LineHeightStyle.Trim.None,
            ),
        )

    val displayLarge = style(34, 40.8, FontWeight.Bold, -0.2)
    val displayMedium = style(28, 33.6, FontWeight.Bold, -0.2)
    val headingLarge = style(24, 28.8, FontWeight.SemiBold)
    val headingMedium = style(22, 26.4, FontWeight.SemiBold)
    val headingSmall = style(19, 22.8, FontWeight.SemiBold)
    val bodyLarge = style(17, 23.8, FontWeight.Normal)
    val bodyLargeBold = style(17, 23.8, FontWeight.SemiBold)
    val bodyMedium = style(15, 21.0, FontWeight.Normal)
    val bodyMediumBold = style(15, 21.0, FontWeight.SemiBold)
    val bodySmall = style(13, 16.9, FontWeight.Normal)
    val labelMedium = style(13, 15.6, FontWeight.Medium)
    val labelSmall = style(12, 14.4, FontWeight.Medium, 0.2)
    val labelXSmall = style(11, 13.2, FontWeight.Medium, 0.2)
}
