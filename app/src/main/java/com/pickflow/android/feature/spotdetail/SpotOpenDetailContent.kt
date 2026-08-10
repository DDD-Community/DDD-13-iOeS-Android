package com.pickflow.android.feature.spotdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.pickflow.android.R
import com.pickflow.android.common.designsystem.PickflowColors
import com.pickflow.android.common.designsystem.PickflowTypography
import com.pickflow.android.common.ui.LoadState
import com.pickflow.android.core.services.protocols.MySpotDetail
import com.pickflow.android.core.services.protocols.MySpotStatus
import com.pickflow.android.core.services.protocols.SpotSource
import com.pickflow.android.core.services.protocols.SpotTheme
import com.pickflow.android.feature.spotdetail.components.ReportButton
import com.pickflow.android.feature.spotdetail.components.SpotDetailData
import com.pickflow.android.feature.spotdetail.components.SpotDetailNavBar
import com.pickflow.android.feature.spotdetail.components.SpotDetailTheme
import com.pickflow.android.feature.spotdetail.components.SpotPhotoSection

/** PV-41 상세 화면에서 열리는 확인 UI. 스냅샷의 초기 상태로도 사용한다. */
enum class SpotOpenSheet {
    REQUEST_OPEN,
    WITHDRAW_REQUEST,
    WITHDRAW_REJECTION,
    CANCEL_OPEN,
    DELETE,
    LOGIN,
}

/**
 * PV-41 상태·출처별 상세 UI.
 *
 * 서비스나 ViewModel을 참조하지 않는 stateless 경계이며, 화면 내부 확인 UI의 열림 상태만
 * 보유한다. 서버 상태와 요청 중 여부는 호출자가 단방향으로 전달한다.
 */
@Composable
fun SpotOpenDetailContent(
    state: LoadState<MySpotDetail>,
    onBack: () -> Unit = {},
    isTransitionInFlight: Boolean = false,
    isRecommendationInFlight: Boolean = false,
    isLoggedIn: Boolean = true,
    showPublishedModal: Boolean = false,
    onRequestOpen: () -> Unit = {},
    onWithdrawRequest: () -> Unit = {},
    onWithdrawRejection: () -> Unit = {},
    onRevise: (Long) -> Unit = {},
    onCancelOpen: () -> Unit = {},
    onDelete: () -> Unit = {},
    onToggleRecommendation: () -> Unit = {},
    onRequireLogin: () -> Unit = {},
    onAcknowledgePublishedModal: () -> Unit = {},
    onReport: () -> Unit = {},
    initialSheet: SpotOpenSheet? = null,
    modifier: Modifier = Modifier,
) {
    var activeSheet by remember(initialSheet) { mutableStateOf(initialSheet) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PickflowColors.gray95),
    ) {
        when (state) {
            LoadState.Idle, LoadState.Loading -> SpotOpenLoading()
            LoadState.Empty -> SpotOpenError()
            is LoadState.Failed -> SpotOpenError()
            is LoadState.Loaded -> SpotOpenLoaded(
                detail = state.value,
                onBack = onBack,
                isTransitionInFlight = isTransitionInFlight,
                isRecommendationInFlight = isRecommendationInFlight,
                isLoggedIn = isLoggedIn,
                onOpenSheet = { activeSheet = it },
                onRevise = onRevise,
                onToggleRecommendation = onToggleRecommendation,
                onReport = onReport,
            )
        }

        activeSheet?.let { sheet ->
            SpotOpenConfirmOverlay(
                sheet = sheet,
                onDismiss = { activeSheet = null },
                onConfirm = {
                    activeSheet = null
                    when (sheet) {
                        SpotOpenSheet.REQUEST_OPEN -> onRequestOpen()
                        SpotOpenSheet.WITHDRAW_REQUEST -> onWithdrawRequest()
                        SpotOpenSheet.WITHDRAW_REJECTION -> onWithdrawRejection()
                        SpotOpenSheet.CANCEL_OPEN -> onCancelOpen()
                        SpotOpenSheet.DELETE -> onDelete()
                        SpotOpenSheet.LOGIN -> onRequireLogin()
                    }
                },
            )
        }

        if (showPublishedModal) {
            SpotPublishedOverlay(
                onConfirm = onAcknowledgePublishedModal,
            )
        }
    }
}

@Composable
private fun SpotOpenLoading() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("spot-open-loading"),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            progress = { 0.75f },
            color = PickflowColors.gray0,
            strokeWidth = 2.dp,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun SpotOpenError() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .testTag("spot-open-error"),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "스팟 정보를 불러오지 못했어요.",
            style = PickflowTypography.bodyLargeBold,
            color = PickflowColors.gray0,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun SpotOpenLoaded(
    detail: MySpotDetail,
    onBack: () -> Unit,
    isTransitionInFlight: Boolean,
    isRecommendationInFlight: Boolean,
    isLoggedIn: Boolean,
    onOpenSheet: (SpotOpenSheet) -> Unit,
    onRevise: (Long) -> Unit,
    onToggleRecommendation: () -> Unit,
    onReport: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        SpotDetailNavBar(onBack = onBack, onClose = onBack)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            SpotOpenHeader(detail)
            SpotPhotoSection(spot = detail.toSpotDetailData())

            when (detail.status) {
                MySpotStatus.DRAFT -> SpotOpenActionButton(
                    text = "내 스팟 오픈하기",
                    tag = "spot-action-request-open",
                    enabled = !isTransitionInFlight,
                    onClick = { onOpenSheet(SpotOpenSheet.REQUEST_OPEN) },
                )

                MySpotStatus.PENDING, MySpotStatus.RE_REVIEW_PENDING -> SpotOpenActionButton(
                    text = "신청 철회하기",
                    tag = "spot-action-withdraw-request",
                    enabled = !isTransitionInFlight,
                    primary = false,
                    onClick = { onOpenSheet(SpotOpenSheet.WITHDRAW_REQUEST) },
                )

                MySpotStatus.REJECTED -> RejectedActions(
                    detail = detail,
                    enabled = !isTransitionInFlight,
                    onWithdraw = { onOpenSheet(SpotOpenSheet.WITHDRAW_REJECTION) },
                    onRevise = { onRevise(detail.id) },
                )

                MySpotStatus.PUBLISHED -> PublishedActions(
                    detail = detail,
                    isRecommendationInFlight = isRecommendationInFlight,
                    isLoggedIn = isLoggedIn,
                    isTransitionInFlight = isTransitionInFlight,
                    onRecommendation = onToggleRecommendation,
                    onLogin = { onOpenSheet(SpotOpenSheet.LOGIN) },
                    onCancelOpen = { onOpenSheet(SpotOpenSheet.CANCEL_OPEN) },
                    onDelete = { onOpenSheet(SpotOpenSheet.DELETE) },
                    onReport = onReport,
                )
            }
        }
    }
}

@Composable
private fun SpotOpenHeader(detail: MySpotDetail) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = detail.name,
                style = PickflowTypography.headingLarge,
                color = PickflowColors.gray0,
            )
            when {
                detail.source is SpotSource.Curated -> Unit
                detail.status == MySpotStatus.DRAFT -> StatusBadge(
                    text = "MY 스팟",
                    tag = "spot-status-draft",
                )
                detail.status == MySpotStatus.PENDING -> StatusBadge(
                    text = "검수중",
                    tag = "spot-status-pending",
                )
                detail.status == MySpotStatus.RE_REVIEW_PENDING -> StatusBadge(
                    text = "검수중",
                    tag = "spot-status-re-review-pending",
                )
                detail.status == MySpotStatus.REJECTED -> StatusBadge(
                    text = "반려됨",
                    tag = "spot-status-rejected",
                )
                else -> StatusBadge(
                    text = "유저 등록",
                    tag = "spot-source-user",
                )
            }
        }

        when (val source = detail.source) {
            SpotSource.User -> Unit
            is SpotSource.Curated -> Row(
                modifier = Modifier.testTag("spot-source-curated"),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier.size(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_global),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(1.dp),
                    )
                }
                Text(
                    text = source.displayName,
                    style = PickflowTypography.bodyMedium,
                    color = PickflowColors.gray30,
                )
            }
        }

        Text(
            text = if (detail.theme == SpotTheme.SUNSET) "노을" else "윤슬",
            style = PickflowTypography.bodySmall,
            color = PickflowColors.gray30,
        )

        if (detail.status == MySpotStatus.REJECTED) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(PickflowColors.gray90)
                    .background(Color(0x1FB83311))
                    .padding(16.dp)
                    .testTag("spot-rejection-banner"),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = "스팟 오픈이 반려되었어요",
                    style = PickflowTypography.bodyMediumBold,
                    color = PickflowColors.gray0,
                )
                Text(
                    text = detail.rejectionReason ?: "등록 정보를 다시 확인해주세요.",
                    style = PickflowTypography.bodyMediumBold,
                    color = PickflowColors.gray30,
                )
                Text(
                    text = detail.updatedAt.take(10),
                    style = PickflowTypography.bodySmall,
                    color = PickflowColors.gray50,
                )
            }
        }

        Text(
            text = detail.comment,
            style = PickflowTypography.bodyMedium,
            color = PickflowColors.gray0,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(PickflowColors.gray90)
                .padding(16.dp),
        )
    }
}

@Composable
private fun StatusBadge(text: String, tag: String) {
    Text(
        text = text,
        style = PickflowTypography.bodySmallBold,
        color = PickflowColors.sunsetOrange,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(PickflowColors.sunsetOrangeBg)
            .padding(horizontal = 7.dp, vertical = 4.dp)
            .testTag(tag),
    )
}

@Composable
private fun RejectedActions(
    detail: MySpotDetail,
    enabled: Boolean,
    onWithdraw: () -> Unit,
    onRevise: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SpotOpenActionButton(
            text = "오픈 철회하기",
            tag = "spot-action-withdraw-rejection",
            enabled = enabled,
            primary = false,
            onClick = onWithdraw,
        )
        SpotOpenActionButton(
            text = "내용 보완해서 다시 신청하기",
            tag = "spot-action-revise",
            enabled = enabled && detail.id > 0L,
            onClick = onRevise,
        )
    }
}

@Composable
private fun PublishedActions(
    detail: MySpotDetail,
    isRecommendationInFlight: Boolean,
    isLoggedIn: Boolean,
    isTransitionInFlight: Boolean,
    onRecommendation: () -> Unit,
    onLogin: () -> Unit,
    onCancelOpen: () -> Unit,
    onDelete: () -> Unit,
    onReport: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(PickflowColors.gray90)
                .semantics {
                    contentDescription = if (detail.isRecommended) "추천함" else "추천하지 않음"
                }
                .clickable(
                    enabled = !isRecommendationInFlight,
                    onClick = if (isLoggedIn) onRecommendation else onLogin,
                )
                .padding(horizontal = 16.dp)
                .testTag("spot-recommendation"),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(24.dp),
                contentAlignment = Alignment.TopCenter,
            ) {
                Icon(
                    painter = painterResource(
                        if (detail.isRecommended) {
                            R.drawable.ic_recommend_filled
                        } else {
                            R.drawable.ic_recommend_outline
                        },
                    ),
                    contentDescription = null,
                    tint = if (detail.isRecommended) PickflowColors.sunsetOrange else PickflowColors.gray80,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            start = if (detail.isRecommended) 2.dp else 1.dp,
                            top = if (detail.isRecommended) 2.dp else 1.dp,
                            end = if (detail.isRecommended) 2.dp else 1.dp,
                            bottom = if (detail.isRecommended) 4.dp else 3.dp,
                        ),
                )
            }
            Text(
                text = if (detail.isRecommended) "추천했어요" else "추천해요",
                style = PickflowTypography.bodyLargeBold,
                color = if (detail.isRecommended) PickflowColors.sunsetOrange else PickflowColors.gray0,
            )
            Text(
                text = detail.recommendationCount.toString(),
                style = PickflowTypography.bodyMedium,
                color = PickflowColors.gray30,
            )
        }

        if (detail.source == SpotSource.User) {
            SpotOpenActionButton(
                text = "오픈 취소하기",
                tag = "spot-action-cancel-open",
                enabled = !isTransitionInFlight,
                primary = false,
                onClick = onCancelOpen,
            )
            Text(
                text = "스팟 삭제",
                style = PickflowTypography.bodyMedium.copy(textDecoration = TextDecoration.Underline),
                color = PickflowColors.gray50,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !isTransitionInFlight, onClick = onDelete)
                    .padding(vertical = 8.dp)
                    .testTag("spot-action-delete"),
            )
        }

        ReportButton(onClick = onReport)
    }
}

@Composable
private fun SpotOpenActionButton(
    text: String,
    tag: String,
    enabled: Boolean,
    primary: Boolean = true,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                when {
                    !enabled -> PickflowColors.gray80
                    primary -> PickflowColors.sunsetOrange
                    else -> PickflowColors.gray0
                },
            )
            .clickable(enabled = enabled, onClick = onClick)
            .testTag(tag),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = PickflowTypography.bodyLargeBold,
            color = if (primary) PickflowColors.gray0 else PickflowColors.gray80,
        )
    }
}

@Composable
private fun SpotOpenConfirmOverlay(
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
                .padding(horizontal = 16.dp)
                .padding(top = 28.dp, bottom = 26.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = content.title,
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
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(PickflowColors.gray0)
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = content.secondary,
                        style = PickflowTypography.bodyLargeBold,
                        color = PickflowColors.gray80,
                        textAlign = TextAlign.Center,
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(PickflowColors.sunsetOrange)
                        .clickable(onClick = onConfirm)
                        .testTag(content.confirmTag),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = content.primary,
                        style = PickflowTypography.bodyLargeBold,
                        color = PickflowColors.gray0,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

private data class SpotOpenSheetContent(
    val title: String,
    val body: String,
    val primary: String,
    val secondary: String,
    val sheetTag: String,
    val confirmTag: String,
)

private val SpotOpenSheet.content: SpotOpenSheetContent
    get() = when (this) {
        SpotOpenSheet.REQUEST_OPEN -> SpotOpenSheetContent(
            title = "스팟을 오픈하면\n다른 유저들도 볼 수 있어요",
            body = "간단한 확인 절차를 거친 후 지도에 공개돼요.\n" +
                "확인 전까지는 나만 볼 수 있어요.",
            primary = "오픈 신청하기",
            secondary = "다음에요",
            sheetTag = "spot-open-request-sheet",
            confirmTag = "spot-open-request-confirm",
        )
        SpotOpenSheet.WITHDRAW_REQUEST -> SpotOpenSheetContent(
            title = "신청을 철회할까요?",
            body = "철회하면 나만 볼 수 있는 상태로 돌아가요.\n" +
                "언제든 다시 신청할 수 있어요.",
            primary = "신청 철회하기",
            secondary = "계속 기다릴게요",
            sheetTag = "spot-withdraw-request-sheet",
            confirmTag = "spot-withdraw-request-confirm",
        )
        SpotOpenSheet.WITHDRAW_REJECTION -> SpotOpenSheetContent(
            title = "스팟 오픈을 철회할까요?",
            body = "철회하면 나만 볼 수 있는 상태로 돌아가요.\n" +
                "언제든 다시 신청할 수 있어요.",
            primary = "오픈 철회하기",
            secondary = "계속 수정할게요",
            sheetTag = "spot-withdraw-rejection-sheet",
            confirmTag = "spot-withdraw-rejection-confirm",
        )
        SpotOpenSheet.CANCEL_OPEN -> SpotOpenSheetContent(
            title = "스팟 오픈을 취소할까요?",
            body = "취소하면 나만 볼 수 있는 상태로 돌아가요.\n" +
                "좋아요 수는 그대로 유지되고, 다시 오픈하면 이어서 보여요.",
            primary = "오픈 취소하기",
            secondary = "계속 공개할게요",
            sheetTag = "spot-cancel-open-sheet",
            confirmTag = "spot-cancel-open-confirm",
        )
        SpotOpenSheet.DELETE -> SpotOpenSheetContent(
            title = "스팟을 삭제할까요?",
            body = "삭제한 스팟은 다시 복구할 수 없어요.",
            primary = "삭제하기",
            secondary = "취소",
            sheetTag = "spot-delete-sheet",
            confirmTag = "spot-delete-confirm",
        )
        SpotOpenSheet.LOGIN -> SpotOpenSheetContent(
            title = "로그인이 필요해요",
            body = "로그인하고 마음에 드는 스팟을 추천해보세요.",
            primary = "로그인하기",
            secondary = "다음에요",
            sheetTag = "spot-recommendation-login",
            confirmTag = "spot-recommendation-login-confirm",
        )
    }

@Composable
private fun SpotPublishedOverlay(onConfirm: () -> Unit) {
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
            SpotOpenActionButton(
                text = "확인",
                tag = "spot-published-modal-confirm",
                enabled = true,
                onClick = onConfirm,
            )
        }
    }
}

private fun MySpotDetail.toSpotDetailData() = SpotDetailData(
    name = name,
    theme = if (theme == SpotTheme.SUNSET) SpotDetailTheme.Sunset else SpotDetailTheme.Reflection,
    comment = comment,
    isMine = source == SpotSource.User,
    address = address,
    hasImage = !imageUrl.isNullOrBlank(),
    imageUrl = imageUrl,
    recordedTime = "$capturedDate $capturedTime",
)
