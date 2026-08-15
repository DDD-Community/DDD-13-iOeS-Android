package com.pickflow.android.feature.spotregistration

import app.cash.turbine.test
import com.pickflow.android.common.ui.LoadState
import com.pickflow.android.core.services.protocols.AddressSuggestion
import com.pickflow.android.core.services.protocols.CreateMySpotResult
import com.pickflow.android.core.services.protocols.ImagePayload
import com.pickflow.android.core.services.protocols.LocationService
import com.pickflow.android.core.services.protocols.MySpotDetail
import com.pickflow.android.core.services.protocols.MySpotService
import com.pickflow.android.core.services.protocols.MySpotStatus
import com.pickflow.android.core.services.protocols.MySpotTransitionResult
import com.pickflow.android.core.services.protocols.MySpotUpdateResult
import com.pickflow.android.core.services.protocols.RejectionReason
import com.pickflow.android.core.services.protocols.SpotRejection
import com.pickflow.android.core.services.protocols.SpotSource
import com.pickflow.android.core.services.protocols.SpotDraft
import com.pickflow.android.core.services.protocols.SpotTheme
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SpotRegistrationViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mySpotService: MySpotService
    private lateinit var locationService: LocationService

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mySpotService = mockk()
        locationService = mockk(relaxed = true)
    }

    @AfterEach
    fun tearDown() { Dispatchers.resetMain() }

    private fun vm() = SpotRegistrationViewModel(mySpotService, locationService)

    private fun address() = AddressSuggestion(
        name = "한강공원",
        fullAddress = "서울 동작구 한강대로",
        latitude = 37.5,
        longitude = 127.0,
    )

    private fun image() = ImagePayload(
        bytes = byteArrayOf(1, 2, 3),
        mimeType = "image/jpeg",
        filename = "spot.jpg",
    )

    private fun rejectedDetail() = MySpotDetail(
        id = 41L,
        name = "기존 노을 스팟",
        theme = SpotTheme.YUNSEUL,
        imageUrl = "https://cdn.example.com/41.jpg",
        latitude = 37.55,
        longitude = 127.01,
        address = "서울특별시 용산구 노을길 41",
        capturedDate = "2026-05-20",
        capturedTime = "19:40",
        comment = "기존 코멘트",
        status = MySpotStatus.REJECTED,
        rejection = SpotRejection(
            reason = RejectionReason.LOW_QUALITY,
            reasonLabel = "사진 상태 불량",
            guideMessage = "사진이 흐려요",
            detail = null,
            rejectedAt = "2026-08-06T10:00:00Z",
        ),
        recommendationCount = 3L,
        isRecommended = false,
        source = SpotSource.User,
        updatedAt = "2026-08-06T10:00:00Z",
    )

    /** 필수값(사진/이름/주소/테마/날짜/시간)을 모두 채운다. */
    private fun SpotRegistrationViewModel.fillRequired() {
        setImagePayload(image(), previewUri = null)
        setSpotName("Cafe")
        applyAddressSelection(address())
        toggleTheme(SpotTheme.YUNSEUL)
        setCapturedDate(LocalDate.of(2026, 5, 21))
        setCapturedTime(LocalTime.of(14, 30))
    }

    @Test
    fun `isRegisterEnabled false until all required fields set`() = runTest(testDispatcher) {
        val vm = vm()
        vm.isRegisterEnabled.test {
            assertFalse(awaitItem())
            vm.fillRequired()
            advanceUntilIdle()
            // 마지막 필수값 입력 후 true 로 전환.
            var enabled = false
            while (!enabled) { enabled = awaitItem() }
            assertTrue(enabled)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setSpotName clamps to 20 chars and comment to 50`() {
        val vm = vm()
        vm.setSpotName("a".repeat(40))
        assertEquals(SpotRegistrationViewModel.MAX_NAME_LENGTH, vm.spotName.value.length)
        vm.setComment("b".repeat(80))
        assertEquals(SpotRegistrationViewModel.MAX_COMMENT_LENGTH, vm.comment.value.length)
    }

    @Test
    fun `setCapturedDate clamps future date to today`() {
        val vm = vm()
        vm.setCapturedDate(LocalDate.now().plusDays(3))
        assertEquals(LocalDate.now(), vm.capturedDate.value)
    }

    @Test
    fun `submit fails fast when invalid`() = runTest(testDispatcher) {
        val vm = vm()
        vm.submit()
        advanceUntilIdle()
        assertTrue(vm.submission.value is LoadState.Failed)
    }

    @Test
    fun `create mode submit emits draft result`() = runTest(testDispatcher) {
        val captured = slot<SpotDraft>()
        coEvery { mySpotService.create(capture(captured), any()) } returns CreateMySpotResult(
            spotId = 42L,
            status = MySpotStatus.DRAFT,
            imageUrl = "https://s3/42.jpg",
        )

        val vm = vm()
        assertEquals(SpotRegistrationMode.CREATE, vm.mode.value)
        vm.fillRequired()
        vm.submit()
        advanceUntilIdle()

        val state = vm.submission.value as LoadState.Loaded
        assertEquals(42L, state.value.spotId)
        assertEquals(MySpotStatus.DRAFT, state.value.status)

        // 드래프트는 서버 포맷(yyyy-MM-dd / HH:mm)으로 직렬화된다.
        assertEquals("Cafe", captured.captured.name)
        assertEquals(SpotTheme.YUNSEUL, captured.captured.theme)
        assertEquals("2026-05-21", captured.captured.capturedDate)
        assertEquals("14:30", captured.captured.capturedTime)
        coVerify(exactly = 1) { mySpotService.create(any(), any()) }
    }

    @Test
    fun `loadRevision switches mode and prefills every rejected detail field`() = runTest(testDispatcher) {
        val detail = rejectedDetail()
        coEvery { mySpotService.detail(41L) } returns detail
        val vm = vm()

        vm.loadRevision(41L)
        advanceUntilIdle()

        assertEquals(SpotRegistrationMode.REVISE, vm.mode.value)
        assertEquals(LoadState.Loaded(detail), vm.revisionLoadState.value)
        assertEquals(detail.name, vm.spotName.value)
        assertEquals(detail.theme, vm.theme.value)
        assertEquals(detail.address, vm.selectedAddress.value?.fullAddress)
        assertEquals(detail.latitude, vm.selectedAddress.value?.latitude)
        assertEquals(detail.longitude, vm.selectedAddress.value?.longitude)
        assertEquals(LocalDate.of(2026, 5, 20), vm.capturedDate.value)
        assertEquals(LocalTime.of(19, 40), vm.capturedTime.value)
        assertEquals(detail.comment, vm.comment.value)
        assertEquals(detail.imageUrl, vm.existingImageUrl.value)
        assertEquals(null, vm.imagePayload.value)
    }

    @Test
    fun `existing server image enables revise submission without local payload`() =
        runTest(testDispatcher) {
            coEvery { mySpotService.detail(41L) } returns rejectedDetail()
            val vm = vm()

            vm.isRegisterEnabled.test {
                assertFalse(awaitItem())
                vm.loadRevision(41L)
                advanceUntilIdle()

                var enabled = false
                while (!enabled) enabled = awaitItem()
                assertTrue(enabled)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `revise submit without replacement keeps existing server image`() = runTest(testDispatcher) {
        coEvery { mySpotService.detail(41L) } returns rejectedDetail()
        coEvery { mySpotService.update(41L, any(), null) } returns
            MySpotUpdateResult(
                spotId = 41L,
                status = MySpotStatus.REJECTED,
                imageUrl = "https://cdn.example.com/41.jpg",
            )
        coEvery { mySpotService.requestOpen(41L) } returns
            MySpotTransitionResult(
                spotId = 41L,
                status = MySpotStatus.RE_REVIEW_PENDING,
                updatedAt = "2026-08-06T10:01:00Z",
            )
        val vm = vm()
        vm.loadRevision(41L)
        advanceUntilIdle()

        vm.setSpotName("보완한 노을 스팟")
        vm.submit()
        advanceUntilIdle()

        val state = vm.submission.value as LoadState.Loaded
        assertEquals(MySpotStatus.RE_REVIEW_PENDING, state.value.status)
        assertEquals("https://cdn.example.com/41.jpg", vm.existingImageUrl.value)
        coVerify(exactly = 1) { mySpotService.update(41L, any(), null) }
        coVerify(exactly = 1) { mySpotService.requestOpen(41L) }
        coVerify(exactly = 0) { mySpotService.create(any(), any()) }
    }

    @Test
    fun `revise submit sends only newly selected replacement image`() = runTest(testDispatcher) {
        val replacement = image().copy(filename = "replacement.jpg")
        coEvery { mySpotService.detail(41L) } returns rejectedDetail()
        coEvery { mySpotService.update(41L, any(), replacement) } returns
            MySpotUpdateResult(
                spotId = 41L,
                status = MySpotStatus.REJECTED,
                imageUrl = "https://cdn.example.com/41.jpg",
            )
        coEvery { mySpotService.requestOpen(41L) } returns
            MySpotTransitionResult(
                spotId = 41L,
                status = MySpotStatus.RE_REVIEW_PENDING,
                updatedAt = "2026-08-06T10:01:00Z",
            )
        val vm = vm()
        vm.loadRevision(41L)
        advanceUntilIdle()

        vm.setImagePayload(replacement, previewUri = "content://replacement")
        vm.submit()
        advanceUntilIdle()

        assertEquals("content://replacement", vm.selectedImageUri.value)
        coVerify(exactly = 1) { mySpotService.update(41L, any(), replacement) }
        coVerify(exactly = 1) { mySpotService.requestOpen(41L) }
    }

    @Test
    fun `resubmit retry after saved revision does not send update twice`() = runTest(testDispatcher) {
        coEvery { mySpotService.detail(41L) } returns rejectedDetail()
        coEvery { mySpotService.update(41L, any(), null) } returns
            MySpotUpdateResult(
                spotId = 41L,
                status = MySpotStatus.REJECTED,
                imageUrl = "https://cdn.example.com/41.jpg",
            )
        coEvery { mySpotService.requestOpen(41L) } throws RuntimeException("network") andThen
            MySpotTransitionResult(
                spotId = 41L,
                status = MySpotStatus.RE_REVIEW_PENDING,
                updatedAt = "2026-08-06T10:01:00Z",
            )
        val vm = vm()
        vm.loadRevision(41L)
        advanceUntilIdle()
        vm.setSpotName("보완한 노을 스팟")

        vm.submit()
        advanceUntilIdle()

        // 수정은 저장됐고 재신청만 실패한 상태.
        assertTrue(vm.submission.value is LoadState.Failed)
        assertTrue(vm.isRevisionSaved.value)

        vm.submit()
        advanceUntilIdle()

        val state = vm.submission.value as LoadState.Loaded
        assertEquals(MySpotStatus.RE_REVIEW_PENDING, state.value.status)
        assertFalse(vm.isRevisionSaved.value)
        coVerify(exactly = 1) { mySpotService.update(41L, any(), null) }
        coVerify(exactly = 2) { mySpotService.requestOpen(41L) }
    }

    @Test
    fun `revise failure keeps prefilled form and replacement selection`() = runTest(testDispatcher) {
        val replacement = image().copy(filename = "replacement.jpg")
        coEvery { mySpotService.detail(41L) } returns rejectedDetail()
        coEvery { mySpotService.update(41L, any(), replacement) } throws
            RuntimeException("network")
        val vm = vm()
        vm.loadRevision(41L)
        advanceUntilIdle()
        vm.setSpotName("수정 중인 이름")
        vm.setImagePayload(replacement, previewUri = "content://replacement")

        vm.submit()
        advanceUntilIdle()

        assertTrue(vm.submission.value is LoadState.Failed)
        assertEquals("수정 중인 이름", vm.spotName.value)
        assertEquals(replacement, vm.imagePayload.value)
        assertEquals("https://cdn.example.com/41.jpg", vm.existingImageUrl.value)
    }
}
