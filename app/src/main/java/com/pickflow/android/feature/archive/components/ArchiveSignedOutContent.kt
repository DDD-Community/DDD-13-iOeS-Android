package com.pickflow.android.feature.archive.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pickflow.android.common.designsystem.PickflowColors
import com.pickflow.android.common.designsystem.PickflowTypography
import com.pickflow.android.feature.login.components.AppleLoginButton
import com.pickflow.android.feature.login.components.KakaoLoginButton

/**
 * iOS `ArchiveSignedOutContent` 1:1 — 비로그인 시 보관함 안내 + 로그인 CTA.
 *
 * Android 컨벤션상 카카오/애플 버튼 두 개 모두 `onRequireLogin` 단일 콜백으로
 * 위임한다 — 실제 소셜 로그인 플로우는 `LoginScreen` 에서 처리.
 */
@Composable
fun ArchiveSignedOutContent(
    onKakaoLogin: () -> Unit = {},
    onAppleLogin: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp)
            .testTag("archive-signedout"),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "보관함 이용을 위해\n로그인이 필요해요",
                style = PickflowTypography.headingLarge,
                color = PickflowColors.gray0,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "로그인하면 저장한 스팟을\n한 곳에서 확인할 수 있어요.",
                style = PickflowTypography.bodyMedium,
                color = PickflowColors.gray40,
                textAlign = TextAlign.Center,
            )
        }

        Column(
            modifier = Modifier
                .padding(top = 32.dp)
                .widthIn(max = 358.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            KakaoLoginButton(onClick = onKakaoLogin, modifier = Modifier.testTag("archive-kakao"))
            AppleLoginButton(onClick = onAppleLogin, modifier = Modifier.testTag("archive-apple"))
        }

        Spacer(Modifier.weight(1f))
    }
}
