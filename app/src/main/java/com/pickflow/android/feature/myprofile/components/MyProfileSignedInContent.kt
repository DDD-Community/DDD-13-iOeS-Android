package com.pickflow.android.feature.myprofile.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.pickflow.android.common.designsystem.PickflowColors
import com.pickflow.android.common.designsystem.PickflowTypography

/**
 * iOS `MyProfileSignedInContent` 대응 — 로그인 상태 마이페이지.
 *
 * 프로필 헤더(아바타·닉네임·이메일·chevron) + 구분선 + 메뉴 행 목록.
 * 프로필 사진은 사용자 업로드 사진 에셋 자리라 Material `Person` 아이콘으로
 * 자리만 잡는다(에셋 치환 규칙).
 */
@Composable
fun MyProfileSignedInContent(
    nickname: String = "capybara123",
    email: String = "test@example.com",
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PickflowColors.gray95)
            .testTag("myprofile-signedin"),
    ) {
        // profile header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(PickflowColors.gray80),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = null,
                    tint = PickflowColors.gray50,
                    modifier = Modifier.size(38.dp),
                )
            }
            Spacer(Modifier.size(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = nickname,
                    style = PickflowTypography.headingSmall,
                    color = PickflowColors.gray0,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = email,
                    style = PickflowTypography.bodySmall,
                    color = PickflowColors.gray40,
                )
            }
            Icon(
                imageVector = Icons.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = PickflowColors.gray50,
                modifier = Modifier.size(24.dp),
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(PickflowColors.gray80),
        )

        MenuRow("계정 관리")
        MenuRow("공지사항")
        MenuRow("고객센터")
        MenuRow("앱 정보")
    }
}

@Composable
private fun MenuRow(title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = PickflowTypography.bodyLarge,
            color = PickflowColors.gray0,
        )
        Spacer(Modifier.weight(1f))
        Icon(
            imageVector = Icons.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = PickflowColors.gray50,
            modifier = Modifier.size(22.dp),
        )
    }
}
