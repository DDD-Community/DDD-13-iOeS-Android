package com.pickflow.android.feature.accountmanagement

import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pickflow.android.common.designsystem.PickflowColors
import com.pickflow.android.common.designsystem.PickflowTypography
import com.pickflow.android.core.services.protocols.ImagePayload
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountManagementScreen(
    onBack: () -> Unit,
    onSignedOut: () -> Unit,
    onOpenWithdrawal: () -> Unit = {},
    viewModel: AccountManagementViewModel = hiltViewModel(),
) {
    val signedOut by viewModel.signedOut.collectAsStateWithLifecycle()
    val nicknameDraft by viewModel.nicknameDraft.collectAsStateWithLifecycle()
    val isSaveEnabled by viewModel.isSaveEnabled.collectAsStateWithLifecycle()
    val profileImageUrl by viewModel.profileImageUrl.collectAsStateWithLifecycle()
    val draftPreviewUri by viewModel.draftImagePreviewUri.collectAsStateWithLifecycle()

    LaunchedEffect(signedOut) {
        if (signedOut) onSignedOut()
    }

    val context = LocalContext.current
    val photoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val resolver = context.contentResolver
        val bytes = resolver.openInputStream(uri)?.use { it.readBytes() }
            ?: return@rememberLauncherForActivityResult
        val mimeType = resolver.getType(uri) ?: "image/jpeg"
        val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: "jpg"
        viewModel.setDraftImage(
            payload = ImagePayload(bytes = bytes, mimeType = mimeType, filename = "profile.$ext"),
            previewUri = uri.toString(),
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
                actions = {
                    TextButton(
                        onClick = viewModel::save,
                        enabled = isSaveEnabled,
                        modifier = Modifier.testTag("account-save"),
                    ) {
                        Text(
                            text = "저장",
                            style = PickflowTypography.bodyMediumBold,
                            color = if (isSaveEnabled) {
                                PickflowColors.sunsetOrange
                            } else {
                                PickflowColors.gray50
                            },
                        )
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
                .padding(horizontal = 20.dp)
                .testTag("accountmanagement-screen"),
        ) {
            Spacer(Modifier.height(24.dp))

            // iOS `AccountManagementView.profileImageSection` 1:1 — PhotosPicker 트리거.
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clickable {
                            photoLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                            )
                        }
                        .testTag("account-profile-image"),
                ) {
                    val preview = draftPreviewUri
                    val remoteUrl = profileImageUrl
                    when {
                        preview != null -> AsyncImage(
                            model = preview,
                            contentDescription = "선택한 프로필 사진",
                            modifier = Modifier
                                .size(96.dp)
                                .clip(CircleShape)
                                .background(PickflowColors.gray80, CircleShape),
                            contentScale = ContentScale.Crop,
                        )
                        !remoteUrl.isNullOrBlank() -> AsyncImage(
                            model = remoteUrl,
                            contentDescription = "프로필 사진",
                            modifier = Modifier
                                .size(96.dp)
                                .clip(CircleShape)
                                .background(PickflowColors.gray80, CircleShape),
                            contentScale = ContentScale.Crop,
                        )
                        else -> Box(
                            modifier = Modifier
                                .size(96.dp)
                                .background(PickflowColors.gray80, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Person,
                                contentDescription = null,
                                tint = PickflowColors.gray40,
                                modifier = Modifier.size(48.dp),
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .align(Alignment.BottomEnd)
                            .background(PickflowColors.gray80, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(text = "📷", style = PickflowTypography.bodyMedium)
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            Text(
                text = "닉네임",
                style = PickflowTypography.bodyMedium,
                color = PickflowColors.gray40,
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = nicknameDraft,
                onValueChange = viewModel::updateNickname,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("account-nickname-field"),
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = PickflowColors.gray80,
                    unfocusedContainerColor = PickflowColors.gray80,
                    disabledContainerColor = PickflowColors.gray80,
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    disabledBorderColor = Color.Transparent,
                    focusedTextColor = PickflowColors.gray0,
                    unfocusedTextColor = PickflowColors.gray0,
                ),
            )

            Spacer(Modifier.height(24.dp))

            Text(
                text = "연결된 소셜",
                style = PickflowTypography.bodyMedium,
                color = PickflowColors.gray40,
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PickflowColors.gray80, RoundedCornerShape(10.dp))
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                Text(
                    text = "카카오로 로그인됨",
                    style = PickflowTypography.bodyMedium,
                    color = PickflowColors.gray40,
                )
            }

            Spacer(Modifier.weight(1f))

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "로그아웃",
                    style = PickflowTypography.bodyMedium,
                    color = PickflowColors.gray0,
                    modifier = Modifier
                        .clickable(onClick = viewModel::logout)
                        .testTag("account-logout"),
                )
                Spacer(Modifier.height(24.dp))
                Text(
                    text = "회원탈퇴",
                    style = PickflowTypography.bodyMedium,
                    color = Color(0xFFFF453A),
                    modifier = Modifier
                        .clickable(onClick = onOpenWithdrawal)
                        .testTag("account-withdraw"),
                )
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}
