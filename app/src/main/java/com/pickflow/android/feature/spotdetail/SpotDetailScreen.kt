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
    viewModel: SpotDetailViewModel = hiltViewModel(),
    actionsViewModel: SpotDetailActionsViewModel = hiltViewModel(),
) {
    val spotState by viewModel.spot.collectAsStateWithLifecycle()
    val bookmarked by viewModel.bookmarked.collectAsStateWithLifecycle()
    val reportSubmitted by viewModel.reportSubmitted.collectAsStateWithLifecycle()

    var isReportSheetOpen by remember { mutableStateOf(false) }
    var toastVisible by remember { mutableStateOf(false) }
    var fullscreenImageUrl by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(spotId) { viewModel.load(spotId) }
    LaunchedEffect(reportSubmitted) {
        if (reportSubmitted) {
            toastVisible = true
            delay(2000)
            toastVisible = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PickflowColors.gray95)
            .testTag("spotdetail-screen"),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
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
                    onOpenSpot = { /* TODO(KAN-future): 내 스팟 오픈 분기 연결. */ },
                    onReport = { isReportSheetOpen = true },
                    onImageClick = { fullscreenImageUrl = state.value.imageUrl },
                )
            }
        }

        if (toastVisible) {
            ReportSubmittedToast(modifier = Modifier.align(Alignment.Center))
        }
    }

    fullscreenImageUrl?.let { url ->
        FullscreenImageViewer(
            imageUrl = url,
            contentDescription = null,
            onDismiss = { fullscreenImageUrl = null },
        )
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
                onClose = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        isReportSheetOpen = false
                    }
                },
                onSubmit = {
                    viewModel.reportInvalidInfo()
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
private fun ReportSubmittedToast(modifier: Modifier = Modifier) {
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
            text = "신고가 접수되었어요",
            style = PickflowTypography.bodyMediumBold,
            color = PickflowColors.gray95,
        )
    }
}

/** iOS `ReportSheet.swift` 1:1 — 최소 5자, 최대 200자 신고 본문 입력. */
@Composable
private fun ReportSheetBody(
    onClose: () -> Unit,
    onSubmit: (String) -> Unit,
) {
    var text by remember { mutableStateOf("") }
    val minLength = 5
    val maxLength = 200
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
                onValueChange = { new ->
                    text = if (new.length > maxLength) new.take(maxLength) else new
                },
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
