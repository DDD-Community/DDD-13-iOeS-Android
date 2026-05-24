package com.pickflow.android.feature.myprofile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pickflow.android.common.designsystem.PickflowColors
import com.pickflow.android.common.designsystem.PickflowTypography
import com.pickflow.android.common.ui.LoadStateContent

@Composable
fun MyProfileScreen(
    onRequireLogin: () -> Unit,
    onOpenDebug: () -> Unit = {},
    onOpenAccount: () -> Unit = {},
    viewModel: MyProfileViewModel = hiltViewModel(),
) {
    val userName by viewModel.userName.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.loadUserName() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PickflowColors.gray95)
            .testTag("myprofile-screen"),
    ) {
        LoadStateContent(
            state = userName,
            emptyMessage = "프로필 정보가 없어요.",
            onRetry = viewModel::loadUserName,
        ) { name ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(48.dp))
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(PickflowColors.gray80),
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = name,
                    style = PickflowTypography.headingMedium,
                    color = PickflowColors.gray0,
                )
                Spacer(Modifier.height(32.dp))
                ProfileRow("계정 관리", onClick = onOpenAccount)
                ProfileRow("알림 설정")
                ProfileRow("개발자 메뉴", onClick = onOpenDebug)
                ProfileRow("로그아웃", onClick = onRequireLogin)
            }
        }
    }
}

@Composable
private fun ProfileRow(label: String, onClick: (() -> Unit)? = null) {
    OutlinedButton(
        onClick = { onClick?.invoke() },
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
    ) {
        Text(
            text = label,
            style = PickflowTypography.bodyMedium,
            color = PickflowColors.gray10,
            modifier = Modifier.fillMaxWidth(),
        )
    }
    Spacer(Modifier.height(4.dp))
}
