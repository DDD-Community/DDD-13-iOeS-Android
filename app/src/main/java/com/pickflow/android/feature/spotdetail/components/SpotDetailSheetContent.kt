package com.pickflow.android.feature.spotdetail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pickflow.android.R
import com.pickflow.android.common.designsystem.PickflowColors
import com.pickflow.android.common.designsystem.PickflowTypography

/**
 * iOS `SheetChromeView` 1:1 — 상단 24 패딩 + gray95 배경 + 상단 코너 20 라운드.
 */
@Composable
fun SpotSheetChrome(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .background(PickflowColors.gray95)
            .padding(top = 24.dp),
    ) {
        content()
    }
}

/**
 * iOS `SpotDetailSheetContentView` 1:1 — 바텀시트 컨텐츠.
 *
 * 이름·MY배지·닫기 / 테마·북마크 / 거리·주소(펼침) / 사진 / 액션 버튼.
 */
@Composable
fun SpotDetailSheetContent(
    spot: SpotDetailData,
    isBookmarked: Boolean,
    addressExpanded: Boolean,
    modifier: Modifier = Modifier,
    onClose: () -> Unit = {},
    onRoute: () -> Unit = {},
    onSave: () -> Unit = {},
) {
    var expanded by remember(spot.name) { mutableStateOf(addressExpanded) }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 26.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = spot.name,
                        style = PickflowTypography.headingLarge,
                        color = PickflowColors.gray5,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (spot.isMine) {
                        Text(
                            text = "MY 스팟",
                            style = PickflowTypography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = PickflowColors.sunsetOrange,
                            modifier = Modifier
                                .border(1.dp, PickflowColors.sunsetOrange, RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.15f))
                        .clickable(onClick = onClose)
                        .testTag("spotsheet-close"),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "닫기",
                        tint = PickflowColors.gray0,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }

            // 테마 · 북마크
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(spot.theme.displayName, style = PickflowTypography.bodyMedium, color = PickflowColors.gray10)
                Dot()
                Text(
                    text = "북마크 ${spot.bookmarkCount}",
                    style = PickflowTypography.bodyMedium,
                    color = PickflowColors.gray10,
                )
            }

            // 거리 · 주소(펼침 토글). 화살표 터치 시 도로명/지번 박스 노출.
            val hasDetailAddress = !spot.addressRoad.isNullOrBlank() || !spot.addressJibun.isNullOrBlank()
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = spot.distanceKm?.let { "%.1fkm".format(it) } ?: "-",
                    style = PickflowTypography.bodyMediumBold,
                    color = PickflowColors.gray10,
                )
                Dot()
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = if (hasDetailAddress) {
                        Modifier.clickable { expanded = !expanded }.testTag("spotsheet-address-toggle")
                    } else {
                        Modifier
                    },
                ) {
                    Text(spot.address, style = PickflowTypography.bodyMedium, color = PickflowColors.gray0)
                    if (hasDetailAddress) {
                        Icon(
                            imageVector = if (expanded) Icons.Filled.KeyboardArrowUp
                            else Icons.Filled.KeyboardArrowDown,
                            contentDescription = if (expanded) "주소 접기" else "주소 펼치기",
                            tint = PickflowColors.gray10,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }

            // 펼침 시 도로명/지번 박스(인라인).
            if (expanded && hasDetailAddress) {
                AddressBox(road = spot.addressRoad, jibun = spot.addressJibun)
            }
        }

        // 사진 영역
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(PickflowColors.gray90),
        ) {
            if (!spot.imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = spot.imageUrl,
                    contentDescription = spot.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        SheetActionButtons(
            isMine = spot.isMine,
            isBookmarked = isBookmarked,
            onRoute = onRoute,
            onSave = onSave,
        )
    }
}

@Composable
private fun Dot() {
    Box(
        modifier = Modifier
            .size(3.dp)
            .clip(RoundedCornerShape(50))
            .background(PickflowColors.gray50),
    )
}

@Composable
private fun AddressBox(road: String?, jibun: String?, modifier: Modifier = Modifier) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val onCopy: (String) -> Unit = { value ->
        clipboard.setText(AnnotatedString(value))
        android.widget.Toast.makeText(context, "주소가 복사되었습니다.", android.widget.Toast.LENGTH_SHORT).show()
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(PickflowColors.gray90)
            .border(1.dp, PickflowColors.gray80, RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (!road.isNullOrBlank()) AddressLine(label = "도로명", value = road, onCopy = onCopy)
        if (!jibun.isNullOrBlank()) AddressLine(label = "지번", value = jibun, onCopy = onCopy)
    }
}

@Composable
private fun AddressLine(label: String, value: String, onCopy: (String) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(label, style = PickflowTypography.bodySmall, color = PickflowColors.gray30)
        Text(
            text = value,
            style = PickflowTypography.bodySmall,
            color = PickflowColors.gray0,
            modifier = Modifier.weight(1f, fill = false),
        )
        Text(
            text = "복사",
            style = PickflowTypography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
            color = PickflowColors.sunsetOrange,
            modifier = Modifier
                .clickable { onCopy(value) }
                .padding(horizontal = 4.dp, vertical = 2.dp)
                .testTag("spotsheet-address-copy"),
        )
    }
}

/** iOS `SpotBottomSheetActionButtons` 1:1 — 길 안내 + 저장(또는 mine: 길 안내만). */
@Composable
private fun SheetActionButtons(
    isMine: Boolean,
    isBookmarked: Boolean,
    onRoute: () -> Unit,
    onSave: () -> Unit,
) {
    // 길 안내 / 저장 버튼 높이를 동일하게(56dp) 맞춘다.
    if (isMine) {
        RouteButton(modifier = Modifier.fillMaxWidth(), onClick = onRoute)
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            RouteButton(modifier = Modifier.weight(1f), onClick = onRoute)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(PickflowColors.gray0)
                    .clickable(onClick = onSave)
                    .testTag("spotsheet-save"),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painter = painterResource(
                            id = if (isBookmarked) R.drawable.ic_bookmark_filled
                            else R.drawable.ic_bookmark_border,
                        ),
                        contentDescription = null,
                        tint = PickflowColors.gray80,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = if (isBookmarked) "저장됨" else "저장하기",
                        style = PickflowTypography.bodyLargeBold,
                        color = PickflowColors.gray80,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun RouteButton(modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    Row(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(PickflowColors.sunsetOrange)
            .clickable(onClick = onClick)
            .testTag("spotsheet-route")
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.Send,
            contentDescription = null,
            tint = PickflowColors.gray0,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = "길 안내 받기",
            style = PickflowTypography.bodyLargeBold,
            color = PickflowColors.gray0,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
    }
}
