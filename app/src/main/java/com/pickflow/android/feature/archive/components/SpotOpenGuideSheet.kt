package com.pickflow.android.feature.archive.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pickflow.android.R
import com.pickflow.android.common.designsystem.PickflowColors
import com.pickflow.android.common.designsystem.PickflowTypography

/**
 * "스팟 공개 OPEN!" 안내 바텀시트 — 보관함 > 나만의 스팟 탭 최초 1회 (Figma `1201-9416` / `1201-9439`).
 *
 * 버튼이 둘이다. 위(오렌지 아웃라인)는 "지금 하러 간다", 아래(흰 채움)는 "알겠다".
 * **어느 쪽을 눌러도, 심지어 스와이프로 닫아도 "봤음" 으로 친다** — 안내를 무한 반복하지 않기 위해서다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpotOpenGuideSheet(
    onGoToOpen: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onConfirm,
        sheetState = sheetState,
        containerColor = PickflowColors.gray95,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        dragHandle = null,
        modifier = modifier.testTag("spot-open-guide-sheet"),
    ) {
        SpotOpenGuideSheetContent(onGoToOpen = onGoToOpen, onConfirm = onConfirm)
    }
}

/**
 * Stateless 본체 — Paparazzi / Compose UI 테스트가 시트 컨테이너 없이 직접 호출한다
 * (`ModalBottomSheet` 은 별도 윈도우라 호스트 사이드 렌더에 얹기 어렵다).
 */
@Composable
fun SpotOpenGuideSheetContent(
    onGoToOpen: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(PickflowColors.gray95),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SheetDragHandle()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            GuideIllustration()

            Text(
                text = "스팟 공개 OPEN!",
                style = PickflowTypography.headingMedium,
                color = PickflowColors.gray0,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 24.dp),
            )
            Text(
                text = "내가 기록한 스팟을 다른 유저에게 공개할 수 있어요",
                style = PickflowTypography.bodyMedium,
                color = PickflowColors.gray20,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp),
            )

            // 폭이 내용에 맞춰 줄어드는 유일한 버튼 — 아래 "확인했어요" 는 전폭이다.
            Row(
                modifier = Modifier
                    .padding(top = 24.dp)
                    .height(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, PickflowColors.sunsetOrange, RoundedCornerShape(8.dp))
                    .clickable(onClick = onGoToOpen)
                    .padding(start = 20.dp, end = 12.dp)
                    .testTag("spot-open-guide-go"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "내 스팟 오픈하러 가기",
                    style = PickflowTypography.bodyMediumBold,
                    color = PickflowColors.sunsetOrange,
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = PickflowColors.sunsetOrange,
                    modifier = Modifier.size(18.dp),
                )
            }

            Box(
                modifier = Modifier
                    .padding(top = 12.dp)
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(PickflowColors.gray0)
                    .clickable(onClick = onConfirm)
                    .testTag("spot-open-guide-confirm"),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "확인했어요",
                    style = PickflowTypography.bodyLargeBold,
                    color = PickflowColors.gray100,
                )
            }
        }
    }
}

@Composable
private fun SheetDragHandle() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .width(40.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(PickflowColors.gray30),
        )
    }
}

/**
 * 기능을 보여주는 삽화 — 스팟 상세의 "내 스팟 오픈하기" 버튼이 담긴 목업.
 *
 * 원본(Figma `Detail-Default-My.svg`)은 사진이 base64 로 박힌 SVG 라 벡터 드로어블로 못 옮긴다.
 * xxhdpi PNG 로 래스터화해 넣었다(190x170dp @3x). 아래쪽 안내 문구가 잘려 보이는 건
 * 원본이 이미 그렇게 잘린 것이라 따로 클립하지 않는다.
 */
@Composable
private fun GuideIllustration() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(ILLUSTRATION_HEIGHT)
            .clip(RoundedCornerShape(12.dp))
            .background(PickflowColors.gray90)
            .testTag("spot-open-guide-illustration"),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.img_spot_open_guide),
            contentDescription = null,
            modifier = Modifier
                .width(ILLUSTRATION_WIDTH)
                .height(ILLUSTRATION_HEIGHT),
        )
    }
}

/** 원본 SVG 의 자체 크기. 늘리면 사진이 뭉개진다. */
private val ILLUSTRATION_WIDTH = 190.dp
private val ILLUSTRATION_HEIGHT = 170.dp
