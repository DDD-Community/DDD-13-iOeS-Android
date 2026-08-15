package com.pickflow.android.feature.spotdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pickflow.android.common.designsystem.PickflowColors
import com.pickflow.android.common.designsystem.PickflowTypography
import com.pickflow.android.common.ui.LoadState
import com.pickflow.android.core.services.protocols.MySpotDetail
import com.pickflow.android.core.services.protocols.ReviewDecision
import com.pickflow.android.feature.home.ReviewResultViewModel
import com.pickflow.android.feature.spotdetail.components.LoginPromptPopup
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/** Stub-first PV-41 상세의 ViewModel orchestration 경계. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpotOpenScreen(
    spotId: Long,
    onBack: () -> Unit,
    onRevise: (Long) -> Unit,
    onDeleted: () -> Unit,
    onRequireLogin: () -> Unit,
    showRegisteredToast: Boolean = false,
    openViewModel: SpotOpenViewModel = hiltViewModel(),
    recommendationViewModel: SpotRecommendationViewModel = hiltViewModel(),
    reviewResultViewModel: ReviewResultViewModel = hiltViewModel(),
    feedbackViewModel: SpotOpenFeedbackViewModel = hiltViewModel(),
) {
    val detailState by openViewModel.detail.collectAsStateWithLifecycle()
    val isTransitionInFlight by openViewModel.isTransitionInFlight.collectAsStateWithLifecycle()
    val openToast by openViewModel.toast.collectAsStateWithLifecycle()
    val recommendation by recommendationViewModel.uiState.collectAsStateWithLifecycle()
    val recommendationLoginRequired by recommendationViewModel.isLoginRequired.collectAsStateWithLifecycle()
    val recommendationToast by recommendationViewModel.toast.collectAsStateWithLifecycle()
    val reviewStatus by reviewResultViewModel.status.collectAsStateWithLifecycle()
    val feedbackDraft by feedbackViewModel.draft.collectAsStateWithLifecycle()
    val feedbackToast by feedbackViewModel.toast.collectAsStateWithLifecycle()
    var showReportSheet by remember { mutableStateOf(false) }

    LaunchedEffect(spotId) {
        openViewModel.load(spotId)
        reviewResultViewModel.load()
        if (showRegisteredToast) openViewModel.showRegisteredToast()
    }
    val loadedDetail = (detailState as? LoadState.Loaded<MySpotDetail>)?.value
    LaunchedEffect(
        loadedDetail?.id,
        loadedDetail?.recommendationCount,
        loadedDetail?.isRecommended,
    ) {
        loadedDetail?.let { detail ->
            recommendationViewModel.initialize(
                spotId = detail.id,
                recommendationCount = detail.recommendationCount,
                isRecommended = detail.isRecommended,
            )
        }
    }
    LaunchedEffect(openViewModel) {
        openViewModel.deletedSpotIds.collectLatest { onDeleted() }
    }

    val renderedState = if (
        loadedDetail != null && recommendation.spotId == loadedDetail.id
    ) {
        LoadState.Loaded(
            loadedDetail.copy(
                recommendationCount = recommendation.recommendationCount,
                isRecommended = recommendation.isRecommended,
            ),
        )
    } else {
        detailState
    }
    val publishedResult = (reviewStatus as? LoadState.Loaded)
        ?.value
        ?.unacknowledgedResults
        ?.firstOrNull { result ->
            result.spotId == spotId &&
                result.decision == ReviewDecision.APPROVED &&
                !result.publishedModalAcknowledged
        }

    Box(modifier = Modifier.fillMaxSize()) {
        SpotOpenDetailContent(
            state = renderedState,
            isTransitionInFlight = isTransitionInFlight,
            isRecommendationInFlight = recommendation.isInFlight,
            showPublishedModal = publishedResult != null,
            onBack = onBack,
            onRequestOpen = openViewModel::requestOpen,
            onWithdrawRequest = openViewModel::unpublish,
            onWithdrawRejection = openViewModel::withdrawRejection,
            onRevise = onRevise,
            onCancelOpen = openViewModel::unpublish,
            onDelete = openViewModel::delete,
            onToggleRecommendation = recommendationViewModel::toggleRecommendation,
            onAcknowledgePublishedModal = {
                publishedResult?.let { reviewResultViewModel.acknowledgePublishedModal(it.resultId) }
            },
            onReport = { showReportSheet = true },
        )

        val toast = openToast ?: recommendationToast ?: feedbackToast
        toast?.let { message ->
            Text(
                text = message,
                style = PickflowTypography.bodyMediumBold,
                color = PickflowColors.gray95,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(24.dp)
                    .background(PickflowColors.gray0)
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .testTag("spot-open-toast"),
            )
            LaunchedEffect(message) {
                delay(3_000)
                openViewModel.consumeToast()
                recommendationViewModel.consumeToast()
                feedbackViewModel.consumeToast()
            }
        }

        if (recommendationLoginRequired) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable { recommendationViewModel.dismissLoginRequired() }
                    .testTag("spot-recommendation-login"),
            ) {
                LoginPromptPopup(
                    onCancel = recommendationViewModel::dismissLoginRequired,
                    onLogin = {
                        recommendationViewModel.dismissLoginRequired()
                        onRequireLogin()
                    },
                    isClosable = true,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 32.dp)
                        .clickable(enabled = false) {},
                )
            }
        }
    }

    if (showReportSheet && loadedDetail != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val scope = rememberCoroutineScope()
        ModalBottomSheet(
            onDismissRequest = { showReportSheet = false },
            sheetState = sheetState,
            containerColor = PickflowColors.gray95,
            contentColor = PickflowColors.gray0,
        ) {
            ReportSheetBody(
                text = feedbackDraft,
                onTextChange = feedbackViewModel::setDraft,
                onClose = {
                    feedbackViewModel.clearDraft()
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        showReportSheet = false
                    }
                },
                onSubmit = {
                    feedbackViewModel.submit(loadedDetail.id)
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        showReportSheet = false
                    }
                },
            )
        }
    }
}
