package com.pickflow.android.feature.map

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.pickflow.android.R
import com.pickflow.android.common.designsystem.PickflowColors
import com.pickflow.android.common.designsystem.PickflowTypography
import com.pickflow.android.common.ui.LoadState
import com.pickflow.android.core.services.protocols.MySpotStatus
import com.pickflow.android.core.services.protocols.SpotMapMarker
import com.pickflow.android.core.services.protocols.SpotSource
import com.pickflow.android.feature.map.clustering.MyClusterPinView
import com.pickflow.android.feature.map.clustering.SpotMarkerView
import com.pickflow.android.feature.spotlist.components.SpotListCell
import com.pickflow.android.feature.spotlist.components.SpotListGridItem

/**
 * Viewport 분기 규칙을 Naver SDK 없이 검증·snapshot 할 수 있는 stateless marker content.
 * 실제 화면 연결은 [HomeMapScreen]이 담당한다.
 */
@Composable
fun HomeMapContent(
    markerState: LoadState<List<SpotMapMarker>>,
    selectedSpotId: Long?,
    onSpotTap: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PickflowColors.themeReflection),
        contentAlignment = Alignment.Center,
    ) {
        when (markerState) {
            LoadState.Idle,
            LoadState.Loading,
            -> CircularProgressIndicator(
                color = PickflowColors.sunsetOrange,
                modifier = Modifier.testTag("homemap-loading"),
            )

            is LoadState.Failed -> Text(
                text = "지도를 불러오지 못했어요.",
                style = PickflowTypography.bodyMedium,
                color = PickflowColors.gray0,
                modifier = Modifier.testTag("homemap-error"),
            )

            LoadState.Empty -> Text(
                text = "이 지역에는 표시할 스팟이 없어요.",
                style = PickflowTypography.bodyMedium,
                color = PickflowColors.gray30,
            )

            is LoadState.Loaded -> MarkerViewport(
                markers = markerState.value,
                selectedSpotId = selectedSpotId,
                onSpotTap = onSpotTap,
            )
        }
    }
}

@Composable
private fun MarkerViewport(
    markers: List<SpotMapMarker>,
    selectedSpotId: Long?,
    onSpotTap: (Long) -> Unit,
) {
    val visible = markers.filter { marker ->
        (marker.isOwnedByCurrentUser && marker.status == MySpotStatus.DRAFT) ||
            marker.status == MySpotStatus.PUBLISHED ||
            marker.source is SpotSource.Curated
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        visible.forEach { marker ->
            val selected = marker.spotId == selectedSpotId
            if (marker.isOwnedByCurrentUser && marker.status == MySpotStatus.DRAFT) {
                Box(
                    modifier = Modifier
                        .clickable { onSpotTap(marker.spotId) }
                        .semantics { contentDescription = "MY 스팟" }
                        .testTag("map-marker-my-${marker.spotId}"),
                ) {
                    MyClusterPinView(isSelected = selected)
                }
            } else {
                Box(
                    modifier = Modifier
                        .clickable { onSpotTap(marker.spotId) }
                        .semantics {
                            contentDescription = if (selected) "선택된 공개 스팟" else "공개 스팟"
                        }
                        .testTag("map-marker-public-${marker.spotId}"),
                ) {
                    if (selected) SelectedPublicMarker() else SpotMarkerView(isSelected = false)
                }
            }
        }
    }
}

/** Figma 764:10482 — 60dp 원, 4dp orange ring, 20dp photo glyph. */
@Composable
private fun SelectedPublicMarker() {
    Box(
        modifier = Modifier
            .size(60.dp)
            .clip(CircleShape)
            .background(PickflowColors.sunsetOrange),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp)
                .clip(CircleShape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_photo),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/** 공개 유저 스팟도 기존 탐색 카드 모양을 그대로 사용하며 출처 UI를 추가하지 않는다. */
@Composable
fun SpotOpenListCell(
    item: SpotListGridItem,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .testTag("spot-list-cell-${item.spotId}"),
    ) {
        SpotListCell(
            item = item,
            isBookmarked = false,
            bookmarkCount = null,
        )
    }
}
