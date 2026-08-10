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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.semantics.clearAndSetSemantics
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
import com.pickflow.android.core.services.protocols.AddressSuggestion
import com.pickflow.android.core.services.protocols.ImagePayload
import com.pickflow.android.core.services.protocols.MySpotDetail
import com.pickflow.android.core.services.protocols.SpotTheme
import com.pickflow.android.feature.spotlist.label
import com.pickflow.android.feature.spotregistration.components.CaptureDatePickerSheet
import com.pickflow.android.feature.spotregistration.components.CaptureTimePickerSheet
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DATE_DISPLAY = DateTimeFormatter.ofPattern("M월 d일", Locale.KOREAN)
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
    val mode by viewModel.mode.collectAsStateWithLifecycle()
    val revisionLoadState by viewModel.revisionLoadState.collectAsStateWithLifecycle()
    val existingImageUrl by viewModel.existingImageUrl.collectAsStateWithLifecycle()
    val submission by viewModel.submission.collectAsStateWithLifecycle()
    val isRegisterEnabled by viewModel.isRegisterEnabled.collectAsStateWithLifecycle()

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showResubmitSheet by remember { mutableStateOf(false) }

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

    SpotRegistrationContent(
        mode = mode,
        revisionLoadState = revisionLoadState,
        selectedAddress = selectedAddress,
        distanceText = distanceText,
        spotName = spotName,
        theme = theme,
        capturedDate = capturedDate,
        capturedTime = capturedTime,
        comment = comment,
        selectedImageUri = selectedImageUri,
        hasReplacementImage = imagePayload != null,
        existingImageUrl = existingImageUrl,
        submission = submission,
        isRegisterEnabled = isRegisterEnabled,
        showResubmitSheet = showResubmitSheet,
        onBack = onBack,
        onSubmit = {
            if (mode == SpotRegistrationMode.REVISE) {
                showResubmitSheet = true
            } else {
                viewModel.submit()
            }
        },
        onConfirmResubmit = {
            showResubmitSheet = false
            viewModel.submit()
        },
        onDismissResubmit = { showResubmitSheet = false },
        onPhotoPick = {
            photoLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
            )
        },
        onOpenSearch = onOpenSearch,
        onSpotNameChange = viewModel::setSpotName,
        onThemeToggle = viewModel::toggleTheme,
        onDateClick = { showDatePicker = true },
        onTimeClick = { showTimePicker = true },
        onCommentChange = viewModel::setComment,
    )
}

/** ViewModel/Hilt 의존이 없는 등록·반려 편집 화면 본체. */
@Composable
fun SpotRegistrationContent(
    mode: SpotRegistrationMode,
    revisionLoadState: LoadState<MySpotDetail>,
    selectedAddress: AddressSuggestion?,
    distanceText: String,
    spotName: String,
    theme: SpotTheme?,
    capturedDate: java.time.LocalDate?,
    capturedTime: java.time.LocalTime?,
    comment: String,
    selectedImageUri: String?,
    hasReplacementImage: Boolean,
    existingImageUrl: String?,
    submission: LoadState<SpotRegistrationSubmissionResult>,
    isRegisterEnabled: Boolean,
    showResubmitSheet: Boolean,
    onBack: () -> Unit = {},
    onSubmit: () -> Unit = {},
    onConfirmResubmit: () -> Unit = {},
    onDismissResubmit: () -> Unit = {},
    onPhotoPick: () -> Unit = {},
    onOpenSearch: () -> Unit = {},
    onSpotNameChange: (String) -> Unit = {},
    onThemeToggle: (SpotTheme) -> Unit = {},
    onDateClick: () -> Unit = {},
    onTimeClick: () -> Unit = {},
    onCommentChange: (String) -> Unit = {},
) {
    if (mode == SpotRegistrationMode.REVISE && revisionLoadState !is LoadState.Loaded) {
        RevisionLoadStateContent(revisionLoadState)
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PickflowColors.gray95)
            .testTag("spotregistration-screen"),
    ) {
        RegistrationHeader(
            title = if (mode == SpotRegistrationMode.REVISE) "스팟 수정" else "스팟 등록",
            actionLabel = if (mode == SpotRegistrationMode.REVISE) "다시 신청" else "등록",
            isRegisterEnabled = isRegisterEnabled,
            isSubmitting = submission is LoadState.Loading,
            onBack = onBack,
            onSubmit = onSubmit,
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
                hasImage = hasReplacementImage || existingImageUrl != null,
                isExistingImage = mode == SpotRegistrationMode.REVISE &&
                    !hasReplacementImage && existingImageUrl != null,
                onPick = onPhotoPick,
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
                    onValueChange = onSpotNameChange,
                    placeholder = "이 장소를 무엇이라 부를까요?",
                    count = spotName.length,
                    maxCount = SpotRegistrationViewModel.MAX_NAME_LENGTH,
                    singleLine = true,
                    testTag = "registration-name",
                )
            }

            LabeledSection("사진 카테고리") {
                ThemeChipGroup(selected = theme, onToggle = onThemeToggle)
            }

            LabeledSection("촬영 기록 정보") {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SelectionField(
                        value = capturedDate?.format(DATE_DISPLAY),
                        placeholder = "날짜 선택",
                        modifier = Modifier.weight(1f),
                        testTag = "registration-date",
                        onClick = onDateClick,
                    )
                    SelectionField(
                        value = capturedTime?.format(TIME_DISPLAY),
                        placeholder = "시간 선택",
                        modifier = Modifier.weight(1f),
                        testTag = "registration-time",
                        onClick = onTimeClick,
                    )
                }
            }

            LabeledSection("한 줄 코멘트") {
                CountedInput(
                    value = comment,
                    onValueChange = onCommentChange,
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

    if (showResubmitSheet) {
        RegistrationResubmitSheet(
            onConfirm = onConfirmResubmit,
            onDismiss = onDismissResubmit,
        )
    }
}

@Composable
private fun RevisionLoadStateContent(state: LoadState<MySpotDetail>) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PickflowColors.gray95)
            .testTag(
                if (state is LoadState.Failed) {
                    "registration-revision-error"
                } else {
                    "registration-revision-loading"
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (state is LoadState.Failed) {
            Text(
                text = "편집 정보를 불러오지 못했어요.",
                style = PickflowTypography.bodyMedium,
                color = PickflowColors.gray30,
            )
        } else {
            CircularProgressIndicator(color = PickflowColors.sunsetOrange)
        }
    }
}

@Composable
private fun RegistrationHeader(
    title: String,
    actionLabel: String,
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
                .width(72.dp)
                .height(44.dp)
                .clickable(onClick = onBack)
                .testTag("registration-back"),
            contentAlignment = Alignment.CenterStart,
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "뒤로가기",
                tint = PickflowColors.gray0,
            )
        }
        Text(
            text = title,
            style = PickflowTypography.headingMedium,
            color = PickflowColors.gray0,
            modifier = Modifier.weight(1f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Box(
            modifier = Modifier
                .width(72.dp)
                .height(44.dp)
                .clickable(enabled = isRegisterEnabled && !isSubmitting, onClick = onSubmit)
                .testTag("registration-submit"),
            contentAlignment = Alignment.CenterEnd,
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(color = PickflowColors.gray0, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
            } else {
                Text(
                    text = actionLabel,
                    style = PickflowTypography.headingSmall,
                    color = if (isRegisterEnabled) PickflowColors.spotOrange else PickflowColors.spotDisabled,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun PhotoPickerCard(
    previewUri: String?,
    hasImage: Boolean,
    isExistingImage: Boolean,
    onPick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(PickflowColors.spotPhotoCardBackground)
            .clickable(onClick = onPick)
            .testTag(if (isExistingImage) "registration-existing-image" else "registration-photo-card"),
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
@OptIn(ExperimentalMaterial3Api::class)
private fun RegistrationResubmitSheet(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = PickflowColors.gray95,
        dragHandle = null,
    ) {
        RegistrationResubmitSheetContent(
            onConfirm = onConfirm,
            onDismiss = onDismiss,
        )
    }
}

@Composable
fun RegistrationResubmitSheetContent(
    onConfirm: () -> Unit = {},
    onDismiss: () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 24.dp, bottom = 32.dp)
            .testTag("registration-resubmit-sheet"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "다시 신청할까요?",
            style = PickflowTypography.headingMedium,
            color = PickflowColors.gray0,
        )
        Text(
            text = "제출하면 검수가 다시 시작돼요.",
            style = PickflowTypography.bodyMedium,
            color = PickflowColors.gray30,
            modifier = Modifier.padding(bottom = 12.dp),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(PickflowColors.sunsetOrange)
                .clickable(onClick = onConfirm)
                .testTag("registration-resubmit-confirm"),
            contentAlignment = Alignment.Center,
        ) {
            Text("신청하기", style = PickflowTypography.bodyLargeBold, color = PickflowColors.gray0)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(PickflowColors.gray80)
                .clickable(onClick = onDismiss)
                .testTag("registration-resubmit-cancel"),
            contentAlignment = Alignment.Center,
        ) {
            Text("계속 수정할게요", style = PickflowTypography.bodyLargeBold, color = PickflowColors.gray0)
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
            Text(
                text = address,
                style = PickflowTypography.bodySmall,
                color = PickflowColors.spotTertiaryText,
                modifier = Modifier.testTag("registration-address"),
            )
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
 * 사진 카테고리 칩 — Figma `Btn-tag` 컴포넌트 세트(`1:46095`) 1:1.
 *
 * **단독 선택**이다(탐색 탭 무드 필터의 다중선택과 다르다). 그 외 시각 사양은
 * 무드 캡슐과 같은 규칙을 따른다 — 선택 여부는 **보더 유무로만** 구분하고 라벨색은 고정이다.
 *
 * 사양(Figma `1:44480` on 상태):
 * - 칩: 패딩 12×8, 코너 8, 배경 `gray90`(#1E2124), 아이콘–라벨 간격 6
 *   — 가로 패딩은 고정이 아니라 남는 공간이다. 4개를 `weight` 로 균등 분배하고 내용을
 *   가운데 정렬해, 390dp 에서 Figma 와 같은 12dp 가 나오고 좁은 기기에선 함께 줄어든다.
 * - 아이콘 20dp, 라벨 `bodyMediumBold`(15sp), 라벨색 `gray0` 고정
 * - 선택 시에만 `sunsetOrange` 1dp 보더 (미선택은 보더 없음)
 * - 칩 간격 12
 *
 * stateless — Paparazzi 스냅샷이 직접 렌더한다(`SpotRegistrationThemeChipSnapshotTest`).
 * 등록 폼은 로그인이 필요해 비회원 상태의 에뮬레이터로는 진입할 수 없기 때문이다.
 */
@Composable
internal fun ThemeChipGroup(selected: SpotTheme?, onToggle: (SpotTheme) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SpotTheme.entries.forEach { t ->
            val isSelected = selected == t
            Row(
                modifier = Modifier
                    // fill = false 여야 widthIn(max) 이 먹는다 — 기본값(true)은 4등분한 폭을
                    // 고정 제약으로 내려보내 상한을 무력화한다(MoodFilterRow 와 동일 이슈).
                    .weight(1f, fill = false)
                    .widthIn(max = THEME_CHIP_MAX_WIDTH)
                    // fillMaxWidth 가 있어야 칩이 배분받은 폭을 꽉 채운다. 없으면 콘텐츠 폭에
                    // 붙어버려 라벨 글자폭 차이만큼 칩마다 폭이 달라지고 좌우 여백이 사라진다.
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(PickflowColors.gray90)
                    .then(
                        if (isSelected) {
                            Modifier.border(1.dp, PickflowColors.sunsetOrange, RoundedCornerShape(8.dp))
                        } else {
                            Modifier
                        },
                    )
                    .selectable(
                        selected = isSelected,
                        onClick = { onToggle(t) },
                    )
                    .padding(vertical = 8.dp)
                    .testTag("registration-theme-${t.name.lowercase()}"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
            ) {
                Image(
                    painter = androidx.compose.ui.res.painterResource(t.iconRes()),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = t.label(),
                    style = PickflowTypography.bodyMediumBold,
                    // 폭이 모자라도 '야경' 이 두 줄로 쪼개지지 않게 한 줄 고정.
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                    softWrap = false,
                    color = PickflowColors.gray0,
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
            Text(
                text = "수정",
                style = PickflowTypography.labelMedium,
                color = PickflowColors.spotOrange,
                modifier = Modifier.clearAndSetSemantics {},
            )
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
    SpotTheme.NIGHT_VIEW -> R.drawable.ic_night
}

/** 사진 카테고리 칩 폭 상한 — Figma 콘텐츠 폭(패딩 12 + 아이콘 20 + 간격 6 + 라벨 + 패딩 12). */
private val THEME_CHIP_MAX_WIDTH = 80.dp
