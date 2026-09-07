package com.pickflow.android.feature.onboarding.components

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.pickflow.android.feature.onboarding.model.OnboardingPageContent

/**
 * 페이지별 일러스트. 네 페이지 모두 그라데이션 배경까지 합쳐진 단일 이미지 한 장이다.
 *
 * 일러스트 영역 비율이 원본(390x500)보다 납작한 기기에서 Crop 이 중앙 기준으로
 * 위아래를 함께 잘라 폰 목업 하단이 날아갔다. 잘림을 전부 상단(그라데이션뿐)으로
 * 몰아 하단 라인을 보존한다.
 */
@Composable
fun OnboardingIllustration(
    page: OnboardingPageContent,
    modifier: Modifier = Modifier,
) {
    Image(
        painter = painterResource(page.illustration),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        alignment = Alignment.BottomCenter,
        modifier = modifier.clipToBounds(),
    )
}
