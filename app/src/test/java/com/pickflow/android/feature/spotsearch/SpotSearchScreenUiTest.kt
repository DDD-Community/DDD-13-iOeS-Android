package com.pickflow.android.feature.spotsearch

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextInput
import com.pickflow.android.common.designsystem.PickflowTheme
import com.pickflow.android.core.services.protocols.AddressService
import com.pickflow.android.core.services.protocols.AddressSuggestion
import com.pickflow.android.core.services.protocols.LocationService
import io.mockk.coEvery
import io.mockk.mockk
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SpotSearchScreenUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun renders_search_field() {
        val vm = SpotSearchViewModel(
            mockk<AddressService>(relaxed = true),
            mockk<LocationService>(relaxed = true),
        )
        composeRule.setContent {
            PickflowTheme { SpotSearchScreen(onBack = {}, viewModel = vm) }
        }
        composeRule.onNodeWithTag("spotsearch-screen").assertIsDisplayed()
        composeRule.onNodeWithTag("search-field").assertIsDisplayed()
    }

    @Test
    fun typing_query_shows_results() {
        val addressService = mockk<AddressService>()
        coEvery { addressService.search(any()) } returns listOf(
            AddressSuggestion("강남역", 37.49, 127.02),
        )
        val vm = SpotSearchViewModel(addressService, mockk<LocationService>(relaxed = true))

        composeRule.setContent {
            PickflowTheme { SpotSearchScreen(onBack = {}, viewModel = vm) }
        }
        composeRule.onNodeWithTag("search-field").performTextInput("강남")
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("search-results").assertIsDisplayed()
    }
}
