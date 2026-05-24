package com.pickflow.android.feature.spotregistration

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pickflow.android.common.designsystem.PickflowColors
import com.pickflow.android.common.designsystem.PickflowTypography
import com.pickflow.android.common.ui.LoadState
import com.pickflow.android.core.services.protocols.SpotTheme
import com.pickflow.android.feature.spotlist.label
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SpotRegistrationScreen(
    onBack: () -> Unit,
    onRegistered: (String) -> Unit,
    viewModel: SpotRegistrationViewModel = hiltViewModel(),
) {
    val name by viewModel.name.collectAsStateWithLifecycle()
    val theme by viewModel.theme.collectAsStateWithLifecycle()
    val address by viewModel.address.collectAsStateWithLifecycle()
    val capturedDate by viewModel.capturedDate.collectAsStateWithLifecycle()
    val capturedTime by viewModel.capturedTime.collectAsStateWithLifecycle()
    val comment by viewModel.comment.collectAsStateWithLifecycle()
    val submission by viewModel.submission.collectAsStateWithLifecycle()

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    LaunchedEffect(submission) {
        (submission as? LoadState.Loaded)?.let { onRegistered(it.value.id) }
    }

    if (showDatePicker) {
        val dateState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dateState.selectedDateMillis?.let {
                        viewModel.setCapturedDate(
                            SimpleDateFormat("yyyy.MM.dd", Locale.KOREA).format(Date(it))
                        )
                    }
                    showDatePicker = false
                }) { Text("확인") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("취소") }
            },
        ) { DatePicker(state = dateState) }
    }

    if (showTimePicker) {
        val timeState = rememberTimePickerState()
        Dialog(onDismissRequest = { showTimePicker = false }) {
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(PickflowColors.gray90)
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                TimePicker(state = timeState)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { showTimePicker = false }) { Text("취소") }
                    TextButton(onClick = {
                        viewModel.setCapturedTime(
                            "%02d:%02d".format(timeState.hour, timeState.minute)
                        )
                        showTimePicker = false
                    }) { Text("확인") }
                }
            }
        }
    }

    Scaffold(
        containerColor = PickflowColors.spotBackground,
        topBar = {
            TopAppBar(
                title = { Text("스팟 등록", style = PickflowTypography.bodyLargeBold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("registration-back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PickflowColors.spotBackground,
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
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
                .testTag("spotregistration-screen"),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(PickflowColors.gray90),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "사진 추가",
                    style = PickflowTypography.bodyMedium,
                    color = PickflowColors.gray40,
                )
            }
            Spacer(Modifier.height(20.dp))

            DarkField(
                value = address,
                onValueChange = viewModel::setAddress,
                label = "주소",
                placeholder = "주소를 검색하세요",
            )
            Spacer(Modifier.height(12.dp))
            DarkField(
                value = name,
                onValueChange = viewModel::setName,
                label = "스팟명",
                placeholder = "스팟 이름 (최대 20자)",
            )
            Spacer(Modifier.height(12.dp))

            PickerRow(
                label = "촬영 날짜",
                value = capturedDate,
                placeholder = "날짜 선택",
                testTag = "registration-date",
                onClick = { showDatePicker = true },
            )
            Spacer(Modifier.height(12.dp))
            PickerRow(
                label = "촬영 시간",
                value = capturedTime,
                placeholder = "시간 선택",
                testTag = "registration-time",
                onClick = { showTimePicker = true },
            )
            Spacer(Modifier.height(16.dp))

            Text("카테고리", style = PickflowTypography.labelMedium, color = PickflowColors.gray30)
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SpotTheme.entries.forEach { t ->
                    FilterChip(
                        selected = theme == t,
                        onClick = { viewModel.setTheme(t) },
                        label = { Text(t.label(), style = PickflowTypography.labelMedium) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = PickflowColors.surfaceChip,
                            labelColor = PickflowColors.gray30,
                            selectedContainerColor = PickflowColors.spotOrange,
                            selectedLabelColor = PickflowColors.gray0,
                        ),
                    )
                }
            }
            Spacer(Modifier.height(16.dp))

            DarkField(
                value = comment,
                onValueChange = viewModel::setComment,
                label = "설명",
                placeholder = "스팟 설명 (최대 50자)",
                singleLine = false,
                testTag = "registration-comment",
            )
            Spacer(Modifier.height(24.dp))

            (submission as? LoadState.Failed)?.let {
                Text(
                    text = it.error.message ?: "등록에 실패했어요.",
                    style = PickflowTypography.labelMedium,
                    color = PickflowColors.spotOrange,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            Button(
                onClick = {
                    if (viewModel.capturedDate.value.isNotBlank() &&
                        viewModel.capturedTime.value.isNotBlank() &&
                        viewModel.name.value.isNotBlank() &&
                        viewModel.address.value.isNotBlank()
                    ) {
                        viewModel.setCoordinates(37.5665, 126.9780)
                    }
                    viewModel.submit()
                },
                enabled = submission !is LoadState.Loading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("registration-submit"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PickflowColors.spotOrange,
                    contentColor = PickflowColors.gray0,
                    disabledContainerColor = PickflowColors.spotDisabled,
                ),
            ) {
                Text("등록하기", style = PickflowTypography.bodyLargeBold)
            }
        }
    }
}

@Composable
private fun PickerRow(
    label: String,
    value: String,
    placeholder: String,
    testTag: String,
    onClick: () -> Unit,
) {
    Column {
        Text(label, style = PickflowTypography.labelMedium, color = PickflowColors.gray30)
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(PickflowColors.gray90)
                .clickable(onClick = onClick)
                .testTag(testTag)
                .padding(horizontal = 16.dp, vertical = 16.dp),
        ) {
            Text(
                text = value.ifBlank { placeholder },
                style = PickflowTypography.bodyMedium,
                color = if (value.isBlank()) PickflowColors.gray50 else PickflowColors.gray0,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DarkField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    singleLine: Boolean = true,
    testTag: String? = null,
) {
    Column {
        Text(label, style = PickflowTypography.labelMedium, color = PickflowColors.gray30)
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .let { if (testTag != null) it.testTag(testTag) else it },
            placeholder = { Text(placeholder) },
            shape = RoundedCornerShape(12.dp),
            singleLine = singleLine,
            minLines = if (singleLine) 1 else 3,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = PickflowColors.gray0,
                unfocusedTextColor = PickflowColors.gray0,
                focusedContainerColor = PickflowColors.gray90,
                unfocusedContainerColor = PickflowColors.gray90,
                focusedBorderColor = PickflowColors.spotOrange,
                unfocusedBorderColor = PickflowColors.gray70,
                focusedPlaceholderColor = PickflowColors.gray50,
                unfocusedPlaceholderColor = PickflowColors.gray50,
            ),
        )
    }
}
