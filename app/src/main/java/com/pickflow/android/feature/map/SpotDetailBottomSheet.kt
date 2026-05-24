package com.pickflow.android.feature.map

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.pickflow.android.common.designsystem.PickflowColors
import com.pickflow.android.core.services.protocols.Spot
import com.pickflow.android.feature.spotdetail.components.SpotDetailData
import com.pickflow.android.feature.spotdetail.components.SpotDetailSheetContent
import com.pickflow.android.feature.spotdetail.components.SpotDetailTheme

/**
 * iOS `spotBottomSheet` + `SpotShellRootView`(medium 시트) 대응.
 *
 * iOS는 커스텀 `UIPresentationController` 기반 3단계 인터랙티브 시트지만, Android는
 * 표준 Material3 `ModalBottomSheet`(부분/전체 확장)로 이식한다. 시트 본문을 탭하면
 * iOS의 "위로 드래그 → 전체 상세" 대응으로 전체화면 `SpotDetailScreen`을 연다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpotDetailBottomSheet(
    spot: SpotDetailData,
    spotId: String,
    onDismiss: () -> Unit,
    onOpenFullDetail: (String) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = PickflowColors.gray95,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
    ) {
        SpotDetailSheetContent(
            spot = spot,
            isBookmarked = spot.isBookmarked,
            addressExpanded = false,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenFullDetail(spotId) }
                .testTag("spotdetail-bottomsheet"),
        )
    }
}

/** 지도 `Spot`을 바텀시트용 `SpotDetailData`로 변환. 미보유 필드는 기본값 유지. */
fun Spot.toSpotDetailData(): SpotDetailData = SpotDetailData(
    name = name,
    theme = if (id.hashCode() % 2 == 0) SpotDetailTheme.Sunset else SpotDetailTheme.Reflection,
    address = address.ifBlank { "주소 정보 없음" },
    hasImage = imageUrl != null,
)
