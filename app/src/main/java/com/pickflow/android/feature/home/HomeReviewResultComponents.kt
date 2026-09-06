package com.pickflow.android.feature.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.pickflow.android.R
import com.pickflow.android.app.navigation.HomeTab
import com.pickflow.android.common.designsystem.PickflowColors
import com.pickflow.android.common.designsystem.PickflowTypography
import com.pickflow.android.core.services.protocols.ReviewDecision
import com.pickflow.android.core.services.protocols.ReviewResult

/** Figma 729:11199 — 3 × 114dp 하단 탭과 보관 탭의 4dp 결과 indicator. */
@Composable
fun HomeBottomNavigation(
    selectedTab: HomeTab,
    hasSavedIndicator: Boolean,
    onTabSelected: (HomeTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(PickflowColors.gray95)
            .drawBehind {
                drawLine(
                    color = PickflowColors.gray80,
                    start = Offset.Zero,
                    end = Offset(size.width, 0f),
                    strokeWidth = 1.dp.toPx(),
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Row(modifier = Modifier.width(342.dp)) {
            HomeTab.entries.forEach { tab ->
                HomeNavigationItem(
                    tab = tab,
                    selected = tab == selectedTab,
                    onClick = { onTabSelected(tab) },
                )
            }
        }
        if (hasSavedIndicator) {
            // Sibling of clickable tab semantics: the indicator remains independently discoverable
            // by accessibility and Compose UI tests instead of being merged into the tab node.
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(x = 16.dp, y = 3.dp)
                    .size(20.dp)
                    .semantics { contentDescription = "확인하지 않은 검수 결과 있음" }
                    .testTag("home-saved-indicator"),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(PickflowColors.sunsetOrange),
                )
            }
        }
    }
}

@Composable
private fun HomeNavigationItem(
    tab: HomeTab,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .width(114.dp)
            .height(64.dp)
            .clickable(role = Role.Tab, onClick = onClick)
            .testTag("home-tab-${tab.name.lowercase()}"),
    ) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Image(
                painter = painterResource(tab.iconRes(selected)),
                contentDescription = tab.label,
                modifier = Modifier.size(28.dp),
            )
            Text(
                text = tab.label,
                style = PickflowTypography.labelSmall,
                color = if (selected) PickflowColors.sunsetOrange else PickflowColors.gray50,
            )
        }
    }
}

/** Figma 729:11224 / 729:11253 — 결과별 카피와 액션을 가진 전역 snackbar. */
@Composable
fun ReviewResultSnackbar(
    result: ReviewResult,
    onOpenResult: (Long) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val approved = result.decision == ReviewDecision.APPROVED
    val rootTag = if (approved) "review-snackbar-approved" else "review-snackbar-rejected"
    val title = if (approved) "MY 스팟이 오픈됐어요!" else "MY 스팟 오픈이 반려되었어요"
    val body = if (approved) {
        "신청한 스팟이 등록되었어요."
    } else {
        "반려 사유를 확인해 주세요."
    }
    val action = if (approved) "바로 가기" else "확인하기"

    Row(
        modifier = modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .background(PickflowColors.gray0)
            .testTag(rootTag)
            .padding(start = 20.dp, top = 12.dp, end = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = PickflowTypography.bodyMediumBold,
                color = PickflowColors.gray95,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = body,
                style = PickflowTypography.bodySmall,
                color = PickflowColors.gray60,
            )
        }
        Text(
            text = action,
            style = PickflowTypography.bodyMediumBold,
            color = PickflowColors.sunsetOrange,
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .clickable { onOpenResult(result.spotId) }
                .padding(horizontal = 8.dp, vertical = 10.dp)
                .testTag("review-snackbar-action"),
        )
        Spacer(Modifier.width(4.dp))
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .clickable(onClick = onClose)
                .testTag("review-snackbar-close"),
            contentAlignment = Alignment.Center,
        ) {
            androidx.compose.material3.Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "닫기",
                tint = PickflowColors.gray60,
                modifier = Modifier.size(12.dp),
            )
        }
    }
}

private fun HomeTab.iconRes(selected: Boolean): Int = when (this) {
    HomeTab.EXPLORE -> if (selected) R.drawable.ic_map_selected else R.drawable.ic_map
    HomeTab.SAVED -> if (selected) R.drawable.ic_bookmark_selected else R.drawable.ic_bookmark
    HomeTab.MY -> if (selected) R.drawable.ic_person_selected else R.drawable.ic_person
}
