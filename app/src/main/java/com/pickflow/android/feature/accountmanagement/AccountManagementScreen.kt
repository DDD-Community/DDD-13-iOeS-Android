package com.pickflow.android.feature.accountmanagement

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pickflow.android.common.designsystem.PickflowColors
import com.pickflow.android.common.designsystem.PickflowTypography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountManagementScreen(
    onBack: () -> Unit,
    onSignedOut: () -> Unit,
    viewModel: AccountManagementViewModel = hiltViewModel(),
) {
    val signedOut by viewModel.signedOut.collectAsStateWithLifecycle()
    val withdrawDialogVisible by viewModel.withdrawDialogVisible.collectAsStateWithLifecycle()

    LaunchedEffect(signedOut) {
        if (signedOut) onSignedOut()
    }

    if (withdrawDialogVisible) {
        AlertDialog(
            onDismissRequest = viewModel::dismissWithdraw,
            title = { Text("회원 탈퇴") },
            text = { Text("탈퇴하면 저장한 스팟과 계정 정보가 모두 삭제돼요. 계속할까요?") },
            confirmButton = {
                TextButton(
                    onClick = viewModel::confirmWithdraw,
                    modifier = Modifier.testTag("account-withdraw-confirm"),
                ) { Text("탈퇴하기", color = PickflowColors.sunsetOrange) }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissWithdraw) { Text("취소") }
            },
            containerColor = PickflowColors.gray90,
        )
    }

    Scaffold(
        containerColor = PickflowColors.gray95,
        topBar = {
            TopAppBar(
                title = { Text("계정 관리", style = PickflowTypography.bodyLargeBold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("account-back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PickflowColors.gray95,
                    titleContentColor = PickflowColors.gray0,
                    navigationIconContentColor = PickflowColors.gray0,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp)
                .testTag("accountmanagement-screen"),
        ) {
            InfoRow(label = "닉네임", value = "Pickflow User")
            InfoRow(label = "이메일", value = "user@pickflow.app")
            InfoRow(label = "연결 계정", value = "카카오")
            Spacer(Modifier.height(24.dp))
            ActionRow(
                label = "로그아웃",
                testTag = "account-logout",
                onClick = viewModel::logout,
            )
            ActionRow(
                label = "회원 탈퇴",
                testTag = "account-withdraw",
                destructive = true,
                onClick = viewModel::requestWithdraw,
            )
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
    ) {
        Text(
            text = label,
            style = PickflowTypography.bodyMedium,
            color = PickflowColors.gray40,
            modifier = Modifier.height(24.dp),
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = value,
            style = PickflowTypography.bodyMedium,
            color = PickflowColors.gray0,
        )
    }
}

@Composable
private fun ActionRow(
    label: String,
    testTag: String,
    destructive: Boolean = false,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(PickflowColors.gray90, RoundedCornerShape(12.dp))
            .testTag(testTag)
            .padding(horizontal = 16.dp, vertical = 16.dp),
    ) {
        Text(
            text = label,
            style = PickflowTypography.bodyMedium,
            color = if (destructive) PickflowColors.sunsetOrange else PickflowColors.gray10,
        )
    }
    Spacer(Modifier.height(10.dp))
}
