package com.pickflow.android.feature.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pickflow.android.app.navigation.HomeTab
import com.pickflow.android.app.navigation.HomeTabRequest
import com.pickflow.android.common.designsystem.PickflowColors
import com.pickflow.android.feature.archive.ArchiveScreen
import com.pickflow.android.feature.archive.ArchiveTab
import com.pickflow.android.feature.map.HomeMapScreen
import com.pickflow.android.feature.myprofile.MyProfileScreen

@Composable
fun HomeScreen(
    onOpenSpotDetail: (String) -> Unit,
    onOpenRegistration: () -> Unit,
    onRequireLogin: () -> Unit,
    onOpenDebug: () -> Unit,
    onOpenAccount: () -> Unit,
    onOpenNotice: () -> Unit = {},
    onOpenTermsAndPolicy: () -> Unit = {},
    reviewResultViewModel: ReviewResultViewModel = hiltViewModel(),
) {
    var selectedTab by remember { mutableStateOf(HomeTab.EXPLORE) }
    // 마이페이지 카드 → 보관 탭의 특정 내부 탭으로 진입 요청.
    var pendingArchiveTab by remember { mutableStateOf<ArchiveTab?>(null) }
    val hasSavedIndicator by reviewResultViewModel.hasIndicator.collectAsStateWithLifecycle()
    val latestReviewResult by reviewResultViewModel.latestUnacknowledgedResult.collectAsStateWithLifecycle()
    var dismissedReviewResultId by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(Unit) { reviewResultViewModel.load() }

    // HOME 바깥 라우트(스팟 등록 완료 등)에서 온 탭 전환 요청 소비.
    val tabRequest by HomeTabRequest.pending.collectAsStateWithLifecycle()
    LaunchedEffect(tabRequest) {
        tabRequest?.let { request ->
            selectedTab = request.tab
            pendingArchiveTab = request.archiveTab
            HomeTabRequest.clear()
        }
    }

    Scaffold(
        containerColor = PickflowColors.gray95,
        bottomBar = {
            HomeBottomNavigation(
                selectedTab = selectedTab,
                hasSavedIndicator = hasSavedIndicator,
                onTabSelected = { selectedTab = it },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center,
        ) {
            when (selectedTab) {
                HomeTab.EXPLORE -> HomeMapScreen(
                    onOpenSpotDetail = onOpenSpotDetail,
                    onOpenRegistration = onOpenRegistration,
                    onRequireLogin = onRequireLogin,
                    onOpenDebug = onOpenDebug,
                )
                HomeTab.SAVED -> ArchiveScreen(
                    onOpenSpotDetail = onOpenSpotDetail,
                    onRequireLogin = onRequireLogin,
                    onExploreClick = { selectedTab = HomeTab.EXPLORE },
                    onOpenRegistration = onOpenRegistration,
                    initialTab = pendingArchiveTab,
                    onInitialTabConsumed = { pendingArchiveTab = null },
                )
                HomeTab.MY -> MyProfileScreen(
                    onRequireLogin = onRequireLogin,
                    onOpenAccount = onOpenAccount,
                    onOpenNotice = onOpenNotice,
                    onOpenTermsAndPolicy = onOpenTermsAndPolicy,
                    onOpenSavedSpots = {
                        pendingArchiveTab = ArchiveTab.SavedSpots
                        selectedTab = HomeTab.SAVED
                    },
                    onOpenMySpots = {
                        pendingArchiveTab = ArchiveTab.MySpots
                        selectedTab = HomeTab.SAVED
                    },
                )
            }

            latestReviewResult
                ?.takeUnless { it.resultId == dismissedReviewResultId }
                ?.let { result ->
                    ReviewResultSnackbar(
                        result = result,
                        onOpenResult = { spotId ->
                            reviewResultViewModel.acknowledge(result.resultId)
                            onOpenSpotDetail(spotId.toString())
                        },
                        onClose = { dismissedReviewResultId = result.resultId },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 16.dp),
                    )
                }
        }
    }
}
