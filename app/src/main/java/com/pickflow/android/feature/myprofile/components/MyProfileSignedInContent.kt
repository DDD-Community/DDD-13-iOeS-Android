package com.pickflow.android.feature.myprofile.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.pickflow.android.common.designsystem.PickflowColors
import com.pickflow.android.common.designsystem.PickflowTypography
import com.pickflow.android.core.services.protocols.MyPageHome

/**
 * iOS `MyProfileSignedInContent` 대응 — 로그인 상태 마이페이지.
 *
 * Figma 디자인 기준: 타이틀 → 프로필 행(아바타·닉네임·연결 상태·계정 관리) →
 * 2x stat 카드(저장한 스팟·기록한 스팟) → 메뉴 리스트 → divider → 약관/버전.
 */
@Composable
fun MyProfileSignedInContent(
    home: MyPageHome,
    onOpenAccount: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PickflowColors.gray95)
            .verticalScroll(rememberScrollState())
            .testTag("myprofile-signedin"),
    ) {
        // 1. 타이틀
        Text(
            text = "마이페이지",
            style = PickflowTypography.headingLarge,
            color = PickflowColors.gray0,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 24.dp),
        )

        // 2. 프로필 행
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
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
                    text = home.nickname,
                    style = PickflowTypography.headingMedium,
                    color = PickflowColors.gray0,
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(PickflowColors.sunsetOrange),
                    )
                    Spacer(Modifier.size(6.dp))
                    Text(
                        text = "카카오 계정 연결됨",
                        style = PickflowTypography.bodySmall,
                        color = PickflowColors.gray40,
                    )
                }
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, PickflowColors.gray70, RoundedCornerShape(8.dp))
                    .clickable(onClick = onOpenAccount)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Text(
                    text = "계정 관리",
                    style = PickflowTypography.bodyMedium,
                    color = PickflowColors.gray0,
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // 3. 2x stat 카드 Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StatCard(
                label = "저장한 스팟",
                value = home.savedSpotCount,
                modifier = Modifier.weight(1f),
            )
            StatCard(
                label = "기록한 스팟",
                value = home.recordedSpotCount,
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(24.dp))

        // 4. 메뉴 리스트
        MenuRow(icon = Icons.Outlined.Info, label = "고객센터 및 1:1 문의")
        MenuRow(icon = Icons.Outlined.Notifications, label = "알림 설정")
        MenuRow(icon = Icons.Outlined.Info, label = "공지사항")

        // 5. divider
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .height(1.dp)
                .background(PickflowColors.gray80),
        )

        // 6. 추가 메뉴
        MenuRow(icon = null, label = "약관 및 정책")
        AppVersionRow(version = "v1.0.0")
    }
}

@Composable
private fun StatCard(
    label: String,
    value: Long,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .height(92.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(PickflowColors.gray90),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            style = PickflowTypography.bodyMedium,
            color = PickflowColors.gray40,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = value.toString(),
            style = PickflowTypography.headingLarge,
            color = PickflowColors.gray0,
        )
    }
}

@Composable
private fun MenuRow(
    icon: ImageVector?,
    label: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = PickflowColors.gray0,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.size(12.dp))
        }
        Text(
            text = label,
            style = PickflowTypography.bodyLarge,
            color = PickflowColors.gray0,
        )
        Spacer(Modifier.weight(1f))
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = PickflowColors.gray50,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun AppVersionRow(version: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "앱 버전",
            style = PickflowTypography.bodyLarge,
            color = PickflowColors.gray0,
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = version,
            style = PickflowTypography.bodyMedium,
            color = PickflowColors.gray40,
        )
    }
}
