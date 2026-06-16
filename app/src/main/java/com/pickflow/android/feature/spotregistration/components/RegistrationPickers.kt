package com.pickflow.android.feature.spotregistration.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pickflow.android.common.designsystem.PickflowColors
import com.pickflow.android.common.designsystem.PickflowTypography
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import kotlin.math.roundToInt

/**
 * iOS `wheelPicker`(.pickerStyle(.wheel)) 1:1 — 가운데 정렬 + 스냅 휠.
 * 가운데 항목이 선택값. 스크롤이 멈추면 [onSelected]로 인덱스를 보고한다.
 */
@Composable
fun WheelColumn(
    count: Int,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    label: (Int) -> String,
    modifier: Modifier = Modifier,
    visibleCount: Int = 5,
    itemHeight: Dp = 36.dp,
) {
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex.coerceIn(0, (count - 1).coerceAtLeast(0)))
    val fling = rememberSnapFlingBehavior(lazyListState = listState)
    val density = LocalDensity.current
    val itemPx = with(density) { itemHeight.toPx() }

    val centerIndex by remember {
        derivedStateOf {
            val first = listState.firstVisibleItemIndex
            val offset = listState.firstVisibleItemScrollOffset
            (first + (offset / itemPx).roundToInt()).coerceIn(0, (count - 1).coerceAtLeast(0))
        }
    }
    LaunchedEffect(centerIndex) { onSelected(centerIndex) }

    Box(
        modifier = modifier.height(itemHeight * visibleCount),
        contentAlignment = Alignment.Center,
    ) {
        // 가운데 선택 하이라이트 밴드.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White.copy(alpha = 0.06f)),
        )
        LazyColumn(
            state = listState,
            flingBehavior = fling,
            contentPadding = PaddingValues(vertical = itemHeight * (visibleCount / 2)),
            modifier = Modifier.fillMaxSize(),
        ) {
            itemsIndexed(List(count) { it }) { index, _ ->
                val selected = index == centerIndex
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeight),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label(index),
                        style = if (selected) PickflowTypography.bodyLargeBold else PickflowTypography.bodyMedium,
                        color = if (selected) PickflowColors.gray0 else PickflowColors.gray50,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

/** iOS `CaptureDatePickerSheet` 1:1 — 년/월/일 휠 + 확인. 미래 날짜 비허용. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptureDatePickerSheet(
    initialDate: LocalDate?,
    onConfirm: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val today = remember { LocalDate.now() }
    val seed = (initialDate ?: today).let { if (it.isAfter(today)) today else it }

    var year by remember { mutableIntStateOf(seed.year) }
    var month by remember { mutableIntStateOf(seed.monthValue) }
    var day by remember { mutableIntStateOf(seed.dayOfMonth) }

    val years = remember { (2000..today.year).toList() }
    val months = (1..(if (year == today.year) today.monthValue else 12)).toList()
    val maxDay = run {
        val lengthOfMonth = YearMonth.of(year, month.coerceIn(1, 12)).lengthOfMonth()
        if (year == today.year && month == today.monthValue) minOf(lengthOfMonth, today.dayOfMonth) else lengthOfMonth
    }
    val days = (1..maxDay).toList()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = PickflowColors.spotCardBackground,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        dragHandle = null,
    ) {
        PickerSheetBody(
            title = "날짜 선택",
            onConfirm = {
                val safeMonth = month.coerceIn(1, months.last())
                val safeDay = day.coerceIn(1, days.last())
                onConfirm(LocalDate.of(year, safeMonth, safeDay))
            },
        ) {
            WheelColumn(
                count = years.size,
                selectedIndex = years.indexOf(year).coerceAtLeast(0),
                onSelected = { year = years[it] },
                label = { "${years[it]}년" },
                modifier = Modifier.weight(1f),
            )
            WheelColumn(
                count = months.size,
                selectedIndex = months.indexOf(month.coerceIn(1, months.last())).coerceAtLeast(0),
                onSelected = { month = months[it] },
                label = { "${months[it]}월" },
                modifier = Modifier.weight(1f),
            )
            WheelColumn(
                count = days.size,
                selectedIndex = days.indexOf(day.coerceIn(1, days.last())).coerceAtLeast(0),
                onSelected = { day = days[it] },
                label = { "${days[it]}일" },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** iOS `CaptureTimePickerSheet` 1:1 — 오전·오후/시/분 휠 + 확인. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptureTimePickerSheet(
    initialTime: LocalTime?,
    onConfirm: (LocalTime) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val seed = initialTime ?: LocalTime.now()

    var isPm by remember { mutableIntStateOf(if (seed.hour >= 12) 1 else 0) }
    var hour12 by remember { mutableIntStateOf(((seed.hour + 11) % 12) + 1) }
    var minute by remember { mutableIntStateOf(seed.minute) }

    val meridiems = listOf("오전", "오후")
    val hours = (1..12).toList()
    val minutes = (0..59).toList()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = PickflowColors.spotCardBackground,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        dragHandle = null,
    ) {
        PickerSheetBody(
            title = "시간 선택",
            onConfirm = {
                val baseHour = hour12 % 12
                val hour24 = if (isPm == 1) baseHour + 12 else baseHour
                onConfirm(LocalTime.of(hour24, minute))
            },
        ) {
            WheelColumn(
                count = meridiems.size,
                selectedIndex = isPm,
                onSelected = { isPm = it },
                label = { meridiems[it] },
                modifier = Modifier.weight(1f),
            )
            WheelColumn(
                count = hours.size,
                selectedIndex = hours.indexOf(hour12).coerceAtLeast(0),
                onSelected = { hour12 = hours[it] },
                label = { "${hours[it]}시" },
                modifier = Modifier.weight(1f),
            )
            WheelColumn(
                count = minutes.size,
                selectedIndex = minute.coerceIn(0, 59),
                onSelected = { minute = minutes[it] },
                label = { "%02d분".format(minutes[it]) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun PickerSheetBody(
    title: String,
    onConfirm: () -> Unit,
    wheels: @Composable RowScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp, bottom = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text(title, style = PickflowTypography.headingSmall, color = PickflowColors.gray0)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = wheels,
        )
        Box(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(PickflowColors.spotOrange)
                .clickable(onClick = onConfirm),
            contentAlignment = Alignment.Center,
        ) {
            Text("확인", style = PickflowTypography.bodyLargeBold, color = PickflowColors.gray0)
        }
    }
}
