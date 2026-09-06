package com.pickflow.android.feature.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.resources.Density
import com.android.resources.ScreenOrientation
import com.pickflow.android.common.designsystem.PickflowColors
import com.pickflow.android.common.designsystem.PickflowTheme
import com.pickflow.android.common.ui.LoadState
import com.pickflow.android.core.services.protocols.Coordinates
import com.pickflow.android.core.services.protocols.MySpotStatus
import com.pickflow.android.core.services.protocols.SpotMapMarker
import com.pickflow.android.core.services.protocols.SpotSource
import com.pickflow.android.feature.spotlist.components.SpotListGridItem
import com.pickflow.android.feature.spotlist.components.SpotListMood
import org.junit.Rule
import org.junit.Test

class SpotOpenMapSnapshotTest {

    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = device(393, 280))

    @Test
    fun map_owned_draft_and_selected_public() = snapshot(393, 280) {
        HomeMapContent(
            markerState = LoadState.Loaded(
                listOf(
                    marker(41L, MySpotStatus.DRAFT, true),
                    marker(42L, MySpotStatus.PUBLISHED, true),
                    curatedMarker(44L),
                ),
            ),
            selectedSpotId = 42L,
            onSpotTap = {},
        )
    }

    @Test
    fun published_user_list_cell() = snapshot(200, 320) {
        SpotOpenListCell(
            item = SpotListGridItem(
                spotId = 43L,
                name = "유저 공개 스팟",
                mood = SpotListMood.Sunset,
                distanceKm = 1.2,
            ),
            modifier = Modifier.padding(8.dp),
        )
    }

    @Test
    fun curated_list_cell_keeps_public_card_shape() = snapshot(200, 320) {
        SpotOpenListCell(
            item = SpotListGridItem(
                spotId = 44L,
                name = "큐레이션 공개 스팟",
                mood = SpotListMood.Reflection,
                distanceKm = 2.4,
            ),
            modifier = Modifier.padding(8.dp),
        )
    }

    private fun snapshot(
        widthDp: Int,
        heightDp: Int,
        content: @Composable () -> Unit,
    ) {
        paparazzi.unsafeUpdateConfig(device(widthDp, heightDp))
        paparazzi.snapshot {
            PickflowTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(PickflowColors.gray95),
                    contentAlignment = Alignment.Center,
                ) {
                    content()
                }
            }
        }
    }

    private fun marker(
        spotId: Long,
        status: MySpotStatus,
        isOwnedByCurrentUser: Boolean,
    ) = SpotMapMarker(
        spotId = spotId,
        imageUrl = null,
        coordinates = Coordinates(latitude = 37.5665, longitude = 126.9780),
        isMySpot = isOwnedByCurrentUser,
        source = SpotSource.User,
        status = status,
        isOwnedByCurrentUser = isOwnedByCurrentUser,
    )

    private fun curatedMarker(spotId: Long) = SpotMapMarker(
        spotId = spotId,
        imageUrl = null,
        coordinates = Coordinates(latitude = 37.5670, longitude = 126.9790),
        isMySpot = false,
        source = SpotSource.Curated(displayName = "Pickflow"),
        status = null,
        isOwnedByCurrentUser = false,
    )

    private companion object {
        fun device(widthDp: Int, heightDp: Int): DeviceConfig = DeviceConfig.PIXEL_5.copy(
            screenWidth = widthDp * 2,
            screenHeight = heightDp * 2,
            xdpi = 320,
            ydpi = 320,
            density = Density.XHIGH,
            orientation = if (widthDp > heightDp) {
                ScreenOrientation.LANDSCAPE
            } else {
                ScreenOrientation.PORTRAIT
            },
        )
    }
}
