package com.pickflow.android.feature.spotdetail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.pickflow.android.common.designsystem.PickflowColors
import com.pickflow.android.common.designsystem.PickflowTypography

/** iOS `loadingScreen()` 대응 — gray95 위 중앙 ProgressView(결정성 위해 정적 호). */
@Composable
fun SpotDetailLoadingContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PickflowColors.gray95)
            .testTag("spotdetail-loading"),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator(
            progress = { 0.75f },
            color = PickflowColors.gray0,
            strokeWidth = 2.dp,
            modifier = Modifier.size(20.dp),
        )
    }
}

/** iOS `errorScreen()` 대응 — gray95 위 중앙 실패 안내. */
@Composable
fun SpotDetailErrorContent(
    modifier: Modifier = Modifier,
    message: String = "오류 메시지",
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PickflowColors.gray95)
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
        Text(
            text = message,
            style = PickflowTypography.bodySmall,
            color = PickflowColors.gray50,
            textAlign = TextAlign.Center,
        )
    }
}

/** iOS `ReportButton` 1:1 — 신고 버튼(밑줄 텍스트). */
@Composable
fun ReportButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("detail-report"),
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Info,
            contentDescription = null,
            tint = PickflowColors.gray50,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = "잘못된 정보가 있나요?",
            style = PickflowTypography.bodyMedium.copy(textDecoration = TextDecoration.Underline),
            color = PickflowColors.gray50,
        )
    }
}

/**
 * iOS `loadedScreen()` 대응 — NavBar + 스크롤 본문(헤더/사진/액션/실시간/신고).
 */
@Composable
fun SpotDetailLoadedContent(
    spot: SpotDetailData,
    isBookmarked: Boolean,
    modifier: Modifier = Modifier,
    onShare: () -> Unit = {},
    onClose: () -> Unit = {},
    onRoute: () -> Unit = {},
    onBookmark: () -> Unit = {},
    onOpenSpot: () -> Unit = {},
    onReport: () -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PickflowColors.gray95)
            .testTag("spotdetail-loaded"),
    ) {
        SpotDetailNavBar(onShare = onShare, onClose = onClose)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            SpotHeaderSection(spot = spot)
            SpotPhotoSection(spot = spot)
            SpotActionButtons(
                isMine = spot.isMine,
                isBookmarked = isBookmarked,
                onRoute = onRoute,
                onBookmark = onBookmark,
                onOpenSpot = onOpenSpot,
            )
            SpotRealTimeInfoSection(spot = spot)
            ReportButton(onClick = onReport)
        }
    }
}
