package com.pickflow.android.feature.map.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.pickflow.android.common.designsystem.PickflowColors
import com.pickflow.android.common.designsystem.PickflowTypography
import com.pickflow.android.feature.map.MoodFilter

/**
 * 탐색 탭 무드 필터 행 — Figma `Category` 컴포넌트 세트(`1:43761`) 1:1.
 *
 * **지도와 리스트가 이 하나를 공유한다.** 예전에는 두 화면이 각자 캡슐을 그려
 * 패딩·아이콘 크기·타이포가 서로 달랐다(PV-59에서 통합).
 *
 * 사양(Figma `729:7837` Default / `733:11506` Selected):
 * - 캡슐: **84×40 고정**, 코너 8, 배경 `gray95`(#131416), 아이콘–라벨 간격 6 (내용은 중앙 정렬)
 * - 아이콘 20dp, 라벨 `bodyLargeBold`(17sp), 라벨색은 선택 여부와 무관하게 `gray0`
 * - 선택 시에만 `sunsetOrange`(#FA6133) 1dp 보더가 붙는다
 * - 행: 패딩 16×8, 캡슐 간격 8
 *
 * stateless — Paparazzi 스냅샷이 직접 렌더한다(`HomeMapMoodFilterSnapshotTest`).
 */
@Composable
fun MoodFilterRow(
    selected: Set<MoodFilter>,
    onSelect: (MoodFilter) -> Unit,
    testTag: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag(testTag),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MoodFilter.entries.forEach { mood ->
            MoodCapsule(
                mood = mood,
                selected = mood in selected,
                onClick = { onSelect(mood) },
            )
        }
    }
}

@Composable
private fun MoodCapsule(mood: MoodFilter, selected: Boolean, onClick: () -> Unit) {
    // 캡슐 자체는 코너로 clip 되므로, 신규 dot 은 clip 밖(형제)에 그려 모서리에 걸치게 둔다.
    Box {
        Row(
            modifier = Modifier
                .size(width = CAPSULE_WIDTH, height = CAPSULE_HEIGHT)
                .clip(RoundedCornerShape(8.dp))
                .background(PickflowColors.gray95)
                .then(
                    if (selected) {
                        Modifier.border(1.dp, PickflowColors.sunsetOrange, RoundedCornerShape(8.dp))
                    } else {
                        Modifier
                    },
                )
                .clickable(onClick = onClick),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        ) {
            Image(
                painter = painterResource(mood.iconRes),
                contentDescription = mood.displayName,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = mood.displayName,
                style = PickflowTypography.bodyLargeBold,
                // Figma는 Default/Selected 모두 흰색이다. 구분은 보더가 진다.
                color = PickflowColors.gray0,
            )
        }

        if (mood.isNew) {
            // Figma: 4×4, color/Dark/Primary/normal(#FA6133). 캡슐 안쪽 우상단.
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = -NEW_DOT_INSET, y = NEW_DOT_INSET)
                    .size(NEW_DOT_SIZE)
                    .background(PickflowColors.sunsetOrange, CircleShape)
                    .testTag("mood-new-dot-${mood.name}"),
            )
        }
    }
}

/** 캡슐 고정 크기 — 라벨 길이와 무관하게 4개 칩의 폭이 같아야 한다. */
private val CAPSULE_WIDTH = 84.dp
private val CAPSULE_HEIGHT = 40.dp

/** 신규 무드 표시 dot 지름 — Figma 4×4. */
private val NEW_DOT_SIZE = 4.dp

/** dot 과 캡슐 우측/상단 모서리 사이 여백. */
private val NEW_DOT_INSET = 8.dp
