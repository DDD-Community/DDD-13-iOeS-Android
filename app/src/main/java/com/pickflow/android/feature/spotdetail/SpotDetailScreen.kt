package com.pickflow.android.feature.spotdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pickflow.android.common.designsystem.PickflowColors
import com.pickflow.android.common.designsystem.PickflowTypography
import com.pickflow.android.common.ui.LoadState
import com.pickflow.android.core.services.protocols.SpotDetail
import com.pickflow.android.feature.spotdetail.components.FullscreenImageViewer
import com.pickflow.android.feature.spotdetail.components.LoginPromptPopup
import com.pickflow.android.feature.spotdetail.components.MySpotComingSoonSheet
import com.pickflow.android.feature.spotdetail.components.ReportButton
import com.pickflow.android.feature.spotdetail.components.SpotActionButtons
import com.pickflow.android.feature.spotdetail.components.SpotDetailNavBar
import com.pickflow.android.feature.spotdetail.components.SpotHeaderSection
import com.pickflow.android.feature.spotdetail.components.SpotPhotoSection
import com.pickflow.android.feature.spotdetail.components.SpotRealTimeInfoSection
import com.pickflow.android.feature.spotdetail.components.toDetailData
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * iOS `SpotDetailView.swift` 1:1 이식.
 *
 * 구조:
 *  - gray95 Box → Column { NavBar, content(state) }
 *  - content: Idle/Loading → ProgressView, Failed/Empty → 안내, Loaded → Header/Photo/Actions/RealTime/Report 스크롤
 *  - overlays: ReportSheet(ModalBottomSheet), 신고 완료 토스트
 *
 * iOS의 LoginPrompt / Toast(viewModel.toast) / dismissRequested / preview 상태는
 * 현 ViewModel에 대응 필드가 아직 없어 본 화면에서는 보류한다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpotDetailScreen(
    spotId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onRequireLogin: () -> Unit = {},
    showRegisteredToast: Boolean = false,
    /**
     * 신고/오픈알림 등 내부 모달 시트 열림 여부 통지 — 지도 바텀시트에 임베드될 때
     * 키보드 리사이즈로 인한 외부 시트 앵커 변동을 무시하기 위한 신호.
     */
    onOverlaySheetVisible: (Boolean) -> Unit = {},
    viewModel: SpotDetailViewModel = hiltViewModel(),
    actionsViewModel: SpotDetailActionsViewModel = hiltViewModel(),
) {
    val spotState by viewModel.spot.collectAsStateWithLifecycle()
    val bookmarked by viewModel.bookmarked.collectAsStateWithLifecycle()
    val toastMessage by viewModel.toast.collectAsStateWithLifecycle()
    val isLoginRequired by viewModel.isLoginRequired.collectAsStateWithLifecycle()
    val reportDraft by viewModel.reportDraft.collectAsStateWithLifecycle()

    var isReportSheetOpen by remember { mutableStateOf(false) }
    var isComingSoonSheetOpen by remember { mutableStateOf(false) }
    var toastVisible by remember { mutableStateOf(false) }
    var fullscreenImageUrl by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(spotId) { viewModel.load(spotId) }
    LaunchedEffect(Unit) {
        if (showRegisteredToast) viewModel.showRegisteredToast()
    }
    LaunchedEffect(isReportSheetOpen, isComingSoonSheetOpen) {
        onOverlaySheetVisible(isReportSheetOpen || isComingSoonSheetOpen)
    }
    LaunchedEffect(toastMessage) {
        if (toastMessage != null) {
            toastVisible = true
            delay(3000)
            toastVisible = false
            viewModel.consumeToast()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PickflowColors.gray95)
            .testTag("spotdetail-screen"),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                // targetSdk 35(edge-to-edge 강제)에서 하단 콘텐츠가 내비게이션 바에 가려지지 않도록.
                // (예: 3버튼 내비게이션의 갤럭시 S24 울트라에서 "잘못된 정보가 있나요?" 잘림 제보)
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            SpotDetailNavBar(
                onBack = onBack,
                onShare = viewModel::share,
                onClose = onBack,
            )

            when (val state = spotState) {
                LoadState.Idle, LoadState.Loading -> LoadingBody()
                LoadState.Empty -> ErrorBody(message = "스팟 정보를 찾을 수 없어요.")
                is LoadState.Failed -> ErrorBody(message = state.error.message ?: "")
                is LoadState.Loaded -> LoadedBody(
                    spot = state.value,
                    isBookmarked = bookmarked,
                    onRoute = { actionsViewModel.openInMap(state.value) },
                    onBookmark = viewModel::toggleBookmark,
                    onOpenSpot = { isComingSoonSheetOpen = true },
                    onReport = { isReportSheetOpen = true },
                    onImageClick = { fullscreenImageUrl = state.value.imageUrl },
                )
            }
        }

        if (toastVisible) {
            ReportSubmittedToast(
                message = toastMessage ?: "제보가 접수되었습니다.",
                modifier = Modifier.align(Alignment.Center),
            )
        }

        if (isLoginRequired) {
            // iOS `isLoginRequired` overlay 1:1 — 검은 50% 딤 + 탭하면 닫힘 + LoginPromptPopup.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable { viewModel.dismissLoginRequired() }
                    .testTag("spotdetail-login-overlay"),
            ) {
                LoginPromptPopup(
                    onCancel = viewModel::dismissLoginRequired,
                    onLogin = {
                        viewModel.dismissLoginRequired()
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

    fullscreenImageUrl?.let { url ->
        FullscreenImageViewer(
            imageUrl = url,
            contentDescription = null,
            onDismiss = { fullscreenImageUrl = null },
        )
    }

    if (isComingSoonSheetOpen) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val scope = rememberCoroutineScope()
        ModalBottomSheet(
            onDismissRequest = { isComingSoonSheetOpen = false },
            sheetState = sheetState,
            containerColor = PickflowColors.gray95,
            contentColor = PickflowColors.gray0,
        ) {
            MySpotComingSoonSheet(
                onCancel = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        isComingSoonSheetOpen = false
                    }
                },
                onNotify = {
                    viewModel.notifyUpdateRequested()
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        isComingSoonSheetOpen = false
                    }
                },
            )
        }
    }

    if (isReportSheetOpen) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val scope = rememberCoroutineScope()
        ModalBottomSheet(
            onDismissRequest = { isReportSheetOpen = false },
            sheetState = sheetState,
            containerColor = PickflowColors.gray95,
            contentColor = PickflowColors.gray0,
        ) {
            ReportSheetBody(
                text = reportDraft,
                onTextChange = viewModel::setReportDraft,
                onClose = {
                    viewModel.clearReportDraft()
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        isReportSheetOpen = false
                    }
                },
                onSubmit = { text ->
                    viewModel.reportInvalidInfo(text)
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        isReportSheetOpen = false
                    }
                },
            )
        }
    }
}

@Composable
private fun LoadingBody() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("spotdetail-loading"),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = PickflowColors.gray0, strokeWidth = 2.dp)
    }
}

@Composable
private fun ErrorBody(message: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .testTag("spotdetail-error"),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "스팟 정보를 불러오지 못했어요.",
            style = PickflowTypography.bodyLargeBold,
            color = PickflowColors.gray0,
        )
        if (message.isNotBlank()) {
            Text(
                text = message,
                style = PickflowTypography.bodySmall,
                color = PickflowColors.gray50,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun LoadedBody(
    spot: SpotDetail,
    isBookmarked: Boolean,
    onRoute: () -> Unit,
    onBookmark: () -> Unit,
    onOpenSpot: () -> Unit,
    onReport: () -> Unit,
    onImageClick: () -> Unit,
) {
    val data = spot.toDetailData(isBookmarked)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        SpotHeaderSection(spot = data)
        SpotPhotoSection(spot = data, onImageClick = onImageClick)
        SpotActionButtons(
            isMine = data.isMine,
            isBookmarked = isBookmarked,
            onRoute = onRoute,
            onBookmark = onBookmark,
            onOpenSpot = onOpenSpot,
        )
        SpotRealTimeInfoSection(spot = data)
        ReportButton(onClick = onReport)
    }
}

/** iOS `viewModel.toast`(체크 아이콘 + 텍스트, gray0 배경) 1:1. */
@Composable
private fun ReportSubmittedToast(
    message: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .shadow(elevation = 12.dp, shape = RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .background(PickflowColors.gray0)
            .padding(horizontal = 20.dp, vertical = 14.dp)
            .testTag("spotdetail-toast"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = PickflowColors.gray95,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = message,
            style = PickflowTypography.bodyMediumBold,
            color = PickflowColors.gray95,
        )
    }
}

/**
 * iOS `ReportSheet.swift` 1:1 — 최소 5자, 최대 200자 신고 본문 입력.
 * 입력 상태는 [SpotDetailViewModel.reportDraft] 로 호이스팅 — 키보드 리사이즈 등으로
 * 시트가 recomposition 되어도 입력/등록 버튼 활성 상태가 유지된다.
 */
@Composable
private fun ReportSheetBody(
    text: String,
    onTextChange: (String) -> Unit,
    onClose: () -> Unit,
    onSubmit: (String) -> Unit,
) {
    val minLength = SpotDetailViewModel.REPORT_MIN_LENGTH
    val maxLength = SpotDetailViewModel.REPORT_MAX_LENGTH
    val placeholder = "실제 위치가 지도와 달라요, 현재 공사 중이라 출입이 안 돼요 등 상세한 내용을 적어주세요 (최소 5자 이상)"
    val isSubmittable = text.trim().length >= minLength

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 12.dp, bottom = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "닫기",
                tint = PickflowColors.gray0,
                modifier = Modifier
                    .size(24.dp)
                    .clickable(onClick = onClose)
                    .testTag("report-close"),
            )
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text(
                    text = "잘못된 정보가 있나요?",
                    style = PickflowTypography.bodyLargeBold,
                    color = PickflowColors.gray0,
                )
            }
            Text(
                text = "등록",
                style = PickflowTypography.bodyLargeBold,
                color = if (isSubmittable) PickflowColors.sunsetOrange else PickflowColors.gray50,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .then(
                        if (isSubmittable) Modifier.clickable { onSubmit(text) } else Modifier
                    )
                    .testTag("report-submit"),
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(PickflowColors.gray90)
                .padding(16.dp),
        ) {
            BasicTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 140.dp)
                    .testTag("report-text"),
                textStyle = TextStyle(
                    color = PickflowColors.gray0,
                    fontSize = PickflowTypography.bodyMedium.fontSize,
                ),
                cursorBrush = SolidColor(PickflowColors.sunsetOrange),
            )
            if (text.isEmpty()) {
                Text(
                    text = placeholder,
                    style = PickflowTypography.bodyMedium,
                    color = PickflowColors.gray50,
                )
            }
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp),
            ) {
                Text(
                    text = "${text.length}",
                    style = PickflowTypography.bodySmall,
                    color = PickflowColors.gray0,
                )
                Text(
                    text = "/$maxLength",
                    style = PickflowTypography.bodySmall,
                    color = PickflowColors.gray50,
                )
            }
        }
    }

}
