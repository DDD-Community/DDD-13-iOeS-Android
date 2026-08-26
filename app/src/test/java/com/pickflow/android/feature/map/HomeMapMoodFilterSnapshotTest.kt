package com.pickflow.android.feature.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.resources.Density
import com.android.resources.ScreenOrientation
import com.pickflow.android.common.designsystem.PickflowColors
import com.pickflow.android.common.designsystem.PickflowTheme
import com.pickflow.android.feature.map.components.MoodFilterRow
import com.pickflow.android.feature.spotlist.components.SpotListCell
import com.pickflow.android.feature.spotlist.components.SpotListGridItem
import com.pickflow.android.feature.spotlist.components.SpotListMood
import org.junit.Rule
import org.junit.Test

/**
 * PV-59 무드 필터 확장(햇살/야경) 스냅샷 — `docs/PV-59/ui-test-cases.md` 참조.
 *
 * 대상은 stateless Composable 두 개다:
 * - `MoodFilterRow`(지도 상단) — 미선택/부분선택/전체선택 3케이스 (PV59-MAP6/7/8)
 * - `SpotListCell`(저장 탭 카드) — 무드 4종 배지 (PV59-ARC1)
 *
 * 무드 캡슐은 정적 다크 토큰(gray95/sunsetOrange)만 쓰므로 light/dark 렌더가 동일하다.
 * 기존 그룹 관례를 따라 dark 이름 하나만 record 한다.
 */
class HomeMapMoodFilterSnapshotTest {

    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = device(390, 56))

    /** PV59-MAP6 — 아무 무드도 선택되지 않은 초기 상태. */
    @Test
    fun moodfilter_none_selected_dark() = moodRow(emptySet())

    /** PV59-MAP7 — 햇살+윤슬 두 개가 동시에 선택된 상태(다중선택). */
    @Test
    fun moodfilter_sunlight_reflection_selected_dark() =
        moodRow(setOf(MoodFilter.Sunlight, MoodFilter.Reflection))

    /** PV59-MAP8 — 네 무드가 모두 선택된 상태. */
    @Test
    fun moodfilter_all_selected_dark() = moodRow(MoodFilter.entries.toSet())

    /**
     * 좁은 기기(360dp) 회귀 방지 — 84dp 고정일 때 4×84+간격24+패딩32=392dp 라
     * 마지막 '야경' 칩이 잘리고 라벨이 두 줄로 쪼개졌다(hotfix). 네 칩이 같은 폭으로
     * 줄어들고 라벨이 한 줄로 남는지 확인한다.
     */
    @Test
    fun moodfilter_narrow_360dp_dark() {
        paparazzi.unsafeUpdateConfig(device(360, 56))
        moodRow(setOf(MoodFilter.Night))
    }

    /**
     * 넓은 기기(411dp) 상한 확인 — weight 기본값(fill = true)은 4등분한 폭을 고정 제약으로
     * 내려보내 `widthIn(max)` 을 무시시킨다. 그때 캡슐이 84dp 를 넘어 89dp 로 커졌다.
     * Figma 상한인 84dp 를 지키는지 본다.
     */
    @Test
    fun moodfilter_wide_411dp_dark() {
        paparazzi.unsafeUpdateConfig(device(411, 56))
        moodRow(setOf(MoodFilter.Sunlight))
    }

    /** 최소 폭(320dp) — 칩 66dp 로 줄어도 아이콘·라벨이 온전히 남는지 확인한다. */
    @Test
    fun moodfilter_narrow_320dp_dark() {
        paparazzi.unsafeUpdateConfig(device(320, 56))
        moodRow(emptySet())
    }


    /** PV59-ARC1 — 저장 탭 카드 셀의 무드 배지 4종. */
    @Test
    fun spotlist_cell_moods_all_four_dark() {
        // 셀 폭은 기존 SpotList 셀 스냅샷과 동일한 168dp — 4장을 나란히 두려면 가로 캔버스가 필요하다.
        paparazzi.unsafeUpdateConfig(device(720, 320))
        paparazzi.snapshot {
            PickflowTheme {
                Canvas {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SpotListMood.entries.forEach { mood ->
                            SpotListCell(
                                item = SpotListGridItem(
                                    spotId = mood.ordinal.toLong(),
                                    name = mood.displayName,
                                    mood = mood,
                                    hasThumbnail = false,
                                    distanceKm = 1.2,
                                    isBookmarked = true,
                                ),
                                modifier = Modifier.width(168.dp),
                            )
                        }
                    }
                }
            }
        }
    }

    private fun moodRow(selected: Set<MoodFilter>) {
        paparazzi.snapshot {
            PickflowTheme {
                Canvas(alignment = Alignment.CenterStart) {
                    MoodFilterRow(selected = selected, onSelect = {}, testTag = "snapshot-moodfilter")
                }
            }
        }
    }

    /** 스냅샷 배경 — 지도/리스트와 동일한 gray95 위에 컨텐츠를 얹는다. */
    @androidx.compose.runtime.Composable
    private fun Canvas(
        alignment: Alignment = Alignment.Center,
        content: @androidx.compose.runtime.Composable () -> Unit,
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(PickflowColors.gray95),
            contentAlignment = alignment,
        ) {
            content()
        }
    }

    private companion object {
        fun device(wDp: Int, hDp: Int, fontScale: Float = 1f): DeviceConfig =
            DeviceConfig.PIXEL_5.copy(
                screenWidth = wDp * 2,
                screenHeight = hDp * 2,
                xdpi = 320,
                ydpi = 320,
                density = Density.XHIGH,
                fontScale = fontScale,
                orientation = if (wDp > hDp) ScreenOrientation.LANDSCAPE else ScreenOrientation.PORTRAIT,
            )
    }
}
