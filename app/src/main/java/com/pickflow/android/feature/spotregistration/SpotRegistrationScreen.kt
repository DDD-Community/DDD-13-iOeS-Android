package com.pickflow.android.feature.spotregistration

import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Place
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.pickflow.android.R
import com.pickflow.android.common.designsystem.PickflowColors
import com.pickflow.android.common.designsystem.PickflowTypography
import com.pickflow.android.common.ui.LoadState
import com.pickflow.android.core.services.protocols.ImagePayload
import com.pickflow.android.core.services.protocols.SpotTheme
import com.pickflow.android.feature.spotlist.label
import com.pickflow.android.feature.spotregistration.components.CaptureDatePickerSheet
import com.pickflow.android.feature.spotregistration.components.CaptureTimePickerSheet
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DATE_DISPLAY = DateTimeFormatter.ofPattern("M월 d일 EEE", Locale.KOREAN)
private val TIME_DISPLAY = DateTimeFormatter.ofPattern("a h:mm", Locale.KOREAN)

/**
 * iOS `SpotRegistrationView` 1:1 — 사진/주소/이름/카테고리/촬영기록/코멘트 입력 + 등록.
 * 헤더 우측 "등록" 은 모든 필수값 입력 시 색상이 spotOrange 로 활성화된다.
 */
@Composable
fun SpotRegistrationScreen(
    onBack: () -> Unit,
    onOpenSearch: () -> Unit,
    onRegistered: (String) -> Unit,
    viewModel: SpotRegistrationViewModel = hiltViewModel(),
) {
    val selectedAddress by viewModel.selectedAddress.collectAsStateWithLifecycle()
    val distanceText by viewModel.distanceText.collectAsStateWithLifecycle()
    val spotName by viewModel.spotName.collectAsStateWithLifecycle()
    val theme by viewModel.theme.collectAsStateWithLifecycle()
    val capturedDate by viewModel.capturedDate.collectAsStateWithLifecycle()
    val capturedTime by viewModel.capturedTime.collectAsStateWithLifecycle()
    val comment by viewModel.comment.collectAsStateWithLifecycle()
    val imagePayload by viewModel.imagePayload.collectAsStateWithLifecycle()
    val selectedImageUri by viewModel.selectedImageUri.collectAsStateWithLifecycle()
    val submission by viewModel.submission.collectAsStateWithLifecycle()
    val isRegisterEnabled by viewModel.isRegisterEnabled.collectAsStateWithLifecycle()

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    LaunchedEffect(submission) {
        (submission as? LoadState.Loaded)?.let { onRegistered(it.value.spotId.toString()) }
    }

    val context = LocalContext.current
    val photoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val resolver = context.contentResolver
        val bytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: return@rememberLauncherForActivityResult
        val mimeType = resolver.getType(uri) ?: "image/jpeg"
        val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: "jpg"
        viewModel.setImagePayload(
            ImagePayload(bytes = bytes, mimeType = mimeType, filename = "spot.$ext"),
            previewUri = uri.toString(),
        )
    }

    if (showDatePicker) {
        CaptureDatePickerSheet(
            initialDate = capturedDate,
            onConfirm = { viewModel.setCapturedDate(it); showDatePicker = false },
            onDismiss = { showDatePicker = false },
        )
    }
    if (showTimePicker) {
        CaptureTimePickerSheet(
            initialTime = capturedTime,
            onConfirm = { viewModel.setCapturedTime(it); showTimePicker = false },
            onDismiss = { showTimePicker = false },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PickflowColors.gray95)
            .testTag("spotregistration-screen"),
    ) {
        RegistrationHeader(
            isRegisterEnabled = isRegisterEnabled,
            isSubmitting = submission is LoadState.Loading,
            onBack = onBack,
            onSubmit = viewModel::submit,
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            PhotoPickerCard(
                previewUri = selectedImageUri,
                hasImage = imagePayload != null,
                onPick = {
                    photoLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                    )
                },
            )

            selectedAddress?.let { address ->
                SpotAddressCard(
                    title = address.name,
                    address = address.fullAddress,
                    distanceText = distanceText,
                )
            }

            SpotSearchLocationButton(onClick = onOpenSearch)

            LabeledSection("스팟 이름") {
                CountedInput(
                    value = spotName,
                    onValueChange = viewModel::setSpotName,
                    placeholder = "이 장소를 무엇이라 부를까요?",
                    count = spotName.length,
                    maxCount = SpotRegistrationViewModel.MAX_NAME_LENGTH,
                    singleLine = true,
                    testTag = "registration-name",
                )
            }

            LabeledSection("사진 카테고리") {
                ThemeChipGroup(selected = theme, onToggle = viewModel::toggleTheme)
            }

            LabeledSection("촬영 기록 정보") {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SelectionField(
                        value = capturedDate?.format(DATE_DISPLAY),
                        placeholder = "날짜 선택",
                        modifier = Modifier.weight(1f),
                        testTag = "registration-date",
                        onClick = { showDatePicker = true },
                    )
                    SelectionField(
                        value = capturedTime?.format(TIME_DISPLAY),
                        placeholder = "시간 선택",
                        modifier = Modifier.weight(1f),
                        testTag = "registration-time",
                        onClick = { showTimePicker = true },
                    )
                }
            }

            LabeledSection("한 줄 코멘트") {
                CountedInput(
                    value = comment,
                    onValueChange = viewModel::setComment,
                    placeholder = "다른 사람을 위한 꿀팁이나\n촬영 후기를 남겨주세요.",
                    count = comment.length,
                    maxCount = SpotRegistrationViewModel.MAX_COMMENT_LENGTH,
                    singleLine = false,
                    testTag = "registration-comment",
                )
            }

            (submission as? LoadState.Failed)?.let {
                Text(
                    text = it.error.message ?: "등록에 실패했어요.",
                    style = PickflowTypography.labelMedium,
                    color = PickflowColors.spotOrange,
                )
            }
        }
    }
}

@Composable
private fun RegistrationHeader(
    isRegisterEnabled: Boolean,
    isSubmitting: Boolean,
    onBack: () -> Unit,
    onSubmit: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(PickflowColors.gray95)
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp, bottom = 12.dp)
            .height(44.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clickable(onClick = onBack)
                .testTag("registration-back"),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "뒤로가기",
                tint = PickflowColors.gray0,
            )
        }
        Text(
            text = "스팟 등록",
            style = PickflowTypography.headingMedium,
            color = PickflowColors.gray0,
            modifier = Modifier.weight(1f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Box(
            modifier = Modifier
                .size(44.dp)
                .clickable(enabled = isRegisterEnabled && !isSubmitting, onClick = onSubmit)
                .testTag("registration-submit"),
            contentAlignment = Alignment.Center,
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(color = PickflowColors.gray0, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
            } else {
                Text(
                    text = "등록",
                    style = PickflowTypography.headingSmall,
                    color = if (isRegisterEnabled) PickflowColors.spotOrange else PickflowColors.spotDisabled,
                )
            }
        }
    }
}

@Composable
private fun PhotoPickerCard(previewUri: String?, hasImage: Boolean, onPick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(PickflowColors.spotPhotoCardBackground)
            .clickable(onClick = onPick)
            .testTag("registration-photo-card"),
        contentAlignment = Alignment.Center,
    ) {
        if (previewUri != null && hasImage) {
            AsyncImage(
                model = Uri.parse(previewUri),
                contentDescription = "선택한 스팟 사진",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Image(
                    painter = androidx.compose.ui.res.painterResource(R.drawable.ic_photo),
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                )
                Text(
                    text = "스팟의 분위기가\n잘 담긴 사진을 올려주세요.",
                    style = PickflowTypography.bodyMediumBold,
                    color = PickflowColors.spotPlaceholderText,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun SpotAddressCard(title: String, address: String, distanceText: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(PickflowColors.spotPhotoCardBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = PickflowTypography.bodyLargeBold, color = PickflowColors.gray0)
            Text(address, style = PickflowTypography.bodySmall, color = PickflowColors.spotTertiaryText)
        }
        if (distanceText.isNotBlank()) {
            Text(
                text = distanceText,
                style = PickflowTypography.labelMedium,
                color = PickflowColors.gray10,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(PickflowColors.spotPillBackground)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun SpotSearchLocationButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(PickflowColors.spotOrange)
            .clickable(onClick = onClick)
            .testTag("registration-search-location"),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Place, contentDescription = null, tint = PickflowColors.gray0, modifier = Modifier.size(24.dp))
        Text("어디에서 찍으셨나요?", style = PickflowTypography.bodyLargeBold, color = PickflowColors.gray0)
    }
}

@Composable
private fun LabeledSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = PickflowTypography.labelMedium, color = PickflowColors.gray30)
        content()
    }
}

/**
 * 사진 카테고리 칩 — **단독 선택**(무드 필터의 다중선택과 다르다).
 *
 * stateless — Paparazzi 스냅샷이 직접 렌더한다(`SpotRegistrationThemeChipSnapshotTest`).
 * 등록 폼은 로그인이 필요해 비회원 상태의 에뮬레이터로는 진입할 수 없기 때문이다.
 */
@Composable
internal fun ThemeChipGroup(selected: SpotTheme?, onToggle: (SpotTheme) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        SpotTheme.entries.forEach { t ->
            val isSelected = selected == t
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(PickflowColors.spotInputBackground)
                    .border(
                        width = if (isSelected) 1.5.dp else 1.dp,
                        color = if (isSelected) PickflowColors.spotOrange else PickflowColors.spotChipBorder,
                        shape = RoundedCornerShape(8.dp),
                    )
                    .clickable { onToggle(t) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Image(
                    painter = androidx.compose.ui.res.painterResource(t.iconRes()),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = t.label(),
                    style = PickflowTypography.bodyMediumBold,
                    color = if (isSelected) PickflowColors.gray0 else PickflowColors.spotSecondaryText,
                )
            }
        }
    }
}

@Composable
private fun SelectionField(
    value: String?,
    placeholder: String,
    modifier: Modifier = Modifier,
    testTag: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (value == null) PickflowColors.spotInputBackground else PickflowColors.spotPhotoCardBackground)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = value ?: placeholder,
            style = PickflowTypography.bodyMediumBold,
            color = if (value == null) PickflowColors.spotSecondaryText else PickflowColors.gray0,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (value != null) {
            Text("수정", style = PickflowTypography.labelMedium, color = PickflowColors.spotOrange)
        }
    }
}

@Composable
private fun CountedInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    count: Int,
    maxCount: Int,
    singleLine: Boolean,
    testTag: String,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(PickflowColors.spotInputBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    style = PickflowTypography.bodyMediumBold,
                    color = PickflowColors.spotSecondaryText,
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = singleLine,
                textStyle = TextStyle(
                    color = PickflowColors.gray0,
                    fontSize = PickflowTypography.bodyMediumBold.fontSize,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                ),
                cursorBrush = SolidColor(PickflowColors.spotOrange),
                modifier = Modifier
                    .fillMaxWidth()
                    .let { if (!singleLine) it.heightIn(min = 64.dp) else it }
                    .testTag(testTag),
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Text("$count", style = PickflowTypography.labelMedium, color = PickflowColors.gray0)
            Text("/$maxCount", style = PickflowTypography.labelMedium, color = PickflowColors.spotSecondaryText)
        }
    }
}

private fun SpotTheme.iconRes(): Int = when (this) {
    SpotTheme.SUNLIGHT -> R.drawable.ic_sunny
    SpotTheme.YUNSEUL -> R.drawable.ic_reflection
    SpotTheme.SUNSET -> R.drawable.ic_sunset
    SpotTheme.NIGHT -> R.drawable.ic_night
}
