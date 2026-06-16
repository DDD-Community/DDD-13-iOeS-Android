package com.pickflow.android.feature.archive.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.pickflow.android.feature.login.components.SignedOutLoginContent

/**
 * 비로그인 보관 탭 — 공통 [SignedOutLoginContent] 에 보관 탭 문구만 주입.
 * 마이 탭과 레이아웃은 동일하고 title/description 만 다르다.
 */
@Composable
fun ArchiveSignedOutContent(
    onKakaoLogin: () -> Unit = {},
    onAppleLogin: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    SignedOutLoginContent(
        title = "보관함 기능을 사용하려면\n로그인이 필요해요",
        description = "마음에 드는 스팟을 저장하고, 언제든\n꺼내 볼 수 있는 나만의 리스트를 만들어 보세요.",
        onKakaoLogin = onKakaoLogin,
        onAppleLogin = onAppleLogin,
        modifier = modifier.testTag("archive-signedout"),
    )
}
