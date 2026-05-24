package com.pickflow.android.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.pickflow.android.feature.accountmanagement.AccountManagementScreen
import com.pickflow.android.feature.debug.DebugScreen
import com.pickflow.android.feature.home.HomeScreen
import com.pickflow.android.feature.login.LoginScreen
import com.pickflow.android.feature.onboarding.OnboardingScreen
import com.pickflow.android.feature.spotdetail.SpotDetailScreen
import com.pickflow.android.feature.spotregistration.SpotRegistrationScreen
import com.pickflow.android.feature.spotsearch.SpotSearchScreen

@Composable
fun PickflowNavHost(startDestination: String = PickflowRoute.ONBOARDING) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = startDestination) {

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
            )
        }

        composable(PickflowRoute.ACCOUNT_MANAGEMENT) {
            AccountManagementScreen(
                onBack = navController::popBackStack,
                onSignedOut = {
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
            SpotDetailScreen(spotId = spotId, onBack = navController::popBackStack)
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
