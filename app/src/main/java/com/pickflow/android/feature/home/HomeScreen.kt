package com.pickflow.android.feature.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.pickflow.android.R
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
) {
    var selectedTab by remember { mutableStateOf(HomeTab.EXPLORE) }
    // 마이페이지 카드 → 보관 탭의 특정 내부 탭으로 진입 요청.
    var pendingArchiveTab by remember { mutableStateOf<ArchiveTab?>(null) }

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
            NavigationBar(containerColor = PickflowColors.gray90) {
                HomeTab.entries.forEach { tab ->
                    val selected = selectedTab == tab
                    NavigationBarItem(
                        selected = selected,
                        onClick = { selectedTab = tab },
                        icon = {
                            Image(
                                painter = painterResource(tab.iconRes(selected)),
                                contentDescription = tab.label,
                                modifier = Modifier.size(28.dp),
                            )
                        },
                        label = { Text(tab.label) },
                        modifier = Modifier.testTag("home-tab-${tab.name.lowercase()}"),
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PickflowColors.sunsetOrange,
                            selectedTextColor = PickflowColors.sunsetOrange,
                            unselectedIconColor = PickflowColors.gray40,
                            unselectedTextColor = PickflowColors.gray40,
                            indicatorColor = PickflowColors.gray80,
                        ),
                    )
                }
            }
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
        }
    }
}

private fun HomeTab.iconRes(selected: Boolean): Int = when (this) {
    HomeTab.EXPLORE -> if (selected) R.drawable.ic_map_selected else R.drawable.ic_map
    HomeTab.SAVED -> if (selected) R.drawable.ic_bookmark_selected else R.drawable.ic_bookmark
    HomeTab.MY -> if (selected) R.drawable.ic_person_selected else R.drawable.ic_person
}
