package com.pickflow.android.feature.debug

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pickflow.android.core.services.protocols.ReviewDecision
import com.pickflow.android.core.services.stub.StubOperation
import com.pickflow.android.core.services.stub.StubScenario
import com.pickflow.android.core.services.stub.StubSpotBackend
import com.pickflow.android.core.services.stub.StubSpotFixtures
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SpotOpenDebugFixture(val label: String, val spotId: Long)

@HiltViewModel
class DebugViewModel @Inject constructor(
    private val backend: StubSpotBackend,
) : ViewModel() {
    val fixtures = listOf(
        SpotOpenDebugFixture("DRAFT", StubSpotFixtures.DRAFT_SPOT_ID),
        SpotOpenDebugFixture("PENDING", StubSpotFixtures.PENDING_SPOT_ID),
        SpotOpenDebugFixture("RE_REVIEW_PENDING", StubSpotFixtures.RE_REVIEW_PENDING_SPOT_ID),
        SpotOpenDebugFixture("REJECTED", StubSpotFixtures.REJECTED_SPOT_ID),
        SpotOpenDebugFixture("PUBLISHED user", StubSpotFixtures.PUBLISHED_USER_SPOT_ID),
        SpotOpenDebugFixture("PUBLISHED curated", StubSpotFixtures.CURATED_SPOT_ID),
    )

    private val _message = MutableStateFlow("초기 성공 fixture")
    val message: StateFlow<String> = _message.asStateFlow()

    fun reset() = launch("초기 fixture로 복원했어요") { backend.reset() }

    fun failNextOpenRequest() {
        backend.configure(StubScenario.failure(StubOperation.REQUEST_OPEN))
        _message.value = "다음 오픈 신청: 실패"
    }

    fun delayNextOpenRequest() {
        backend.configure(StubScenario.delayed(StubOperation.REQUEST_OPEN, 1_000))
        _message.value = "다음 오픈 신청: 1초 지연"
    }

    fun raceNextWithdrawal(decision: ReviewDecision) {
        backend.configure(StubScenario.withdrawalReviewRace(decision))
        _message.value = "다음 신청 철회: 검수 ${decision.name} 경합"
    }

    fun approvePending() = launch("PENDING fixture를 승인했어요") {
        backend.completeReview(StubSpotFixtures.PENDING_SPOT_ID, ReviewDecision.APPROVED)
    }

    fun rejectPending() = launch("PENDING fixture를 반려했어요") {
        backend.completeReview(
            StubSpotFixtures.PENDING_SPOT_ID,
            ReviewDecision.REJECTED,
            "사진에서 스팟을 확인하기 어려워요",
        )
    }

    private fun launch(success: String, block: suspend () -> Unit) {
        viewModelScope.launch {
            runCatching { block() }
                .onSuccess { _message.value = success }
                .onFailure { _message.value = it.message ?: "fixture 변경에 실패했어요" }
        }
    }
}
