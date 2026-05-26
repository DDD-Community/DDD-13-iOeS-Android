package com.pickflow.android.feature.spotdetail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.pickflow.android.common.designsystem.PickflowColors

/**
 * iOS `SpotDetailNavBar` 1:1 이식 — 우측 정렬 공유/닫기 버튼.
 *
 * iOS `icShare`/`icClose` 커스텀 에셋은 Material `Share`/`Close`로 치환.
 */
@Composable
fun SpotDetailNavBar(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    onShare: () -> Unit = {},
    onClose: () -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(PickflowColors.gray95)
            .padding(horizontal = 8.dp)
            .testTag("spotdetail-navbar"),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NavBarIcon(
            Icons.AutoMirrored.Filled.ArrowBack,
            "뒤로",
            onClick = onBack,
            tag = "detail-back",
        )
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            NavBarIcon(Icons.Filled.Share, "공유", onClick = onShare, tag = "detail-share")
            NavBarIcon(Icons.Filled.Close, "닫기", onClick = onClose, tag = "detail-close")
        }
    }
}

@Composable
private fun NavBarIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    tag: String,
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clickable(onClick = onClick)
            .testTag(tag),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = PickflowColors.gray0,
            modifier = Modifier.size(24.dp),
        )
    }
}
