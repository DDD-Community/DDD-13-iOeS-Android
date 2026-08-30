package com.pickflow.android.feature.onboarding.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.pickflow.android.common.designsystem.PickflowColors
import com.pickflow.android.feature.onboarding.model.OnboardingLayout
import com.pickflow.android.feature.onboarding.model.OnboardingPageContent

/** 0페이지 배경 그라데이션. 1~3페이지는 이미지에 배경이 포함돼 있어 필요 없다. */
private val topAlignedGradient = Brush.verticalGradient(
    0f to PickflowColors.sunsetOrange,
    1f to Color(0xFFF69648),
)

/** 페이지별 일러스트. 1~3페이지는 배경까지 합쳐진 단일 이미지 한 장이다. */
@Composable
fun OnboardingIllustration(
    page: OnboardingPageContent,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.clipToBounds()) {
        when (page.layout) {
            OnboardingLayout.TOP_ALIGNED_IMAGE -> Column(
                Modifier.fillMaxSize().background(topAlignedGradient),
            ) {
                Image(
                    painter = painterResource(page.illustration),
                    contentDescription = null,
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier
                        .padding(horizontal = 55.dp)
                        .fillMaxWidth(),
                )
                Spacer(Modifier.weight(1f))
            }

            // 하단 정렬 — 일러스트 영역 비율이 원본(390x500)보다 납작한 기기에서
            // Crop 이 중앙 기준으로 위아래를 함께 잘라 폰 목업 하단이 날아갔다.
            // 잘림을 전부 상단(그라데이션뿐)으로 몰아 하단 라인을 보존한다.
            OnboardingLayout.FULL_IMAGE -> Image(
                painter = painterResource(page.illustration),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alignment = Alignment.BottomCenter,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
