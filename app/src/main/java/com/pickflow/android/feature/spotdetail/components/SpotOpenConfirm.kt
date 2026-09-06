package com.pickflow.android.feature.spotdetail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.pickflow.android.common.designsystem.PickflowColors
import com.pickflow.android.common.designsystem.PickflowTypography
import com.pickflow.android.core.services.protocols.MySpotStatus

/**
 * 내 스팟 상태 전이 확인 UI. 상세 화면이 여는 바텀 확인 시트다.
 *
 * `SpotOpenDetailContent` 에 있던 것을 그대로 옮겼다 — 상세 화면 하나로 합치면서
 * 오버레이만 컴포넌트로 남긴다.
 */
enum class SpotOpenSheet {
    REQUEST_OPEN,
    WITHDRAW_REQUEST,
    CANCEL_OPEN,
    DELETE,
    LOGIN,
}

/**
 * 상태에서 오픈 버튼이 열어야 할 확인 시트를 고른다.
 * 반려(REJECTED)는 확인 없이 보완 폼으로 바로 보내므로 null 이다.
 */
fun MySpotStatus?.openActionSheet(): SpotOpenSheet? = when (this) {
    MySpotStatus.PENDING, MySpotStatus.RE_REVIEW_PENDING -> SpotOpenSheet.WITHDRAW_REQUEST
    MySpotStatus.PUBLISHED -> SpotOpenSheet.CANCEL_OPEN
    MySpotStatus.REJECTED -> null
    else -> SpotOpenSheet.REQUEST_OPEN
}

@Composable
fun SpotOpenConfirmOverlay(
    sheet: SpotOpenSheet,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val content = sheet.content
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable(onClick = onDismiss)
            .testTag(content.sheetTag),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .background(PickflowColors.gray95)
                .clickable(enabled = false) {}
                // 좌우 20dp — `SpotDetailSheetContent`/`ReportSheetBody` 와 같은 시트 여백.
                .padding(horizontal = 20.dp)
                .padding(top = 10.dp, bottom = 26.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SheetDragHandle()
            Text(
                text = content.annotatedTitle(),
                style = PickflowTypography.headingMedium,
                color = PickflowColors.gray0,
                textAlign = TextAlign.Center,
            )
            Text(
                text = content.body,
                style = PickflowTypography.bodyMedium,
                color = PickflowColors.gray30,
                textAlign = TextAlign.Center,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ConfirmButton(
                    text = content.secondary,
                    background = PickflowColors.gray0,
                    textColor = PickflowColors.gray80,
                    modifier = Modifier.weight(1f),
                    onClick = onDismiss,
                )
                ConfirmButton(
                    text = content.primary,
                    background = PickflowColors.sunsetOrange,
                    textColor = PickflowColors.gray0,
                    modifier = Modifier.weight(content.primaryWeight),
                    testTag = content.confirmTag,
                    onClick = onConfirm,
                )
            }
        }
    }
}

/**
 * 바텀시트 상단 핸들. Material3 `ModalBottomSheet` 이 기본 제공하는 것과 같은 자리·크기지만,
 * 이 확인 시트는 직접 그린 오버레이라 여기서 함께 그린다.
 */
@Composable
private fun SheetDragHandle() {
    Box(
        modifier = Modifier
            .padding(bottom = 2.dp)
            .width(HandleWidth)
            .height(HandleHeight)
            .clip(RoundedCornerShape(percent = 50))
            .background(PickflowColors.gray50)
            .testTag("sheet-drag-handle"),
    )
}

private val HandleWidth = 40.dp
private val HandleHeight = 4.dp

@Composable
private fun ConfirmButton(
    text: String,
    background: Color,
    textColor: Color,
    modifier: Modifier = Modifier,
    testTag: String? = null,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(background)
            .clickable(onClick = onClick)
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = PickflowTypography.bodyLargeBold,
            color = textColor,
            textAlign = TextAlign.Center,
        )
    }
}

/** 오픈 승인 직후 1회 노출되는 축하 모달. */
@Composable
fun SpotPublishedOverlay(onConfirm: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .testTag("spot-published-modal"),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(PickflowColors.gray90)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "스팟이 오픈되었어요!",
                style = PickflowTypography.headingSmall,
                color = PickflowColors.gray0,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "이제 다른 유저들도 지도에서 이 스팟을 볼 수 있어요.",
                style = PickflowTypography.bodyMedium,
                color = PickflowColors.gray30,
                textAlign = TextAlign.Center,
            )
            ConfirmButton(
                text = "확인",
                background = PickflowColors.sunsetOrange,
                textColor = PickflowColors.gray0,
                modifier = Modifier.fillMaxWidth(),
                testTag = "spot-published-modal-confirm",
                onClick = onConfirm,
            )
        }
    }
}

private data class SpotOpenSheetContent(
    val title: String,
    /** 제목 안에서 sunsetOrange 로 강조할 부분. 없으면 전체가 gray0. */
    val titleHighlight: String? = null,
    val body: String,
    val primary: String,
    val secondary: String = "취소",
    /** 확인 버튼이 취소 버튼의 몇 배 폭인지. 1f 면 반반. */
    val primaryWeight: Float = 1f,
    val sheetTag: String,
    val confirmTag: String,
)

private val SpotOpenSheet.content: SpotOpenSheetContent
    get() = when (this) {
        SpotOpenSheet.REQUEST_OPEN -> SpotOpenSheetContent(
            title = "MY 스팟을 오픈할까요?",
            body = "스팟을 오픈하면 다른 사용자들도 MY 스팟을 볼 수 있어요. " +
                "오픈 신청하면 간단한 확인 절차 후 지도에 표시돼요.",
            primary = "오픈 신청하기",
            // 확인 버튼이 취소의 2배 — 디자인 1:2.
            primaryWeight = 2f,
            sheetTag = "spot-open-request-sheet",
            confirmTag = "spot-open-request-confirm",
        )
        SpotOpenSheet.WITHDRAW_REQUEST -> SpotOpenSheetContent(
            title = "오픈 신청을 철회할까요?",
            titleHighlight = "철회",
            body = "오픈 신청이 철회된 MY 스팟은 나만 볼 수 있어요.",
            primary = "오픈 철회하기",
            sheetTag = "spot-withdraw-request-sheet",
            confirmTag = "spot-withdraw-request-confirm",
        )
        SpotOpenSheet.CANCEL_OPEN -> SpotOpenSheetContent(
            title = "스팟 오픈을 취소할까요?",
            titleHighlight = "취소",
            body = "오픈이 취소된 MY 스팟은 나만 볼 수 있어요.\n" +
                "추천 수는 그대로 유지되고, 다시 오픈하면 이어서 보여요.",
            primary = "오픈 취소하기",
            sheetTag = "spot-cancel-open-sheet",
            confirmTag = "spot-cancel-open-confirm",
        )
        SpotOpenSheet.DELETE -> SpotOpenSheetContent(
            title = "MY 스팟을 삭제할까요?",
            titleHighlight = "삭제",
            body = "삭제한 스팟과 관련된 정보는 복구할 수 없어요.",
            primary = "삭제하기",
            sheetTag = "spot-delete-sheet",
            confirmTag = "spot-delete-confirm",
        )
        SpotOpenSheet.LOGIN -> SpotOpenSheetContent(
            title = "로그인이 필요해요",
            body = "로그인하고 마음에 드는 스팟을 추천해보세요.",
            primary = "로그인하기",
            sheetTag = "spot-recommendation-login",
            confirmTag = "spot-recommendation-login-confirm",
        )
    }

/** 제목에서 [SpotOpenSheetContent.titleHighlight] 만 sunsetOrange 로 칠한다. */
private fun SpotOpenSheetContent.annotatedTitle(): AnnotatedString = buildAnnotatedString {
    val highlightStart = titleHighlight?.let { title.indexOf(it) } ?: -1
    if (titleHighlight == null || highlightStart < 0) {
        append(title)
        return@buildAnnotatedString
    }
    append(title.substring(0, highlightStart))
    withStyle(SpanStyle(color = PickflowColors.sunsetOrange)) { append(titleHighlight) }
    append(title.substring(highlightStart + titleHighlight.length))
}
