package com.pickflow.android.feature.login.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pickflow.android.R
import com.pickflow.android.common.designsystem.PickflowColors
import com.pickflow.android.common.designsystem.PickflowTypography

/**
 * 비로그인 탭(보관/마이) 공통 안내 화면.
 *
 * 레이아웃은 두 탭이 완전히 동일하며, **title/description 텍스트만** 탭별로 다르다.
 * - 좌상단 PICKFLOW 워드마크
 * - 화면 중앙에 제목 + 설명 + 카카오/애플 로그인 버튼(그룹)
 */
@Composable
fun SignedOutLoginContent(
    title: String,
    description: String,
    onKakaoLogin: () -> Unit,
    onAppleLogin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PickflowColors.gray95)
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp, bottom = 24.dp),
    ) {
        // 좌상단 워드마크.
        Image(
            painter = painterResource(R.drawable.logo),
            contentDescription = "PICKFLOW",
            modifier = Modifier
                .padding(start = 4.dp, top = 4.dp)
                .height(24.dp),
        )

        Spacer(Modifier.weight(1f))

        // 중앙 그룹: 제목 + 설명 + 로그인 버튼.
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = title,
                style = PickflowTypography.headingLarge,
                color = PickflowColors.gray0,
                textAlign = TextAlign.Center,
            )
            Text(
                text = description,
                style = PickflowTypography.bodyMedium,
                color = PickflowColors.gray40,
                textAlign = TextAlign.Center,
            )
        }

        Column(
            modifier = Modifier
                .padding(top = 32.dp)
                .align(Alignment.CenterHorizontally)
                .widthIn(max = 358.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            KakaoLoginButton(onClick = onKakaoLogin)
            // Apple 로그인 임시 비활성화.
            // AppleLoginButton(onClick = onAppleLogin)
        }

        Spacer(Modifier.weight(1f))
    }
}
