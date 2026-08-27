package com.pickflow.android.feature.home

import androidx.compose.foundation.clickable
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.pickflow.android.app.navigation.HomeTab
import com.pickflow.android.common.designsystem.PickflowTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Phase B — 하단 탭 선택이 HOME 백스택 엔트리에 남는지.
 *
 * 보관/마이 탭에서 상세·공지 라우트로 push 했다가 pop 하면 HOME 컴포지션이 새로 만들어진다.
 * 이때 탭 상태가 초기값(EXPLORE)으로 돌아가면 안 된다.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h950dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class HomeTabBackStackUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun selected_tab_survives_push_and_pop() {
        lateinit var navController: NavHostController
        var composedTab: HomeTab? = null

        composeRule.setContent {
            PickflowTheme {
                navController = rememberNavController()
                NavHost(navController = navController, startDestination = "home") {
                    composable("home") {
                        var selectedTab by rememberHomeTabState()
                        composedTab = selectedTab
                        Text(
                            text = selectedTab.name,
                            modifier = Modifier
                                .clickable { selectedTab = HomeTab.SAVED }
                                .testTag("home-tab-saved"),
                        )
                    }
                    composable("detail") { Text("detail") }
                }
            }
        }

        composeRule.onNodeWithTag("home-tab-saved").performClick()
        composeRule.runOnIdle { assertEquals(HomeTab.SAVED, composedTab) }

        composeRule.runOnIdle { navController.navigate("detail") }
        composeRule.onNodeWithText("detail").assertIsDisplayed()

        composeRule.runOnIdle { navController.popBackStack() }
        composeRule.runOnIdle { assertEquals(HomeTab.SAVED, composedTab) }
    }
}
