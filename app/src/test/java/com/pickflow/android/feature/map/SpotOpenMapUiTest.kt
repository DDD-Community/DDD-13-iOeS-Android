package com.pickflow.android.feature.map

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
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
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Phase B RED contract:
 *
 * - `HomeMapContent(LoadState<List<SpotMapMarker>>, Long?, (Long) -> Unit)` renders viewport state.
 * - `DRAFT` owned markers use the MY pool; `PUBLISHED` user markers use the public cluster pool.
 * - `SpotOpenListCell(SpotListGridItem)` keeps the existing public card without a source row.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h950dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SpotOpenMapUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun map_loading_renders_progress() {
        composeRule.setContent {
            PickflowTheme {
                HomeMapContent(
                    markerState = LoadState.Loading,
                    selectedSpotId = null,
                    onSpotTap = { _: Long -> },
                )
            }
        }

        composeRule.onNodeWithTag("homemap-loading").assertIsDisplayed()
    }

    @Test
    fun map_failed_renders_error() {
        composeRule.setContent {
            PickflowTheme {
                HomeMapContent(
                    markerState = LoadState.Failed(IllegalStateException("viewport failed")),
                    selectedSpotId = null,
                    onSpotTap = { _: Long -> },
                )
            }
        }

        composeRule.onNodeWithTag("homemap-error").assertIsDisplayed()
    }

    @Test
    fun draft_owner_renders_my_marker_only() {
        val marker = marker(
            spotId = 41L,
            status = MySpotStatus.DRAFT,
            isOwnedByCurrentUser = true,
        )

        composeRule.setContent {
            PickflowTheme {
                HomeMapContent(
                    markerState = LoadState.Loaded(listOf(marker)),
                    selectedSpotId = null,
                    onSpotTap = { _: Long -> },
                )
            }
        }

        composeRule.onNodeWithTag("map-marker-my-41").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("MY 스팟").assertIsDisplayed()
        composeRule.onNodeWithTag("map-marker-public-41").assertDoesNotExist()
    }

    @Test
    fun published_user_uses_public_selected_marker() {
        val marker = marker(
            spotId = 42L,
            status = MySpotStatus.PUBLISHED,
            isOwnedByCurrentUser = true,
        )

        composeRule.setContent {
            PickflowTheme {
                HomeMapContent(
                    markerState = LoadState.Loaded(listOf(marker)),
                    selectedSpotId = 42L,
                    onSpotTap = { _: Long -> },
                )
            }
        }

        composeRule.onNodeWithTag("map-marker-public-42").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("선택된 공개 스팟").assertIsDisplayed()
        composeRule.onNodeWithTag("map-marker-my-42").assertDoesNotExist()
    }

    @Test
    fun published_user_list_has_no_source_line() {
        composeRule.setContent {
            PickflowTheme {
                SpotOpenListCell(
                    item = SpotListGridItem(
                        spotId = 43L,
                        name = "유저 공개 스팟",
                        mood = SpotListMood.Sunset,
                        distanceKm = 1.2,
                    ),
                )
            }
        }

        composeRule.onNodeWithTag("spot-list-cell-43").assertIsDisplayed()
        composeRule.onNodeWithTag("spot-source-user").assertDoesNotExist()
        composeRule.onNodeWithText("유저 등록 스팟").assertDoesNotExist()
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
}
