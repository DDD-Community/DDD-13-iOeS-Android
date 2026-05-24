package com.pickflow.android.feature.spotdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Share
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pickflow.android.common.designsystem.PickflowColors
import com.pickflow.android.common.designsystem.PickflowTypography
import com.pickflow.android.common.ui.LoadStateContent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpotDetailScreen(
    spotId: String,
    onBack: () -> Unit,
    viewModel: SpotDetailViewModel = hiltViewModel(),
    actionsViewModel: SpotDetailActionsViewModel = hiltViewModel(),
) {
    val spot by viewModel.spot.collectAsStateWithLifecycle()
    val bookmarked by viewModel.bookmarked.collectAsStateWithLifecycle()
    val reportSubmitted by viewModel.reportSubmitted.collectAsStateWithLifecycle()

    LaunchedEffect(spotId) { viewModel.load(spotId) }

    Scaffold(
        containerColor = PickflowColors.gray95,
        topBar = {
            TopAppBar(
                title = { Text("스팟 상세", style = PickflowTypography.bodyLargeBold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("detail-back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::share) {
                        Icon(Icons.Filled.Share, contentDescription = "공유")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PickflowColors.gray95,
                    titleContentColor = PickflowColors.gray0,
                    navigationIconContentColor = PickflowColors.gray0,
                    actionIconContentColor = PickflowColors.gray0,
                ),
            )
        },
    ) { padding ->
        Box(modifier = Modifier.padding(padding).testTag("spotdetail-screen")) {
            LoadStateContent(
                state = spot,
                emptyMessage = "스팟 정보를 찾을 수 없어요.",
                onRetry = { viewModel.load(spotId) },
            ) { value ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(PickflowColors.gray80),
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = value.name,
                        style = PickflowTypography.headingLarge,
                        color = PickflowColors.gray0,
                    )
                    Text(
                        text = value.address.ifBlank { "주소 정보 없음" },
                        style = PickflowTypography.bodySmall,
                        color = PickflowColors.gray40,
                    )
                    Spacer(Modifier.height(20.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(
                            onClick = viewModel::toggleBookmark,
                            modifier = Modifier.weight(1f).testTag("detail-bookmark"),
                        ) {
                            Icon(
                                imageVector = if (bookmarked) Icons.Filled.Favorite
                                else Icons.Filled.FavoriteBorder,
                                contentDescription = "북마크",
                                tint = if (bookmarked) PickflowColors.sunsetOrange
                                else PickflowColors.gray40,
                            )
                            Text(
                                text = if (bookmarked) "저장됨" else "저장",
                                modifier = Modifier.padding(start = 6.dp),
                                color = PickflowColors.gray10,
                            )
                        }
                        OutlinedButton(
                            onClick = viewModel::share,
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Filled.Share, contentDescription = "공유")
                            Text("공유", modifier = Modifier.padding(start = 6.dp),
                                color = PickflowColors.gray10)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { actionsViewModel.openInMap(value) },
                        modifier = Modifier.fillMaxWidth().testTag("detail-route"),
                    ) {
                        Icon(Icons.Filled.Place, contentDescription = "경로")
                        Text("경로 보기", modifier = Modifier.padding(start = 6.dp),
                            color = PickflowColors.gray10)
                    }

                    Spacer(Modifier.height(24.dp))
                    WeatherSection()

                    Spacer(Modifier.height(16.dp))
                    TextButton(
                        onClick = viewModel::reportInvalidInfo,
                        modifier = Modifier.testTag("detail-report"),
                    ) {
                        Text(
                            text = if (reportSubmitted) "신고가 접수되었어요" else "잘못된 정보 신고",
                            style = PickflowTypography.labelMedium,
                            color = PickflowColors.gray40,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WeatherSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(PickflowColors.gray90)
            .padding(16.dp)
            .testTag("detail-weather"),
    ) {
        Text(
            text = "실시간 날씨",
            style = PickflowTypography.bodyMediumBold,
            color = PickflowColors.gray0,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "맑음 · 21°C · 미세먼지 보통",
            style = PickflowTypography.bodySmall,
            color = PickflowColors.gray30,
        )
    }
}
