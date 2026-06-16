package com.pickflow.android.feature.notice.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.pickflow.android.common.designsystem.PickflowColors
import com.pickflow.android.common.designsystem.PickflowTypography
import com.pickflow.android.common.util.formatNoticeDate
import com.pickflow.android.core.services.protocols.BoardPostDetail

/**
 * 공지사항 상세 본문. 제목 + 작성일 + 평문 본문(Text 그대로).
 *
 * BE 응답 `content: String` 가 평문이라는 가정(§10.E Phase E-1). 마크다운/HTML 포맷이
 * 도입되면 별도 렌더러(`MarkdownText` 등) 로 교체.
 */
@Composable
fun NoticeDetailContent(
    detail: BoardPostDetail,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PickflowColors.gray95)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp)
            .testTag("notice-detail-content"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = detail.title,
            style = PickflowTypography.headingMedium,
            color = PickflowColors.gray0,
        )
        Text(
            text = formatNoticeDate(detail.createdAt),
            style = PickflowTypography.bodySmall,
            color = PickflowColors.gray50,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(PickflowColors.gray80),
        )
        Text(
            text = detail.content,
            style = PickflowTypography.bodyLarge,
            color = PickflowColors.gray0,
        )
    }
}
