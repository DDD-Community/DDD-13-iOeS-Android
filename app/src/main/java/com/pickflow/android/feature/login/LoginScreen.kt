package com.pickflow.android.feature.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pickflow.android.R
import com.pickflow.android.common.designsystem.PickflowColors
import com.pickflow.android.common.designsystem.PickflowTypography
import com.pickflow.android.common.ui.LoadState
// import com.pickflow.android.feature.login.components.AppleLoginButton // Apple 로그인 임시 비활성화.
import com.pickflow.android.feature.login.components.KakaoLoginButton

// iOS LoginView.backgroundGradient 4-stop LinearGradient 1:1 이식.
private val LoginGradientStops = arrayOf(
    0.0f to Color(red = 181, green = 127, blue = 0),
    0.2f to Color(red = 188, green = 59, blue = 0),
    0.5f to Color(red = 15, green = 23, blue = 40),
    1.0f to Color(red = 19, green = 20, blue = 22),
)

@Composable
fun LoginScreen(
    viewModel: LoginViewModel = hiltViewModel(),
    onLoggedIn: () -> Unit,
) {
    val session by viewModel.session.collectAsStateWithLifecycle()
    val restorePrompt by viewModel.restorePrompt.collectAsStateWithLifecycle()

    LaunchedEffect(session) {
        if (session is LoadState.Loaded<*>) onLoggedIn()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LoginScreenContent(
            kakaoLoading = session is LoadState.Loading,
            appleLoading = false,
            isClosable = false,
            onKakaoClick = viewModel::loginWithKakao,
            onAppleClick = viewModel::loginWithKakao,
            onGuestClick = onLoggedIn,
            onCloseClick = {},
        )

        // 탈퇴 이력 계정 재가입 안내 팝업(U007).
        restorePrompt?.let { prompt ->
            com.pickflow.android.feature.login.components.RestoreAccountDialog(
                message = prompt.message,
                onCancel = viewModel::dismissRestorePrompt,
                onConfirm = viewModel::confirmRestore,
            )
        }
    }
}

/**
 * iOS `LoginView`의 stateless 본문. Paparazzi 스냅샷/프리뷰에서 직접 호출한다.
 *
 * iOS는 `.preferredColorScheme(.dark)`로 항상 다크를 강제하므로 라이트 분기가 없다.
 */
@Preview
@Composable
fun LoginScreenContent(
    kakaoLoading: Boolean = false,
    appleLoading: Boolean = false,
    isClosable: Boolean = false,
    onKakaoClick: () -> Unit = {},
    onAppleClick: () -> Unit = {},
    onGuestClick: () -> Unit = {},
    onCloseClick: () -> Unit = {},
) {
    val isLoading = kakaoLoading || appleLoading

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(colorStops = LoginGradientStops))
            .testTag("login-screen"),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp, bottom = 66.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            LoginHeader(isClosable = isClosable, onCloseClick = onCloseClick)

            Spacer(Modifier.weight(1f))

            LoginCenterContent(
                modifier = Modifier
                    .widthIn(max = 295.dp)
                    .padding(bottom = 108.dp),
            )

            Spacer(Modifier.weight(1f))

            LoginBottomCTA(
                modifier = Modifier.widthIn(max = 358.dp),
                kakaoLoading = kakaoLoading,
                appleLoading = appleLoading,
                isLoading = isLoading,
                onKakaoClick = onKakaoClick,
                onAppleClick = onAppleClick,
                onGuestClick = onGuestClick,
            )
        }
    }
}

@Composable
private fun LoginHeader(
    isClosable: Boolean,
    onCloseClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // iOS pickflow_wordmark(브랜드 로고 에셋) 자리 — raw 텍스트 placeholder.
        // iOS 워드마크는 이미지라 Dynamic Type 영향을 받지 않으므로
        // placeholder 텍스트도 fontScale=1f로 고정해 동일하게 동작시킨다.
        Box(
            modifier = Modifier
                .width(140.dp)
                .height(32.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, fontScale = 1f),
            ) {
                Text(
                    text = "PICKFLOW",
                    style = PickflowTypography.headingSmall.copy(fontWeight = FontWeight.Bold),
                    color = PickflowColors.gray0,
                )
            }
        }

        Spacer(Modifier.weight(1f))

        if (isClosable) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clickable(onClick = onCloseClick)
                    .testTag("login-close"),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "닫기",
                    tint = PickflowColors.gray0,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun LoginCenterContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        // iOS appLogo 자리 — login_logo PNG로 교체. 기존 60x60 사이즈 유지.
        Image(
            painter = painterResource(R.drawable.login_logo),
            contentDescription = "PICKFLOW",
            modifier = Modifier.size(60.dp),
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "일상 속 반짝임,\n실패 없이 포착하세요",
                style = PickflowTypography.displayLarge,
                color = PickflowColors.gray0,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "파편화된 포토스팟 정보는 이제 그만.\n정확한 일몰 시간과 촬영 팁을 한눈에 보세요.",
                style = PickflowTypography.bodyLarge,
                color = PickflowColors.gray20,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun LoginBottomCTA(
    kakaoLoading: Boolean,
    appleLoading: Boolean,
    isLoading: Boolean,
    onKakaoClick: () -> Unit,
    onAppleClick: () -> Unit,
    onGuestClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            KakaoLoginButton(
                onClick = onKakaoClick,
                modifier = Modifier.testTag("login-kakao"),
                isLoading = kakaoLoading,
                enabled = !isLoading,
            )
            // Apple 로그인 임시 비활성화.
            // AppleLoginButton(
            //     onClick = onAppleClick,
            //     modifier = Modifier.testTag("login-apple"),
            //     isLoading = appleLoading,
            //     enabled = !isLoading,
            // )
        }

        Box(
            modifier = Modifier
                .heightIn(min = 25.dp)
                .clickable(onClick = onGuestClick)
                .testTag("login-guest"),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "비회원으로 시작하기",
                style = PickflowTypography.bodyMedium,
                color = PickflowColors.gray30,
                textDecoration = TextDecoration.Underline,
            )
        }
    }
}
