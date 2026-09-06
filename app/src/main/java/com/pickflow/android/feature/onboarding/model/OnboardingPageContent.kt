package com.pickflow.android.feature.onboarding.model

import androidx.annotation.DrawableRes
import com.pickflow.android.R

/**
 * 일러스트 배치 방식.
 *
 * - [TOP_ALIGNED_IMAGE]: 그라데이션 배경 위에 이미지를 상단 정렬(0페이지).
 * - [FULL_IMAGE]: 배경까지 포함된 단일 이미지를 영역 전체에 채움(1~3페이지).
 */
enum class OnboardingLayout { TOP_ALIGNED_IMAGE, FULL_IMAGE }

data class OnboardingPageContent(
    val id: Int,
    val title: String,
    /** 타이틀 안에서 accent 색으로 강조할 구간들. 등장 순서대로 첫 매치를 칠한다. */
    val titleHighlights: List<String>,
    val subtitle: String,
    @DrawableRes val illustration: Int,
    val layout: OnboardingLayout,
)

val defaultOnboardingPages: List<OnboardingPageContent> = listOf(
    OnboardingPageContent(
        id = 0,
        title = "흩어진 포토스팟,\n이제 한 번에 찾을 수 있어요",
        titleHighlights = listOf("한 번에 찾을 수 있어요"),
        subtitle = "지도 뷰와 리스트 뷰를 통해\n원하는 방식으로 스팟을 쉽게 탐색해요.",
        illustration = R.drawable.onboarding_0,
        layout = OnboardingLayout.TOP_ALIGNED_IMAGE,
    ),
    OnboardingPageContent(
        id = 1,
        title = "나만의 스팟을\n기록하고, 오픈해보세요",
        titleHighlights = listOf("기록하고,", "오픈"),
        subtitle = "내가 촬영한 스팟을 지도에 남기고,\n기록한 스팟을 공개해보세요.",
        illustration = R.drawable.onboarding_1,
        layout = OnboardingLayout.FULL_IMAGE,
    ),
    OnboardingPageContent(
        id = 2,
        title = "다른 사람의 스팟도\n만나볼 수 있어요",
        titleHighlights = listOf("다른 사람의 스팟"),
        subtitle = "다른 유저가 발견한 스팟을 살펴보고,\n마음에 드는 스팟을 추천해보세요.",
        illustration = R.drawable.onboarding_2,
        layout = OnboardingLayout.FULL_IMAGE,
    ),
    OnboardingPageContent(
        id = 3,
        title = "원하는 순간의 스팟을\n찾아보세요",
        titleHighlights = listOf("원하는 순간"),
        subtitle = "햇살부터 윤슬, 노을, 야경까지\n지금 찍고 싶은 분위기에 맞는 스팟을 찾아보세요.",
        illustration = R.drawable.onboarding_3,
        layout = OnboardingLayout.FULL_IMAGE,
    ),
)
