package com.pickflow.android.feature.notice.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.pickflow.android.common.designsystem.PickflowColors
import com.pickflow.android.common.designsystem.PickflowTypography
import com.pickflow.android.common.util.formatNoticeDate
import com.pickflow.android.core.services.protocols.BoardPost

/** 공지사항 목록의 단일 행. pinned=true 시 좌측에 sunsetOrange "공지" 배지. */
@Composable
fun NoticeRow(
    post: BoardPost,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .testTag("notice-row-${post.postId}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (post.pinned) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(PickflowColors.sunsetOrange)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Text(
                    text = "공지",
                    style = PickflowTypography.labelSmall,
                    color = PickflowColors.gray0,
                )
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = post.title,
                style = PickflowTypography.bodyLargeBold,
                color = PickflowColors.gray0,
                maxLines = 2,
            )
            Text(
                text = formatNoticeDate(post.createdAt),
                style = PickflowTypography.bodySmall,
                color = PickflowColors.gray50,
            )
        }
    }
}
