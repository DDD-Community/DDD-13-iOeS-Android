package com.pickflow.android.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.pickflow.android.feature.accountmanagement.AccountManagementScreen
import com.pickflow.android.feature.debug.DebugScreen
import com.pickflow.android.feature.home.HomeScreen
import com.pickflow.android.feature.login.LoginScreen
import com.pickflow.android.feature.notice.NoticeDetailScreen
import com.pickflow.android.feature.notice.NoticeListScreen
import com.pickflow.android.feature.onboarding.OnboardingScreen
import com.pickflow.android.feature.spotdetail.SpotDetailScreen
import com.pickflow.android.feature.spotregistration.SpotRegistrationScreen
import com.pickflow.android.feature.forceupdate.ForceUpdateScreen
import com.pickflow.android.feature.forceupdate.ForceUpdateViewModel
import com.pickflow.android.feature.spotsearch.SpotSearchScreen
import com.pickflow.android.feature.withdrawal.WithdrawalScreen

@Composable
fun PickflowNavHost(
    entryViewModel: PickflowEntryViewModel = hiltViewModel(),
    forceUpdateViewModel: ForceUpdateViewModel = hiltViewModel(),
) {
    // iOS `ForceUpdateGate` 1:1 — 앱 진입 전에 `/v1/app/config/android` 정책 확인.
    val forceUpdateState by forceUpdateViewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { forceUpdateViewModel.checkForUpdate() }
    when (val s = forceUpdateState) {
        ForceUpdateViewModel.AppLaunchState.Checking -> return
        is ForceUpdateViewModel.AppLaunchState.NeedsForceUpdate -> {
            ForceUpdateScreen(storeUrl = s.storeUrl)
            return
        }
        ForceUpdateViewModel.AppLaunchState.Available -> Unit
    }

    val startDestination by entryViewModel.startDestination.collectAsStateWithLifecycle()
    val resolved = startDestination ?: return
    val navController = rememberNavController()

    val pendingSpotId by DeepLinkState.pendingSpotId.collectAsStateWithLifecycle()
    LaunchedEffect(pendingSpotId) {
        val id = pendingSpotId ?: return@LaunchedEffect
        navController.navigate(PickflowRoute.spotDetail(id.toString()))
        DeepLinkState.clear()
    }

    NavHost(navController = navController, startDestination = resolved) {

        composable(PickflowRoute.ONBOARDING) {
            OnboardingScreen(
                onFinished = {
                    navController.navigate(PickflowRoute.LOGIN) {
                        popUpTo(PickflowRoute.ONBOARDING) { inclusive = true }
                    }
                },
            )
        }

        composable(PickflowRoute.LOGIN) {
            LoginScreen(
                onLoggedIn = {
                    navController.navigate(PickflowRoute.HOME) {
                        popUpTo(PickflowRoute.LOGIN) { inclusive = true }
                    }
                },
            )
        }

        composable(PickflowRoute.HOME) {
            HomeScreen(
                onOpenSpotDetail = { navController.navigate(PickflowRoute.spotDetail(it)) },
                onOpenRegistration = { navController.navigate(PickflowRoute.SPOT_REGISTRATION) },
                onRequireLogin = {
                    navController.navigate(PickflowRoute.LOGIN) {
                        popUpTo(PickflowRoute.HOME) { inclusive = true }
                    }
                },
                onOpenDebug = { navController.navigate(PickflowRoute.DEBUG) },
                onOpenAccount = { navController.navigate(PickflowRoute.ACCOUNT_MANAGEMENT) },
                onOpenNotice = { navController.navigate(PickflowRoute.NOTICE_LIST) },
            )
        }

        composable(PickflowRoute.NOTICE_LIST) {
            NoticeListScreen(
                onBack = navController::popBackStack,
                onOpenDetail = { postId ->
                    navController.navigate(PickflowRoute.noticeDetail(postId))
                },
            )
        }

        composable(
            route = PickflowRoute.NOTICE_DETAIL,
            arguments = listOf(navArgument(PickflowRoute.ARG_NOTICE_POST_ID) {
                type = NavType.LongType
            }),
        ) { entry ->
            val postId = entry.arguments?.getLong(PickflowRoute.ARG_NOTICE_POST_ID) ?: 0L
            NoticeDetailScreen(postId = postId, onBack = navController::popBackStack)
        }

        composable(PickflowRoute.ACCOUNT_MANAGEMENT) {
            AccountManagementScreen(
                onBack = navController::popBackStack,
                onSignedOut = {
                    navController.navigate(PickflowRoute.LOGIN) {
                        popUpTo(PickflowRoute.HOME) { inclusive = true }
                    }
                },
                onOpenWithdrawal = { navController.navigate(PickflowRoute.WITHDRAWAL) },
            )
        }

        composable(PickflowRoute.WITHDRAWAL) {
            WithdrawalScreen(
                onBack = navController::popBackStack,
                onWithdrawn = {
                    navController.navigate(PickflowRoute.LOGIN) {
                        popUpTo(PickflowRoute.HOME) { inclusive = true }
                    }
                },
            )
        }

        composable(
            route = PickflowRoute.SPOT_DETAIL,
            arguments = listOf(navArgument(PickflowRoute.ARG_SPOT_ID) {
                type = NavType.StringType
            }),
        ) { entry ->
            val spotId = entry.arguments?.getString(PickflowRoute.ARG_SPOT_ID).orEmpty()
            SpotDetailScreen(
                spotId = spotId,
                onBack = navController::popBackStack,
                onRequireLogin = { navController.navigate(PickflowRoute.LOGIN) },
            )
        }

        composable(PickflowRoute.SPOT_SEARCH) {
            SpotSearchScreen(onBack = navController::popBackStack)
        }

        composable(PickflowRoute.SPOT_REGISTRATION) {
            SpotRegistrationScreen(
                onBack = navController::popBackStack,
                onRegistered = { navController.popBackStack() },
            )
        }

        composable(PickflowRoute.DEBUG) {
            DebugScreen(onBack = navController::popBackStack)
        }
    }
}
