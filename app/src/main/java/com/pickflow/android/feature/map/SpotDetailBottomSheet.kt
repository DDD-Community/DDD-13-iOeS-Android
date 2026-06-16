package com.pickflow.android.feature.map

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.pickflow.android.common.designsystem.PickflowColors
import com.pickflow.android.common.designsystem.PickflowTypography
import com.pickflow.android.common.ui.LoadState
import com.pickflow.android.core.services.protocols.Spot
import com.pickflow.android.core.services.protocols.SpotPreview
import com.pickflow.android.core.services.protocols.SpotTheme
import com.pickflow.android.feature.spotdetail.SpotDetailScreen
import com.pickflow.android.feature.spotdetail.components.SpotDetailData
import com.pickflow.android.feature.spotdetail.components.SpotDetailSheetContent
import com.pickflow.android.feature.spotdetail.components.SpotDetailTheme
import kotlinx.coroutines.launch

/**
 * iOS `spotBottomSheet` + `SpotShellRootView`(medium/full 시트) 대응.
 *
 * - medium: preview(GET /v1/spots/{id}/preview) 요약 정보 + 길안내/저장 버튼.
 * - full: 시트를 끝까지 올리면(Expanded) 전체 상세(`SpotDetailScreen`)로 전환한다.
 *   (Material3 `ModalBottomSheet` 는 커스텀 detent 가 없어, full detent 도달 시
 *    전체 상세 화면으로 라우팅하여 "full 시 SpotDetailScreen 표시" 를 만족시킨다.)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpotDetailBottomSheet(
    spotId: String,
    preview: LoadState<SpotPreview>,
    isBookmarked: Boolean,
    onDismiss: () -> Unit,
    onOpenFullDetail: (String) -> Unit,
    onRoute: () -> Unit,
    onSave: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = PickflowColors.gray95,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
    ) {
        val dismiss = {
            scope.launch { sheetState.hide() }.invokeOnCompletion {
                if (!sheetState.isVisible) onDismiss()
            }
            Unit
        }
        // 콘텐츠를 full-height 로 채워 medium(부분)/full(전체) 2단 detent 를 보장한다.
        // - 부분 확장(medium): preview 요약 카드(+길안내/저장 버튼).
        // - 전체 확장(full/fullcover): 시트 안에 전체 상세(SpotDetailScreen) 임베드.
        Box(modifier = Modifier.fillMaxHeight()) {
            if (sheetState.targetValue == SheetValue.Expanded) {
                SpotDetailScreen(
                    spotId = spotId,
                    onBack = { scope.launch { sheetState.partialExpand() } },
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("spotdetail-bottomsheet-full"),
                )
            } else {
                when (val state = preview) {
                    LoadState.Idle, LoadState.Loading -> SheetLoading()
                    is LoadState.Failed -> SheetMessage("스팟 정보를 불러오지 못했어요.")
                    LoadState.Empty -> SheetMessage("스팟 정보를 찾을 수 없어요.")
                    is LoadState.Loaded -> SpotDetailSheetContent(
                        spot = state.value.toSpotDetailData(),
                        isBookmarked = isBookmarked,
                        addressExpanded = false,
                        onClose = dismiss,
                        onRoute = onRoute,
                        onSave = onSave,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("spotdetail-bottomsheet"),
                    )
                }
            }
        }
    }
}

@Composable
private fun SheetLoading() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = PickflowColors.gray0, strokeWidth = 2.dp)
    }
}

@Composable
private fun SheetMessage(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            style = PickflowTypography.bodyMedium,
            color = PickflowColors.gray30,
        )
    }
}

/** preview 도메인 → 바텀시트 표시용 `SpotDetailData`. */
fun SpotPreview.toSpotDetailData(): SpotDetailData = SpotDetailData(
    name = name,
    theme = theme.toDetailTheme(),
    bookmarkCount = bookmarkCount.toInt(),
    isMine = isMySpot,
    isBookmarked = false,
    address = addressSimple.ifBlank { addressRoad ?: addressJibun ?: "주소 정보 없음" },
    addressRoad = addressRoad,
    addressJibun = addressJibun,
    distanceKm = distanceKm,
    hasImage = !imageUrl.isNullOrBlank(),
    imageUrl = imageUrl,
)

/** 지도 `Spot` → 바텀시트용 `SpotDetailData`(preview 미가용 시 폴백). */
fun Spot.toSpotDetailData(): SpotDetailData = SpotDetailData(
    name = name,
    theme = theme.toDetailTheme(),
    address = address.ifBlank { "주소 정보 없음" },
    hasImage = imageUrl != null,
    imageUrl = imageUrl,
)

private fun SpotTheme.toDetailTheme(): SpotDetailTheme = when (this) {
    SpotTheme.SUNSET -> SpotDetailTheme.Sunset
    SpotTheme.YUNSEUL -> SpotDetailTheme.Reflection
}
