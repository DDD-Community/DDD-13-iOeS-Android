package com.pickflow.android.feature.myprofile.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.pickflow.android.feature.login.components.SignedOutLoginContent

/**
 * 비로그인 마이 탭 — 공통 [SignedOutLoginContent] 에 마이 탭 문구만 주입.
 * 보관 탭과 레이아웃은 동일하고 title/description 만 다르다.
 */
@Composable
fun MyProfileSignedOutContent(
    onKakaoLogin: () -> Unit = {},
    onAppleLogin: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    SignedOutLoginContent(
        title = "마이페이지 이용을 위해\n로그인이 필요해요",
        description = "지금 로그인하고 내가 공유한 스팟들과\n활동 내역을 한눈에 확인해 보세요.",
        onKakaoLogin = onKakaoLogin,
        onAppleLogin = onAppleLogin,
        modifier = modifier.testTag("myprofile-signedout"),
    )
}
