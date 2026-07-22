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
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.pickflow.android.feature.accountmanagement.components.LogoutConfirmDialogOverlay
import coil.compose.AsyncImage
import kotlinx.coroutines.delay

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
    val nicknameError by viewModel.nicknameError.collectAsStateWithLifecycle()
    val provider by viewModel.provider.collectAsStateWithLifecycle()
    val toastMessage by viewModel.toast.collectAsStateWithLifecycle()

    var showLogoutDialog by remember { mutableStateOf(false) }
    var toastVisible by remember { mutableStateOf(false) }

    LaunchedEffect(signedOut) {
        if (signedOut) onSignedOut()
    }
    LaunchedEffect(toastMessage) {
        if (toastMessage != null) {
            toastVisible = true
            delay(3000)
            toastVisible = false
            viewModel.consumeToast()
        }
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

    // 카메라 촬영 — FileProvider 임시 파일에 저장 후 bytes 추출.
    var cameraUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture(),
    ) { success ->
        val uri = cameraUri
        if (!success || uri == null) return@rememberLauncherForActivityResult
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: return@rememberLauncherForActivityResult
        viewModel.setDraftImage(
            payload = ImagePayload(bytes = bytes, mimeType = "image/jpeg", filename = "profile.jpg"),
            previewUri = uri.toString(),
        )
    }
    val launchCamera = launchCamera@{
        val dir = java.io.File(context.cacheDir, "images").apply { mkdirs() }
        val file = java.io.File(dir, "camera_${System.currentTimeMillis()}.jpg")
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context, "${context.packageName}.fileprovider", file,
        )
        cameraUri = uri
        cameraLauncher.launch(uri)
    }
    val launchAlbum = {
        photoLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
        )
    }
    var showPhotoSheet by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
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
                        .clickable { showPhotoSheet = true }
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
            nicknameError?.let { error ->
                Spacer(Modifier.height(6.dp))
                Text(
                    text = error,
                    style = PickflowTypography.labelMedium,
                    color = PickflowColors.sunsetOrange,
                    modifier = Modifier.testTag("account-nickname-error"),
                )
            }

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
                    text = "${provider?.displayName ?: "소셜"} 계정으로 로그인됨",
                    style = PickflowTypography.bodyMedium,
                    color = PickflowColors.gray40,
                )
            }

            // 시안(iOS) 1:1 — 로그아웃/회원탈퇴는 하단 고정이 아니라 연결된 소셜 바로 아래 배치.
            Spacer(Modifier.height(40.dp))

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
                        .clickable { showLogoutDialog = true }
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

        if (showLogoutDialog) {
            LogoutConfirmDialogOverlay(
                onCancel = { showLogoutDialog = false },
                onConfirm = {
                    showLogoutDialog = false
                    viewModel.logout()
                },
            )
        }

        // 저장 결과 토스트 — "저장되었습니다." (화면 중앙, 3초 후 자동 소멸).
        if (toastVisible && toastMessage != null) {
            SaveResultToast(
                message = toastMessage ?: "",
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }

    if (showPhotoSheet) {
        ModalBottomSheet(
            onDismissRequest = { showPhotoSheet = false },
            containerColor = PickflowColors.gray90,
        ) {
            PhotoSourceRow("카메라 촬영") {
                showPhotoSheet = false
                launchCamera()
            }
            PhotoSourceRow("앨범에서 선택") {
                showPhotoSheet = false
                launchAlbum()
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

/** 시안 공통 토스트(체크 아이콘 + gray0 배경) — 스팟 상세 토스트와 동일 스타일. */
@Composable
private fun SaveResultToast(
    message: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(PickflowColors.gray0)
            .padding(horizontal = 20.dp, vertical = 14.dp)
            .testTag("account-save-toast"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = PickflowColors.gray95,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = message,
            style = PickflowTypography.bodyMediumBold,
            color = PickflowColors.gray95,
        )
    }
}

@Composable
private fun PhotoSourceRow(label: String, onClick: () -> Unit) {
    Text(
        text = label,
        style = PickflowTypography.bodyLarge,
        color = PickflowColors.gray0,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 18.dp),
    )
}
