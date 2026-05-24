package com.pickflow.android.feature.spotsearch

import com.pickflow.android.common.ui.LoadState
import com.pickflow.android.core.services.protocols.AddressService
import com.pickflow.android.core.services.protocols.AddressSuggestion
import com.pickflow.android.core.services.protocols.Coordinates
import com.pickflow.android.core.services.protocols.LocationService
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SpotSearchViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var addressService: AddressService
    private lateinit var locationService: LocationService

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        addressService = mockk()
        locationService = mockk()
    }

    @AfterEach
    fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `blank query resets to Idle`() = runTest(testDispatcher) {
        val vm = SpotSearchViewModel(addressService, locationService)
        vm.onQueryChanged(""); advanceUntilIdle()
        assertEquals(LoadState.Idle, vm.suggestions.value)
    }

    @Test
    fun `query with results emits Loaded`() = runTest(testDispatcher) {
        coEvery { addressService.search("seoul") } returns listOf(
            AddressSuggestion("Seoul", 37.0, 127.0)
        )
        val vm = SpotSearchViewModel(addressService, locationService)
        vm.onQueryChanged("seoul"); advanceUntilIdle()
        assertTrue(vm.suggestions.value is LoadState.Loaded)
    }

    @Test
    fun `query with empty result emits Empty`() = runTest(testDispatcher) {
        coEvery { addressService.search(any()) } returns emptyList()
        val vm = SpotSearchViewModel(addressService, locationService)
        vm.onQueryChanged("???"); advanceUntilIdle()
        assertEquals(LoadState.Empty, vm.suggestions.value)
    }

    @Test
    fun `useCurrentLocation resolves to suggestion`() = runTest(testDispatcher) {
        coEvery { locationService.currentLocation() } returns Coordinates(37.5, 127.0)
        val vm = SpotSearchViewModel(addressService, locationService)
        var captured: AddressSuggestion? = null
        vm.useCurrentLocation { captured = it }
        advanceUntilIdle()
        assertNotNull(captured)
        assertEquals(37.5, captured!!.latitude)
    }

    @Test
    fun `useCurrentLocation null when service returns null`() = runTest(testDispatcher) {
        coEvery { locationService.currentLocation() } returns null
        val vm = SpotSearchViewModel(addressService, locationService)
        var resolved: AddressSuggestion? = AddressSuggestion("", 0.0, 0.0)
        vm.useCurrentLocation { resolved = it }
        advanceUntilIdle()
        assertNull(resolved)
    }
}
