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

    private fun suggestion() = AddressSuggestion(
        name = "Seoul",
        fullAddress = "서울특별시 중구",
        latitude = 37.5,
        longitude = 127.0,
    )

    @Test
    fun `blank query resets to Idle`() = runTest(testDispatcher) {
        val vm = SpotSearchViewModel(addressService, locationService)
        vm.onQueryChanged(""); advanceUntilIdle()
        assertEquals(LoadState.Idle, vm.suggestions.value)
    }

    @Test
    fun `query with results emits Loaded`() = runTest(testDispatcher) {
        coEvery { locationService.currentLocation() } returns null
        coEvery { addressService.search("seoul") } returns listOf(suggestion())
        val vm = SpotSearchViewModel(addressService, locationService)
        vm.onQueryChanged("seoul"); advanceUntilIdle()
        assertTrue(vm.suggestions.value is LoadState.Loaded)
    }

    @Test
    fun `query with empty result emits Empty`() = runTest(testDispatcher) {
        coEvery { locationService.currentLocation() } returns null
        coEvery { addressService.search(any()) } returns emptyList()
        val vm = SpotSearchViewModel(addressService, locationService)
        vm.onQueryChanged("???"); advanceUntilIdle()
        assertEquals(LoadState.Empty, vm.suggestions.value)
    }

    @Test
    fun `distanceText returns formatted distance when location available`() = runTest(testDispatcher) {
        coEvery { locationService.currentLocation() } returns Coordinates(37.5, 127.0)
        coEvery { addressService.search(any()) } returns listOf(suggestion())
        val vm = SpotSearchViewModel(addressService, locationService)
        vm.onQueryChanged("seoul"); advanceUntilIdle()
        val text = vm.distanceText(suggestion())
        assertNotNull(text)
        assertEquals("0.0km", text)
    }

    @Test
    fun `distanceText null when location unavailable`() = runTest(testDispatcher) {
        coEvery { locationService.currentLocation() } returns null
        coEvery { addressService.search(any()) } returns listOf(suggestion())
        val vm = SpotSearchViewModel(addressService, locationService)
        vm.onQueryChanged("seoul"); advanceUntilIdle()
        assertNull(vm.distanceText(suggestion()))
    }
}
