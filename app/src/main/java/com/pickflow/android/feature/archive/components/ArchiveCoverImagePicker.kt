package com.pickflow.android.feature.archive.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.pickflow.android.core.services.protocols.ImagePayload
import java.io.ByteArrayOutputStream

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
            val raw = resolver.openInputStream(uri)?.use { it.readBytes() }
                ?: error("이미지를 읽을 수 없습니다.")
            // 서버는 image/png 또는 image/jpeg 만 허용. PNG 헤더를 sniff 해서 PNG 면 그대로 전송하고,
            // 그 외(HEIC/WebP/확장자 없는 파일명 등)는 Bitmap 으로 decode 후 JPEG 으로 re-encode 한다.
            val isPng = raw.size >= 4 &&
                raw[0] == 0x89.toByte() &&
                raw[1] == 0x50.toByte() &&
                raw[2] == 0x4E.toByte() &&
                raw[3] == 0x47.toByte()
            if (isPng) {
                ImagePayload(bytes = raw, mimeType = "image/png", filename = "archive.png")
            } else {
                val bitmap = BitmapFactory.decodeByteArray(raw, 0, raw.size)
                    ?: error("이미지 디코딩에 실패했습니다.")
                val jpegBytes = ByteArrayOutputStream().use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
                    out.toByteArray()
                }
                ImagePayload(bytes = jpegBytes, mimeType = "image/jpeg", filename = "archive.jpg")
            }
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

private const val JPEG_QUALITY = 90
