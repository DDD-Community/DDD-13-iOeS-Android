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
import com.pickflow.android.core.services.protocols.MySpotStatus
import com.pickflow.android.core.services.protocols.SpotRejection
import androidx.compose.ui.text.style.TextDecoration
import com.pickflow.android.core.services.protocols.SpotDetail
import com.pickflow.android.feature.spotdetail.components.FullscreenImageViewer
import com.pickflow.android.feature.spotdetail.components.LoginPromptPopup
import com.pickflow.android.feature.spotdetail.components.MySpotComingSoonSheet
import com.pickflow.android.feature.spotdetail.components.ReportButton
import com.pickflow.android.feature.spotdetail.components.SpotActionButtons
import com.pickflow.android.feature.spotdetail.components.SpotDetailNavBar
import com.pickflow.android.feature.spotdetail.components.SpotHeaderSection
import com.pickflow.android.feature.spotdetail.components.SpotOpenConfirmOverlay
import com.pickflow.android.feature.spotdetail.components.SpotOpenSheet
import com.pickflow.android.feature.spotdetail.components.openActionSheet
import com.pickflow.android.core.services.protocols.ReviewDecision
import com.pickflow.android.feature.home.ReviewResultViewModel
import com.pickflow.android.feature.spotdetail.components.SpotPublishedOverlay
import com.pickflow.android.feature.spotdetail.components.SpotPublishToggle
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
    /**
     * 반려 후 "다시 신청하기" — 보완 폼으로 이동한다.
     * null 이면 오픈 플로우를 쓰지 않는 임베드 모드로 보고 기존 준비중 안내 시트를 띄운다.
     */
    onReviseMySpot: ((Long) -> Unit)? = null,
    /** 삭제 완료 후 이동(보통 뒤로가기). */
    onSpotDeleted: (() -> Unit)? = null,
    showRegisteredToast: Boolean = false,
    /**
     * 신고/오픈알림 등 내부 모달 시트 열림 여부 통지 — 지도 바텀시트에 임베드될 때
     * 키보드 리사이즈로 인한 외부 시트 앵커 변동을 무시하기 위한 신호.
     */
    onOverlaySheetVisible: (Boolean) -> Unit = {},
    viewModel: SpotDetailViewModel = hiltViewModel(),
    actionsViewModel: SpotDetailActionsViewModel = hiltViewModel(),
    openActionsViewModel: SpotOpenActionsViewModel = hiltViewModel(),
    reviewResultViewModel: ReviewResultViewModel = hiltViewModel(),
) {
    val spotState by viewModel.spot.collectAsStateWithLifecycle()
    val bookmarked by viewModel.bookmarked.collectAsStateWithLifecycle()
    val liked by viewModel.liked.collectAsStateWithLifecycle()
    val toastMessage by viewModel.toast.collectAsStateWithLifecycle()
    val isLoginRequired by viewModel.isLoginRequired.collectAsStateWithLifecycle()
    val reportDraft by viewModel.reportDraft.collectAsStateWithLifecycle()

    val isOpenActionInFlight by openActionsViewModel.isInFlight.collectAsStateWithLifecycle()
    val isReleased by openActionsViewModel.isReleased.collectAsStateWithLifecycle()
    val openActionToast by openActionsViewModel.toast.collectAsStateWithLifecycle()
    val reviewStatus by reviewResultViewModel.status.collectAsStateWithLifecycle()
    var activeOpenSheet by remember { mutableStateOf<SpotOpenSheet?>(null) }
    // 반려 배너 닫기는 서버 상태를 바꾸지 않는다(REJECTED 는 이미 나만보기다).
    // 세션 한정이라 화면을 다시 열면 배너가 복귀한다. docs/PV-41/10-open-questions.md A1
    var isRejectionDismissed by remember(spotId) { mutableStateOf(false) }
    var isReportSheetOpen by remember { mutableStateOf(false) }
    var isComingSoonSheetOpen by remember { mutableStateOf(false) }
    var toastVisible by remember { mutableStateOf(false) }
    var fullscreenImageUrl by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(spotId) {
        viewModel.load(spotId)
        spotId.toLongOrNull()?.let(openActionsViewModel::loadReleased)
        if (onReviseMySpot != null) reviewResultViewModel.load()
    }
    LaunchedEffect(Unit) {
        if (showRegisteredToast) viewModel.showRegisteredToast()
    }
    LaunchedEffect(isReportSheetOpen, isComingSoonSheetOpen) {
        onOverlaySheetVisible(isReportSheetOpen || isComingSoonSheetOpen)
    }
    // 상태 전이가 끝나면 상세를 다시 읽는다 — 배지·버튼 문구가 새 상태를 따라가야 한다.
    LaunchedEffect(openActionsViewModel, spotId) {
        openActionsViewModel.statusChanges.collect { viewModel.load(spotId) }
    }
    LaunchedEffect(openActionsViewModel) {
        openActionsViewModel.deleted.collect { onSpotDeleted?.invoke() }
    }
    LaunchedEffect(openActionToast) {
        openActionToast?.let {
            viewModel.showToast(it)
            openActionsViewModel.consumeToast()
        }
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
                    isLiked = liked,
                    onRoute = { actionsViewModel.openInMap(state.value) },
                    onBookmark = viewModel::toggleBookmark,
                    onLike = viewModel::toggleLike,
                    onOpenSpot = {
                        val status = state.value.mySpotStatus
                        when {
                            // 오픈 플로우를 쓰지 않는 임베드 모드 — 기존 준비중 안내 유지.
                            onReviseMySpot == null -> isComingSoonSheetOpen = true
                            // 반려는 확인 없이 보완 폼으로 바로 보낸다.
                            status == MySpotStatus.REJECTED -> onReviseMySpot(state.value.id)
                            else -> activeOpenSheet = status.openActionSheet()
                        }
                    },
                    onDeleteSpot = { activeOpenSheet = SpotOpenSheet.DELETE }
                        .takeIf { onReviseMySpot != null },
                    isOpenActionInFlight = isOpenActionInFlight,
                    onReport = { viewModel.requestReport { isReportSheetOpen = true } },
                    onImageClick = { fullscreenImageUrl = state.value.imageUrl },
                    isRejectionDismissed = isRejectionDismissed,
                    onDismissRejection = { isRejectionDismissed = true },
                    onRevise = { onReviseMySpot?.invoke(state.value.id) },
                    isReleased = isReleased,
                    onToggleRelease = { openActionsViewModel.setReleased(state.value.id, it) },
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

    val publishedResult = (reviewStatus as? LoadState.Loaded)
        ?.value
        ?.unacknowledgedResults
        ?.firstOrNull { result ->
            result.spotId.toString() == spotId &&
                result.decision == ReviewDecision.APPROVED &&
                !result.publishedModalAcknowledged
        }
    publishedResult?.let { result ->
        SpotPublishedOverlay(
            onConfirm = { reviewResultViewModel.acknowledgePublishedModal(result.resultId) },
        )
    }

    activeOpenSheet?.let { sheet ->
        val spot = (spotState as? LoadState.Loaded<SpotDetail>)?.value
        SpotOpenConfirmOverlay(
            sheet = sheet,
            onDismiss = { activeOpenSheet = null },
            onConfirm = {
                activeOpenSheet = null
                val id = spot?.id ?: return@SpotOpenConfirmOverlay
                when (sheet) {
                    SpotOpenSheet.REQUEST_OPEN -> openActionsViewModel.requestOpen(id)
                    SpotOpenSheet.WITHDRAW_REQUEST,
                    SpotOpenSheet.CANCEL_OPEN,
                    -> openActionsViewModel.unpublish(id)
                    SpotOpenSheet.DELETE -> openActionsViewModel.delete(id)
                    SpotOpenSheet.LOGIN -> onRequireLogin()
                }
            },
        )
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
    isLiked: Boolean,
    onRoute: () -> Unit,
    onBookmark: () -> Unit,
    onLike: () -> Unit,
    onOpenSpot: () -> Unit,
    onReport: () -> Unit,
    onImageClick: () -> Unit,
    onDeleteSpot: (() -> Unit)?,
    isOpenActionInFlight: Boolean,
    isRejectionDismissed: Boolean,
    onDismissRejection: () -> Unit,
    onRevise: () -> Unit,
    isReleased: Boolean,
    onToggleRelease: (Boolean) -> Unit,
) {
    val data = spot.toDetailData(isBookmarked, isLiked)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        // 반려 배너는 헤더보다 위 — 화면을 열자마자 사유와 다음 행동이 먼저 보여야 한다.
        if (data.mySpotStatus == MySpotStatus.REJECTED && !isRejectionDismissed) {
            SpotRejectionBanner(
                rejection = data.rejection,
                onWithdraw = onDismissRejection,
                onRevise = onRevise,
            )
        }
        SpotHeaderSection(spot = data)
        SpotPhotoSection(spot = data, onImageClick = onImageClick)
        SpotActionButtons(
            // 반려는 배너 안 두 버튼이, 공개는 아래 공개 토글이 다음 행동을 맡는다.
            // 둘 다 하단 오픈 버튼과 중복이라 숨긴다.
            isMine = data.isMine && data.mySpotStatus !in BANNER_DRIVEN_STATUSES,
            mySpotStatus = data.mySpotStatus,
            isBookmarked = isBookmarked,
            isLikeable = data.isLikeable,
            isLiked = isLiked,
            onRoute = onRoute,
            onBookmark = onBookmark,
            onOpenSpot = onOpenSpot,
            onLike = onLike,
        )
        SpotRealTimeInfoSection(spot = data)
        if (data.isMine && data.mySpotStatus == MySpotStatus.PUBLISHED) {
            // 노출 on/off 는 재검수 없이 왕복되는 플래그라 확인 시트 없이 즉시 반영한다.
            SpotPublishToggle(
                isPublished = isReleased,
                enabled = !isOpenActionInFlight,
                onToggle = onToggleRelease,
            )
        }
        // 내가 등록한 스팟은 스스로 신고할 일이 없으므로 진입점 자체를 숨긴다.
        if (!data.isMine) ReportButton(onClick = onReport)
        if (data.isMine && onDeleteSpot != null) {
            Text(
                text = "스팟 삭제하기",
                style = PickflowTypography.bodyMedium.copy(
                    textDecoration = TextDecoration.Underline,
                ),
                color = PickflowColors.sunsetOrange,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !isOpenActionInFlight, onClick = onDeleteSpot)
                    .padding(vertical = 8.dp)
                    .testTag("detail-delete-spot"),
            )
        }
    }
}

/** 하단 오픈 버튼 대신 다른 UI 가 다음 행동을 맡는 상태들. */
private val BANNER_DRIVEN_STATUSES = setOf(MySpotStatus.REJECTED, MySpotStatus.PUBLISHED)

/**
 * 반려 배너. 사유와 다음 행동(철회 / 수정 후 재신청)을 한 덩어리로 보여준다.
 * 문구는 서버 `rejection` 값을 그대로 쓴다.
 */
@Composable
private fun SpotRejectionBanner(
    rejection: SpotRejection?,
    onWithdraw: () -> Unit,
    onRevise: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(PickflowColors.gray90)
            .background(Color(0x1FB83311))
            .padding(16.dp)
            .testTag("detail-rejection-banner"),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = rejectedAtLabel(rejection?.rejectedAt),
            style = PickflowTypography.bodySmall,
            color = PickflowColors.gray30,
        )
        Text(
            text = rejection?.let { it.guideMessage ?: it.reasonLabel }
                ?: "등록 정보를 다시 확인해주세요.",
            style = PickflowTypography.bodyMediumBold,
            color = PickflowColors.gray0,
        )
        Row(
            modifier = Modifier.padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            RejectionActionButton(
                text = "스팟 오픈 철회",
                background = PickflowColors.gray0,
                contentColor = PickflowColors.gray80,
                testTag = "detail-dismiss-rejection",
                modifier = Modifier.weight(1f),
                onClick = onWithdraw,
            )
            RejectionActionButton(
                text = "수정 후 재신청",
                background = PickflowColors.sunsetOrange,
                contentColor = PickflowColors.gray0,
                testTag = "detail-revise-spot",
                modifier = Modifier.weight(1f),
                onClick = onRevise,
            )
        }
    }
}

@Composable
private fun RejectionActionButton(
    text: String,
    background: Color,
    contentColor: Color,
    testTag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .heightIn(min = 52.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(background)
            .clickable(onClick = onClick)
            .testTag(testTag),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = PickflowTypography.bodyLargeBold,
            color = contentColor,
            textAlign = TextAlign.Center,
        )
    }
}

/** 서버 ISO 시각 → "26.07.21 반려됨". 파싱 못 하면 "반려됨" 만 남긴다. */
private fun rejectedAtLabel(rejectedAt: String?): String {
    val date = rejectedAt?.take(10)?.split("-")
        ?.takeIf { it.size == 3 && it[0].length == 4 }
        ?.let { (y, m, d) -> "${y.takeLast(2)}.$m.$d" }
    return listOfNotNull(date, "반려됨").joinToString(" ")
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
internal fun ReportSheetBody(
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
