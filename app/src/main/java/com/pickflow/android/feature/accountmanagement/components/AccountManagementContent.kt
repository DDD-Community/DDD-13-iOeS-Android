package com.pickflow.android.feature.accountmanagement.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.pickflow.android.R
import com.pickflow.android.common.designsystem.PickflowColors
import com.pickflow.android.common.designsystem.PickflowTypography

/** iOS `Color.red`(dark) 근사 — 회원탈퇴 텍스트. */
private val SystemRed = Color(0xFFFF453A)

/**
 * iOS `AccountManagementPreviewView` 1:1 이식 — 계정 관리 화면.
 *
 * navBar + (프로필 이미지 / 닉네임 / 연결 소셜 / 구분선 / 계정 액션) 스크롤 영역.
 * 프로필 사진 자리는 Material `Person`, 카메라 배지는 이모지(📷)로 치환.
 */
@Composable
fun AccountManagementContent(
    nicknameDraft: String = "capybara123",
    isSaveEnabled: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PickflowColors.gray95)
            .testTag("accountmanagement-content"),
    ) {
        NavBar(isSaveEnabled = isSaveEnabled)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 24.dp, bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(32.dp),
        ) {
            ProfileImageSection()
            FieldSection(label = "닉네임") {
                Text(
                    text = nicknameDraft,
                    style = PickflowTypography.bodyLarge,
                    color = PickflowColors.gray0,
                )
            }
            FieldSection(label = "연결된 소셜") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "카카오로 로그인됨",
                        style = PickflowTypography.bodyMedium,
                        color = PickflowColors.gray20,
                    )
                    Spacer(Modifier.weight(1f))
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = PickflowColors.sunsetOrange,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(PickflowColors.gray80),
            )
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "로그아웃",
                    style = PickflowTypography.bodyMedium,
                    color = PickflowColors.gray40,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                )
                Text(
                    text = "회원탈퇴",
                    style = PickflowTypography.bodyMedium,
                    color = SystemRed,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                )
            }
        }
    }
}

@Composable
private fun NavBar(isSaveEnabled: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(44.dp), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Filled.KeyboardArrowLeft,
                contentDescription = "뒤로",
                tint = PickflowColors.gray0,
                modifier = Modifier.size(28.dp),
            )
        }
        Spacer(Modifier.weight(1f))
        Text(
            text = "계정 관리",
            style = PickflowTypography.headingSmall,
            color = PickflowColors.gray0,
        )
        Spacer(Modifier.weight(1f))
        Box(modifier = Modifier.size(44.dp), contentAlignment = Alignment.Center) {
            Text(
                text = "저장",
                style = PickflowTypography.bodyMediumBold,
                color = if (isSaveEnabled) PickflowColors.sunsetOrange else PickflowColors.gray50,
            )
        }
    }
}

@Composable
private fun ProfileImageSection() {
    Box(contentAlignment = Alignment.BottomEnd) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(PickflowColors.gray80),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Person,
                contentDescription = null,
                tint = PickflowColors.gray50,
                modifier = Modifier.size(48.dp),
            )
        }
        // 카메라 배지. 벡터 자체가 흰 원 배경 + 테두리를 포함하므로 별도 Box 로 감싸지 않는다.
        // 멀티컬러라 tint 를 걸면 뭉개지므로 Icon 이 아니라 Image 로 그린다.
        Image(
            painter = painterResource(R.drawable.ic_my_camera),
            contentDescription = null,
            modifier = Modifier
                .size(36.dp)
                .testTag("account-profile-camera"),
        )
    }
}

@Composable
private fun FieldSection(label: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = label,
            style = PickflowTypography.labelMedium,
            color = PickflowColors.gray40,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(PickflowColors.gray80)
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            content()
        }
    }
}
