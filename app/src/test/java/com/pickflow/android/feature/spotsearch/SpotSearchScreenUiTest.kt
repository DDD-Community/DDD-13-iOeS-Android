package com.pickflow.android.feature.spotsearch

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
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
            PickflowTheme { SpotSearchScreen(onBack = {}, onSelectResult = {}, viewModel = vm) }
        }
        composeRule.onNodeWithTag("spotsearch-screen").assertIsDisplayed()
        composeRule.onNodeWithTag("search-field").assertIsDisplayed()
    }

    @Test
    fun typing_query_shows_results() {
        val addressService = mockk<AddressService>()
        coEvery { addressService.search(any()) } returns listOf(
            AddressSuggestion(
                name = "강남역",
                fullAddress = "서울 강남구 강남대로",
                latitude = 37.49,
                longitude = 127.02,
            ),
        )
        val vm = SpotSearchViewModel(addressService, mockk<LocationService>(relaxed = true))

        composeRule.setContent {
            PickflowTheme { SpotSearchScreen(onBack = {}, onSelectResult = {}, viewModel = vm) }
        }
        // search-field 태그는 텍스트필드 래퍼에 있어 performTextInput 이 불가 —
        // ViewModel 을 직접 구동해 결과 리스트 노출을 검증한다.
        composeRule.runOnIdle { vm.onQueryChanged("강남") }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("search-results").assertIsDisplayed()
    }
}
