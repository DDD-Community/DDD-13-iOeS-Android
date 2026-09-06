package com.pickflow.android.feature.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pickflow.android.BuildConfig
import com.pickflow.android.app.navigation.HomeTab
import com.pickflow.android.app.navigation.HomeTabRequest
import com.pickflow.android.common.designsystem.PickflowColors
import com.pickflow.android.feature.archive.ArchiveScreen
import com.pickflow.android.feature.archive.ArchiveTab
import com.pickflow.android.core.services.protocols.ApiEnvironment
import com.pickflow.android.feature.devmode.DEV_MODE_TAP_THRESHOLD
import com.pickflow.android.feature.devmode.DevEnvironmentBadge
import com.pickflow.android.feature.devmode.DevModePasscodeDialog
import com.pickflow.android.feature.devmode.DevModeViewModel
import com.pickflow.android.feature.map.HomeMapScreen
import com.pickflow.android.feature.myprofile.MyProfileScreen

@Composable
fun HomeScreen(
    onOpenSpotDetail: (String) -> Unit,
    onOpenMySpot: (Long) -> Unit,
    onOpenRegistration: () -> Unit,
    onRequireLogin: () -> Unit,
    onOpenDevMode: () -> Unit,
    onOpenAccount: () -> Unit,
    onOpenNotice: () -> Unit = {},
    onOpenTermsAndPolicy: () -> Unit = {},
    devModeViewModel: DevModeViewModel = hiltViewModel(),
    reviewResultViewModel: ReviewResultViewModel = hiltViewModel(),
) {
    var selectedTab by rememberHomeTabState()
    val apiEnvironment by devModeViewModel.apiEnvironment.collectAsStateWithLifecycle()
    // 마이 탭 연타 → 코드 입력 → Dev Mode. 다른 탭을 누르면 초기화된다.
    // 릴리스 빌드에는 진입점 자체가 없다.
    var myTabTaps by remember { mutableIntStateOf(0) }
    var askPasscode by remember { mutableStateOf(false) }
    // 마이페이지 카드 → 보관 탭의 특정 내부 탭으로 진입 요청.
    var pendingArchiveTab by remember { mutableStateOf<ArchiveTab?>(null) }
    val hasSavedIndicator by reviewResultViewModel.hasIndicator.collectAsStateWithLifecycle()
    val latestReviewResult by reviewResultViewModel.latestUnacknowledgedResult.collectAsStateWithLifecycle()
    var dismissedReviewResultId by rememberSaveable { mutableStateOf<Long?>(null) }

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
                onTabSelected = { tab ->
                    myTabTaps =
                        if (BuildConfig.DEBUG && tab == HomeTab.MY) myTabTaps + 1 else 0
                    if (myTabTaps >= DEV_MODE_TAP_THRESHOLD) {
                        myTabTaps = 0
                        askPasscode = true
                    }
                    selectedTab = tab
                },
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
                )
                HomeTab.SAVED -> ArchiveScreen(
                    onOpenSpotDetail = onOpenSpotDetail,
                    onOpenMySpot = onOpenMySpot,
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
                    // 운영이 아닌 서버를 보고 있다는 경고이므로 기본값 여부와 무관하게 붙인다.
                    environmentSuffix = apiEnvironment
                        .takeIf { it != ApiEnvironment.PROD }
                        ?.shortLabel,
                )
            }

            // 3개 탭 어디에서나 좌측 하단에 떠 있는 환경 배지 (Dev Mode 토글로 켬).
            DevEnvironmentBadge(
                onClick = onOpenDevMode,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 16.dp, bottom = 16.dp),
                viewModel = devModeViewModel,
            )

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

    if (askPasscode) {
        DevModePasscodeDialog(
            onDismiss = { askPasscode = false },
            onUnlocked = {
                askPasscode = false
                onOpenDevMode()
            },
        )
    }
}

/**
 * 하단 탭 선택 상태. HOME 백스택 엔트리의 SavedState 에 얹는다.
 *
 * `remember` 로 두면 상세·공지 등 다른 라우트로 push 했다가 pop 할 때 HOME 컴포지션이
 * 새로 만들어지면서 초기값 EXPLORE 로 되돌아간다(어느 탭에서 들어갔든 뒤로가기가 탐색 탭).
 */
@Composable
internal fun rememberHomeTabState(): MutableState<HomeTab> =
    rememberSaveable { mutableStateOf(HomeTab.EXPLORE) }
