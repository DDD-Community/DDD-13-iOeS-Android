package com.pickflow.android.feature.map

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pickflow.android.R
import com.pickflow.android.common.designsystem.PickflowColors
import com.pickflow.android.common.designsystem.PickflowTypography
import com.pickflow.android.common.ui.LoadState
import com.pickflow.android.core.services.protocols.Cluster

@Composable
fun HomeMapScreen(
    onOpenSpotDetail: (String) -> Unit,
    onOpenRegistration: () -> Unit,
    onRequireLogin: () -> Unit = {},
    viewModel: HomeMapViewModel = hiltViewModel(),
) {
    val clusters by viewModel.clusters.collectAsStateWithLifecycle()
    val selectedMood by viewModel.selectedMood.collectAsStateWithLifecycle()
    val mapListMode by viewModel.mapListMode.collectAsStateWithLifecycle()
    val selectedCluster by viewModel.selectedCluster.collectAsStateWithLifecycle()
    val cameraTarget by viewModel.cameraTarget.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.load() }

    val context = LocalContext.current
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        if (grants.values.any { it }) viewModel.moveToCurrentLocation()
    }
    val onMyLocationClick = {
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_COARSE_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED
        if (granted) viewModel.moveToCurrentLocation()
        else locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            ),
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PickflowColors.themeReflection)
            .testTag("homemap-screen"),
    ) {
        when (mapListMode) {
            MapListMode.MAP -> {
                val items = (clusters as? LoadState.Loaded)?.value.orEmpty()
                NaverMapView(
                    clusters = items,
                    onClusterTap = viewModel::selectCluster,
                    modifier = Modifier.fillMaxSize(),
                    onViewportChanged = viewModel::onViewportChanged,
                    cameraTarget = cameraTarget,
                    onCameraTargetConsumed = viewModel::consumeCameraTarget,
                )
            }
            MapListMode.LIST -> com.pickflow.android.feature.spotlist.SpotListScreen(
                onOpenSpotDetail = onOpenSpotDetail,
                onRequireLogin = onRequireLogin,
            )
        }

        // MAP 모드에서만 지도 위에 헤더를 그린다. LIST 모드의 헤더(PICKFLOW + 정렬 + 무드)는
        // SpotListScreen 이 자체적으로 갖고 있다.
        if (mapListMode == MapListMode.MAP) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(top = 12.dp),
            ) {
                Image(
                    painter = painterResource(R.drawable.logo),
                    contentDescription = "PICKFLOW",
                    modifier = Modifier
                        .padding(start = 20.dp)
                        .height(24.dp),
                )
                Spacer(Modifier.height(8.dp))
                MoodFilterRow(selected = selectedMood, onSelect = viewModel::selectMood)
            }
        }

        MapListToggle(
            selectedMode = mapListMode,
            onSelect = viewModel::selectMapListMode,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
                .testTag("homemap-toggle"),
        )

        // iOS HomeMapView.trailingControls 1:1 — 추가 버튼(위) + 현재 위치 버튼(아래).
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MapCircleButton(
                iconRes = R.drawable.ic_add_location,
                contentDescription = "스팟 등록",
                onClick = onOpenRegistration,
                modifier = Modifier.testTag("homemap-register"),
            )
            if (mapListMode == MapListMode.MAP) {
                MapCircleButton(
                    iconRes = R.drawable.ic_my_location,
                    contentDescription = "현재 위치",
                    onClick = onMyLocationClick,
                    modifier = Modifier.testTag("homemap-mylocation"),
                )
            }
        }
    }

    // 핀 탭 → 바텀시트.
    selectedCluster?.let { cluster ->
        val spot = cluster.spotIds.firstOrNull()?.let(viewModel::spotById)
        if (spot != null) {
            SpotDetailBottomSheet(
                spot = spot.toSpotDetailData(),
                spotId = spot.id,
                onDismiss = viewModel::dismissCluster,
                onOpenFullDetail = { id ->
                    viewModel.dismissCluster()
                    onOpenSpotDetail(id)
                },
            )
        }
    }
}

/** iOS `HomeMapView.topBar`의 무드 캡슐 행 — 노을/윤슬 2개. */
@Composable
private fun MoodFilterRow(selected: MoodFilter?, onSelect: (MoodFilter) -> Unit) {
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .testTag("homemap-moodfilter"),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MoodFilter.entries.forEach { mood ->
            MoodCapsule(
                mood = mood,
                selected = selected == mood,
                onClick = { onSelect(mood) },
            )
        }
    }
}

/** iOS `moodCapsuleButton` 1:1 — 이모지+라벨, gray95, corner8, 선택 시 sunsetOrange 보더. */
@Composable
private fun MoodCapsule(mood: MoodFilter, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(PickflowColors.gray95)
            .then(
                if (selected) {
                    Modifier.border(1.dp, PickflowColors.sunsetOrange, RoundedCornerShape(8.dp))
                } else {
                    Modifier
                },
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Image(
            painter = painterResource(mood.iconRes),
            contentDescription = mood.displayName,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = mood.displayName,
            style = PickflowTypography.bodyMediumBold,
            color = if (selected) PickflowColors.gray0 else PickflowColors.gray30,
        )
    }
}

/**
 * iOS `MapListToggle` 1:1 — gray95 캡슐 안에서 지도/리스트를 전환하는 세그먼트 컨트롤.
 * 선택된 세그먼트는 gray5 하얀 캡슐로 스프링 애니메이션하며 슬라이드한다.
 */
@Composable
private fun MapListToggle(
    selectedMode: MapListMode,
    onSelect: (MapListMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val modes = MapListMode.entries
    val selectedIndex = modes.indexOf(selectedMode)
    BoxWithConstraints(
        modifier = modifier
            .width(172.dp)
            .height(49.dp)
            .clip(CircleShape)
            .background(PickflowColors.gray95)
            .border(1.dp, PickflowColors.gray80, CircleShape)
            .padding(6.dp),
    ) {
        val segmentWidth = maxWidth / modes.size
        val indicatorOffset by animateDpAsState(
            targetValue = segmentWidth * selectedIndex,
            animationSpec = spring(
                dampingRatio = 0.86f,
                stiffness = Spring.StiffnessMediumLow,
            ),
            label = "mapListIndicator",
        )

        // 선택 인디케이터 — 하얀(gray5) 캡슐.
        Box(
            modifier = Modifier
                .offset(x = indicatorOffset)
                .width(segmentWidth)
                .fillMaxHeight()
                .clip(CircleShape)
                .background(PickflowColors.gray5),
        )

        Row(modifier = Modifier.fillMaxSize()) {
            modes.forEach { mode ->
                val selected = mode == selectedMode
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { onSelect(mode) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = mode.displayName,
                        style = PickflowTypography.bodyMediumBold,
                        color = if (selected) PickflowColors.gray95 else PickflowColors.gray40,
                    )
                }
            }
        }
    }
}

/** iOS `HomeMapView` 의 56dp 원형 컨트롤 버튼 — gray95 배경 + 중앙 아이콘. */
@Composable
private fun MapCircleButton(
    iconRes: Int,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(PickflowColors.gray95)
            .border(1.dp, PickflowColors.gray80, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
        )
    }
}

@Composable
private fun ClusterList(
    clusters: LoadState<List<Cluster>>,
    onOpenSpotDetail: (String) -> Unit,
) {
    val items = (clusters as? LoadState.Loaded)?.value.orEmpty()
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 96.dp, bottom = 80.dp)
            .testTag("homemap-list"),
    ) {
        items(items) { cluster ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(PickflowColors.gray90)
                    .clickable {
                        cluster.spotIds.firstOrNull()?.let(onOpenSpotDetail)
                    }
                    .padding(16.dp),
            ) {
                Text(
                    text = "스팟 ${cluster.count}곳 · ${"%.4f".format(cluster.latitude)}",
                    style = PickflowTypography.bodyMedium,
                    color = PickflowColors.gray10,
                )
            }
        }
    }
}
