package com.pickflow.android.feature.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import com.pickflow.android.app.navigation.HomeTab
import com.pickflow.android.common.designsystem.PickflowColors
import com.pickflow.android.feature.map.HomeMapScreen
import com.pickflow.android.feature.myprofile.MyProfileScreen
import com.pickflow.android.feature.spotlist.SpotListScreen

@Composable
fun HomeScreen(
    onOpenSpotDetail: (String) -> Unit,
    onOpenRegistration: () -> Unit,
    onRequireLogin: () -> Unit,
    onOpenDebug: () -> Unit,
    onOpenAccount: () -> Unit,
) {
    var selectedTab by remember { mutableStateOf(HomeTab.EXPLORE) }

    Scaffold(
        containerColor = PickflowColors.gray95,
        bottomBar = {
            NavigationBar(containerColor = PickflowColors.gray90) {
                HomeTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = { Icon(tab.icon(), contentDescription = tab.label) },
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
                )
                HomeTab.SAVED -> SpotListScreen(
                    onOpenSpotDetail = onOpenSpotDetail,
                    onRequireLogin = onRequireLogin,
                )
                HomeTab.MY -> MyProfileScreen(
                    onRequireLogin = onRequireLogin,
                    onOpenDebug = onOpenDebug,
                    onOpenAccount = onOpenAccount,
                )
            }
        }
    }
}

private fun HomeTab.icon(): ImageVector = when (this) {
    HomeTab.EXPLORE -> Icons.Filled.Place
    HomeTab.SAVED -> Icons.Filled.Favorite
    HomeTab.MY -> Icons.Filled.Person
}
