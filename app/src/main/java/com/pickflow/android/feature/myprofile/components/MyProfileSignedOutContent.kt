package com.pickflow.android.feature.myprofile.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.pickflow.android.common.designsystem.PickflowColors
import com.pickflow.android.common.designsystem.PickflowTypography
import com.pickflow.android.feature.login.components.AppleLoginButton
import com.pickflow.android.feature.login.components.KakaoLoginButton

/**
 * iOS `MyProfileSignedOutContent` 1:1 이식 — 비로그인 마이페이지.
 *
 * 상단 워드마크 + Spacer + (안내 텍스트 + 로그인 CTA) + Spacer 구조.
 * 워드마크는 브랜드 로고 에셋 자리라 raw 텍스트 placeholder로 자리만 잡는다.
 */
@Composable
fun MyProfileSignedOutContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PickflowColors.gray95)
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp, bottom = 66.dp)
            .testTag("myprofile-signedout"),
    ) {
        // header — 워드마크 leading 정렬
        Wordmark()

        Spacer(Modifier.weight(1f))

        // centerContent — 화면 중앙 안내 텍스트
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "마이페이지 이용을 위해\n로그인이 필요해요",
                style = PickflowTypography.headingLarge,
                color = PickflowColors.gray0,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "로그인하면 내 스팟을 저장하고\n일몰 알림을 받을 수 있어요.",
                style = PickflowTypography.bodyMedium,
                color = PickflowColors.gray40,
                textAlign = TextAlign.Center,
            )
        }

        Spacer(Modifier.weight(1f))

        // bottomCTA — 하단 고정 로그인 버튼(maxWidth 358)
        Column(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .widthIn(max = 358.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            KakaoLoginButton(onClick = {})
            AppleLoginButton(onClick = {})
        }
    }
}

/** iOS `Image("pickflow_wordmark")`(140x32) 자리 — 브랜드 로고 에셋이라 raw 텍스트 placeholder. */
@Composable
private fun Wordmark() {
    val density = LocalDensity.current
    CompositionLocalProvider(
        LocalDensity provides Density(density.density, fontScale = 1f),
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "PICKFLOW",
                style = PickflowTypography.headingMedium.copy(fontWeight = FontWeight.Bold),
                color = PickflowColors.gray0,
            )
        }
    }
}
