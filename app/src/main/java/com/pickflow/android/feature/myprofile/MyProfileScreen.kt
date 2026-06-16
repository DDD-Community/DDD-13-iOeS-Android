package com.pickflow.android.feature.myprofile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pickflow.android.common.designsystem.PickflowColors
import com.pickflow.android.common.ui.LoadStateContent
import com.pickflow.android.feature.myprofile.components.MyProfileSignedInContent
import com.pickflow.android.feature.myprofile.components.MyProfileSignedOutContent

@Composable
fun MyProfileScreen(
    onRequireLogin: () -> Unit,
    onOpenAccount: () -> Unit = {},
    onOpenNotice: () -> Unit = {},
    onOpenTermsAndPolicy: () -> Unit = {},
    onOpenSavedSpots: () -> Unit = {},
    onOpenMySpots: () -> Unit = {},
    viewModel: MyProfileViewModel = hiltViewModel(),
) {
    val loggedIn by viewModel.loggedIn.collectAsStateWithLifecycle()
    val myPage by viewModel.myPage.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.load() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PickflowColors.gray95)
            .testTag("myprofile-screen"),
    ) {
        when (loggedIn) {
            null -> Unit
            false -> MyProfileSignedOutContent(
                onKakaoLogin = onRequireLogin,
                onAppleLogin = onRequireLogin,
            )
            true -> LoadStateContent(
                state = myPage,
                emptyMessage = "프로필 정보가 없어요.",
                onRetry = viewModel::load,
            ) { home ->
                MyProfileSignedInContent(
                    home = home,
                    onOpenAccount = onOpenAccount,
                    onOpenNotice = onOpenNotice,
                    onOpenTermsAndPolicy = onOpenTermsAndPolicy,
                    onOpenSavedSpots = onOpenSavedSpots,
                    onOpenMySpots = onOpenMySpots,
                )
            }
        }
    }
}
