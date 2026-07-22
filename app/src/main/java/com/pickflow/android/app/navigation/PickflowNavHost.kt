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
import com.pickflow.android.feature.archive.ArchiveTab
import com.pickflow.android.feature.debug.DebugScreen
import com.pickflow.android.feature.home.HomeScreen
import com.pickflow.android.feature.login.LoginScreen
import com.pickflow.android.feature.myprofile.termsandpolicy.TermsAndPolicyListScreen
import com.pickflow.android.feature.notice.NoticeDetailScreen
import com.pickflow.android.feature.notice.NoticeListScreen
import com.pickflow.android.feature.onboarding.OnboardingScreen
import com.pickflow.android.feature.spotdetail.SpotDetailScreen
import com.pickflow.android.feature.spotregistration.SpotRegistrationScreen
import com.pickflow.android.feature.spotregistration.SpotRegistrationViewModel
import com.pickflow.android.feature.forceupdate.ForceUpdateScreen
import com.pickflow.android.feature.forceupdate.ForceUpdateViewModel
import com.pickflow.android.feature.spotsearch.SpotLocationDetailScreen
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
                onOpenTermsAndPolicy = { navController.navigate(PickflowRoute.TERMS_AND_POLICY) },
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

        composable(PickflowRoute.TERMS_AND_POLICY) {
            TermsAndPolicyListScreen(onBack = navController::popBackStack)
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
            arguments = listOf(
                navArgument(PickflowRoute.ARG_SPOT_ID) {
                    type = NavType.StringType
                },
                navArgument(PickflowRoute.ARG_REGISTERED) {
                    type = NavType.BoolType
                    defaultValue = false
                },
            ),
        ) { entry ->
            val spotId = entry.arguments?.getString(PickflowRoute.ARG_SPOT_ID).orEmpty()
            val registered = entry.arguments?.getBoolean(PickflowRoute.ARG_REGISTERED) ?: false
            SpotDetailScreen(
                spotId = spotId,
                onBack = navController::popBackStack,
                onRequireLogin = { navController.navigate(PickflowRoute.LOGIN) },
                showRegisteredToast = registered,
            )
        }

        composable(PickflowRoute.SPOT_SEARCH) {
            // 등록 화면과 동일 ViewModel 인스턴스를 공유(선택 주소 전달).
            val regEntry = navController.getBackStackEntry(PickflowRoute.SPOT_REGISTRATION)
            val regViewModel: SpotRegistrationViewModel = hiltViewModel(regEntry)
            SpotSearchScreen(
                onBack = navController::popBackStack,
                onSelectResult = { suggestion ->
                    regViewModel.setPendingAddress(suggestion)
                    navController.navigate(PickflowRoute.SPOT_LOCATION_DETAIL)
                },
            )
        }

        composable(PickflowRoute.SPOT_LOCATION_DETAIL) {
            val regEntry = navController.getBackStackEntry(PickflowRoute.SPOT_REGISTRATION)
            val regViewModel: SpotRegistrationViewModel = hiltViewModel(regEntry)
            val pending by regViewModel.pendingAddress.collectAsStateWithLifecycle()
            pending?.let { candidate ->
                SpotLocationDetailScreen(
                    candidate = candidate,
                    onBack = navController::popBackStack,
                    onConfirm = { confirmed ->
                        regViewModel.applyAddressSelection(confirmed)
                        // 검색 → 등록까지 한 번에 복귀.
                        navController.popBackStack(PickflowRoute.SPOT_REGISTRATION, inclusive = false)
                    },
                )
            }
        }

        composable(PickflowRoute.SPOT_REGISTRATION) {
            SpotRegistrationScreen(
                onBack = navController::popBackStack,
                onOpenSearch = { navController.navigate(PickflowRoute.SPOT_SEARCH) },
                onRegistered = { spotId ->
                    // 등록 완료 → 보관함(마이 스팟 탭) 위에 등록 스팟 상세 + 완료 토스트.
                    HomeTabRequest.request(HomeTab.SAVED, ArchiveTab.MySpots)
                    navController.popBackStack()
                    navController.navigate(PickflowRoute.spotDetail(spotId, registered = true))
                },
            )
        }

        composable(PickflowRoute.DEBUG) {
            DebugScreen(onBack = navController::popBackStack)
        }
    }
}
