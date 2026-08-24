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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.pickflow.android.BuildConfig
import com.pickflow.android.common.designsystem.PickflowColors
import com.pickflow.android.common.designsystem.PickflowTypography
import com.pickflow.android.core.services.protocols.MyPageHome

/** "● {소셜} 계정 연결됨" 점 색상 (#FFA100). */
private val ConnectedDotColor = Color(0xFFFFA100)

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
    onOpenNotice: () -> Unit = {},
    onOpenTermsAndPolicy: () -> Unit = {},
    onOpenSavedSpots: () -> Unit = {},
    onOpenMySpots: () -> Unit = {},
    /** 운영이 아닌 서버를 볼 때 버전 뒤에 붙는 짧은 이름. 운영이면 null. */
    environmentSuffix: String? = null,
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
                    .background(PickflowColors.gray80)
                    .clickable(onClick = onOpenAccount)
                    .testTag("myprofile-avatar"),
                contentAlignment = Alignment.Center,
            ) {
                val imageUrl = home.profileImageUrl
                if (!imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = null,
                        tint = PickflowColors.gray50,
                        modifier = Modifier.size(38.dp),
                    )
                }
            }
            Spacer(Modifier.size(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = home.nickname,
                    style = PickflowTypography.headingMedium,
                    color = PickflowColors.gray0,
                )
                Spacer(Modifier.height(4.dp))
                home.provider?.let { provider ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(ConnectedDotColor),
                        )
                        Spacer(Modifier.size(6.dp))
                        Text(
                            text = "${provider.displayName} 계정 연결됨",
                            style = PickflowTypography.bodySmall,
                            color = PickflowColors.gray40,
                        )
                    }
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
                onClick = onOpenSavedSpots,
                modifier = Modifier.weight(1f),
            )
            StatCard(
                label = "기록한 스팟",
                value = home.recordedSpotCount,
                onClick = onOpenMySpots,
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(24.dp))

        // 4. 메뉴 리스트 — 공지사항 / 약관 및 정책 / 앱 버전.
        MenuRow(
            icon = Icons.Outlined.Info,
            label = "공지사항",
            onClick = onOpenNotice,
            tag = "myprofile-menu-notice",
        )

        MenuDivider()

        MenuRow(
            icon = null,
            label = "약관 및 정책",
            onClick = onOpenTermsAndPolicy,
            tag = "myprofile-menu-terms-policy",
        )

        MenuDivider()

        // 앱 버전 — 약관 화면 내부에서 마이 탭 최상위로 이동. 클릭 불가, 버전 후행 표시.
        AppVersionRow(environmentSuffix = environmentSuffix)
    }
}

@Composable
private fun MenuDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .height(1.dp)
            .background(PickflowColors.gray80),
    )
}

@Composable
private fun AppVersionRow(environmentSuffix: String?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 20.dp)
            .testTag("myprofile-menu-appversion"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "\uc571 \ubc84\uc804",
            style = PickflowTypography.bodyLarge,
            color = PickflowColors.gray0,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = environmentSuffix
                ?.let { "v${BuildConfig.VERSION_NAME} \u00b7 $it" }
                ?: "v${BuildConfig.VERSION_NAME}",
            style = PickflowTypography.bodyMedium,
            color = if (environmentSuffix != null) {
                PickflowColors.sunsetOrange
            } else {
                PickflowColors.gray40
            },
        )
    }
}

@Composable
private fun StatCard(
    label: String,
    value: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .height(92.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(PickflowColors.gray90)
            .clickable(onClick = onClick),
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
    onClick: () -> Unit = {},
    tag: String? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp)
            .then(if (tag != null) Modifier.testTag(tag) else Modifier),
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

