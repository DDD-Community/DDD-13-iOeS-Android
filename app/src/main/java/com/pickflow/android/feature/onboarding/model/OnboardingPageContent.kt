package com.pickflow.android.feature.onboarding.model

import androidx.compose.ui.graphics.Color
import com.pickflow.android.common.designsystem.PickflowColors

/** iOS `OnboardingPage.AccentTheme` 1:1. */
enum class OnboardingAccentTheme { ORANGE, BLUE }

/** iOS `OnboardingPage.Layout` 1:1. */
enum class OnboardingLayout { TOP_ALIGNED_IMAGE, BOTTOM_ALIGNED_IMAGE, MOOD_CAROUSEL }

/** iOS `MoodFilter` 1:1 — 라벨/아이콘/스트로크 색. 아이콘은 커스텀 에셋이라 이모지 placeholder. */
enum class OnboardingMoodKind(val label: String, val emoji: String, val borderColor: Color) {
    SUNSET(label = "노을", emoji = "🌅", borderColor = PickflowColors.sunsetOrange),
    RIPPLE(label = "윤슬", emoji = "🌊", borderColor = Color(0xFF1E8AF6)),
}

/** iOS `OnboardingMoodHeader` 1:1. */
data class OnboardingMood(
    val primary: OnboardingMoodKind,
    val secondary: OnboardingMoodKind,
    val description: String,
)

/** iOS `OnboardingPageGradient.Stop` 1:1. */
data class OnboardingGradientStop(val color: Color, val location: Float)

/** iOS `OnboardingPage` 1:1. 이미지 에셋명은 placeholder 처리되므로 보관하지 않는다. */
data class OnboardingPageContent(
    val id: Int,
    val title: String,
    val titleHighlight: String?,
    val subtitle: String,
    val theme: OnboardingAccentTheme,
    val gradientStops: List<OnboardingGradientStop>,
    val carouselImageCount: Int,
    val mood: OnboardingMood?,
    val layout: OnboardingLayout,
)

// iOS OnboardingPageGradient 정의 1:1.
private val orangeWarm = listOf(
    OnboardingGradientStop(PickflowColors.sunsetOrange, 0f),
    OnboardingGradientStop(Color(0xFFF69648), 1f),
)
private val nightWarm = listOf(
    OnboardingGradientStop(PickflowColors.gray95, 0f),
    OnboardingGradientStop(PickflowColors.sunsetOrange.copy(alpha = 0.5f), 1f),
)
private val nightCool = listOf(
    OnboardingGradientStop(PickflowColors.gray95, 0f),
    OnboardingGradientStop(Color(0xFF1E8AF6).copy(alpha = 0.5f), 1f),
)

/** iOS `OnboardingPage.defaultPages` 1:1. */
val defaultOnboardingPages: List<OnboardingPageContent> = listOf(
    OnboardingPageContent(
        id = 0,
        title = "흩어진 포토스팟,\n이제 한 번에 찾을 수 있어요",
        titleHighlight = "한 번에 찾을 수 있어요",
        subtitle = "지도 뷰와 리스트 뷰를 통해\n원하는 방식으로 스팟을 쉽게 탐색해요.",
        theme = OnboardingAccentTheme.ORANGE,
        gradientStops = orangeWarm,
        carouselImageCount = 0,
        mood = null,
        layout = OnboardingLayout.TOP_ALIGNED_IMAGE,
    ),
    OnboardingPageContent(
        id = 1,
        title = "나만의 스팟을\n기록하고 공유해보세요",
        titleHighlight = "나만의 스팟을",
        subtitle = "내가 촬영한 스팟을 지도에 남기고\n확인할 수 있어요.",
        theme = OnboardingAccentTheme.ORANGE,
        gradientStops = orangeWarm,
        carouselImageCount = 0,
        mood = null,
        layout = OnboardingLayout.BOTTOM_ALIGNED_IMAGE,
    ),
    OnboardingPageContent(
        id = 2,
        title = "하루의 끝자락에서,\n노을이 가장 아름다운 순간",
        titleHighlight = "노을이 가장 아름다운 순간",
        subtitle = "노을 태그로,\n그 순간을 담은 스팟을 찾아보세요.",
        theme = OnboardingAccentTheme.ORANGE,
        gradientStops = nightWarm,
        carouselImageCount = 3,
        mood = OnboardingMood(
            primary = OnboardingMoodKind.SUNSET,
            secondary = OnboardingMoodKind.RIPPLE,
            description = "해가 뜨거나 지려고 할 때에\n하늘이 햇빛을 받아 붉게 보이는 현상",
        ),
        layout = OnboardingLayout.MOOD_CAROUSEL,
    ),
    OnboardingPageContent(
        id = 3,
        title = "물가에 빛이 닿을 때,\n윤슬이 가장 반짝이는 순간",
        titleHighlight = "윤슬이 가장 반짝이는 순간",
        subtitle = "윤슬 태그로,\n그 순간을 담은 스팟을 찾아보세요.",
        theme = OnboardingAccentTheme.BLUE,
        gradientStops = nightCool,
        carouselImageCount = 3,
        mood = OnboardingMood(
            primary = OnboardingMoodKind.RIPPLE,
            secondary = OnboardingMoodKind.SUNSET,
            description = "달빛이나 햇빛에 비치어 반짝이는 잔물결\n",
        ),
        layout = OnboardingLayout.MOOD_CAROUSEL,
    ),
)
