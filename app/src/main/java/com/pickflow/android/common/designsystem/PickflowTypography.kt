package com.pickflow.android.common.designsystem

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.pickflow.android.R

/**
 * iOS `PickflowTypography` 1:1 매핑.
 *
 * Pretendard ttf 4 종을 `res/font/` 에 직접 합류 (iOS 가 ttf 를 번들하는 것과 동등).
 * Pretendard 1.3.9 (OFL 1.1) `public/static/alternative/` 정적 ttf 사용 — 시스템
 * 폰트 메트릭 정렬 버전으로 표준 ttf 와 시각 차이는 거의 없음. 네트워크 의존 제거.
 */
object PickflowTypography {

    private val PretendardFamily: FontFamily = FontFamily(
        Font(R.font.pretendard_regular, FontWeight.Normal, FontStyle.Normal),
        Font(R.font.pretendard_medium, FontWeight.Medium, FontStyle.Normal),
        Font(R.font.pretendard_semibold, FontWeight.SemiBold, FontStyle.Normal),
        Font(R.font.pretendard_bold, FontWeight.Bold, FontStyle.Normal),
    )

    private val family: FontFamily = PretendardFamily

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
    /** iOS `body(.small(.bold))` 1:1. */
    val bodySmallBold = style(13, 16.9, FontWeight.SemiBold)
    val labelMedium = style(13, 15.6, FontWeight.Medium)
    val labelSmall = style(12, 14.4, FontWeight.Medium, 0.2)
    val labelXSmall = style(11, 13.2, FontWeight.Medium, 0.2)
}
