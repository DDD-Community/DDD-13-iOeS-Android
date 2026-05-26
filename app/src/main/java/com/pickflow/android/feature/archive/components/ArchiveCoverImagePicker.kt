package com.pickflow.android.feature.archive.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.pickflow.android.core.services.protocols.ImagePayload

/**
 * iOS `ArchiveCoverImagePickerView` 의 Android 대응 — 시스템 PickVisualMedia 사용.
 *
 * iOS는 PHPhotoLibrary 권한 + 커스텀 사진첩 그리드를 제공하지만,
 * Android 13+ Photo Picker(`PickVisualMedia`)는 권한 없이 동일 UX를 보장하므로
 * 1:1 이식 대신 OS-native 피커로 위임한다.
 *
 * [trigger] 가 true 로 바뀌면 1회 launch 하고, 사용자가 사진을 선택하면
 * [onPicked] 로 ImagePayload 를 콜백한다. 미선택/취소 시 [onCancel] 호출.
 */
@Composable
fun rememberCoverImagePickerLauncher(
    onPicked: (ImagePayload) -> Unit,
    onCancel: () -> Unit = {},
): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri: Uri? ->
        if (uri == null) {
            onCancel()
            return@rememberLauncherForActivityResult
        }
        runCatching {
            val resolver = context.contentResolver
            val mime = resolver.getType(uri) ?: "image/jpeg"
            val bytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: ByteArray(0)
            val filename = uri.lastPathSegment?.substringAfterLast('/') ?: "cover.jpg"
            ImagePayload(bytes = bytes, mimeType = mime, filename = filename)
        }.onSuccess(onPicked).onFailure { onCancel() }
    }
    return remember(launcher) {
        {
            launcher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
            )
        }
    }
}
