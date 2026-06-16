package com.pickflow.android.feature.spotsearch

import android.util.Log
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.CameraAnimation
import com.naver.maps.map.CameraPosition
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.MapView
import com.naver.maps.map.NaverMap
import com.pickflow.android.R
import com.pickflow.android.common.designsystem.PickflowColors
import com.pickflow.android.common.designsystem.PickflowTypography
import com.pickflow.android.core.services.protocols.AddressSuggestion
import com.pickflow.android.core.services.protocols.Coordinates

/**
 * iOS `SpotLocationDetailView` 1:1 — 지도 + 중앙 핀으로 정확한 위치 조정 후 "이 장소가 맞아요".
 * 뒤로가기는 검색으로, 확정 시 [onConfirm] → 등록 화면으로 path 복귀.
 */
@Composable
fun SpotLocationDetailScreen(
    candidate: AddressSuggestion,
    onBack: () -> Unit,
    onConfirm: (AddressSuggestion) -> Unit,
    viewModel: SpotLocationDetailViewModel = hiltViewModel(),
) {
    val isConfirming by viewModel.isConfirming.collectAsStateWithLifecycle()
    var markerCoordinate by remember {
        mutableStateOf(Coordinates(candidate.latitude, candidate.longitude))
    }
    var moveToUser by remember { mutableStateOf<Coordinates?>(null) }

    LaunchedEffect(Unit) { viewModel.refreshUserLocation() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PickflowColors.gray95)
            .testTag("location-detail-screen"),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp)
                .height(48.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clickable(onClick = onBack)
                    .testTag("location-detail-back"),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로가기", tint = PickflowColors.gray0)
            }
            Text(
                text = "장소 상세 정보",
                style = PickflowTypography.headingMedium,
                color = PickflowColors.gray0,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.size(44.dp))
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .padding(top = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp)
                    .clip(RoundedCornerShape(12.dp)),
            ) {
                LocationPickerMap(
                    initial = Coordinates(candidate.latitude, candidate.longitude),
                    moveTo = moveToUser,
                    onMoved = { moveToUser = null },
                    onCenterChanged = { markerCoordinate = it },
                    modifier = Modifier.fillMaxSize(),
                )
                // 중앙 고정 핀(팁이 정중앙을 가리키도록 위로 offset).
                Icon(
                    imageVector = Icons.Filled.Place,
                    contentDescription = null,
                    tint = PickflowColors.spotOrange,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(40.dp)
                        .offset(y = (-20).dp),
                )
                // 현재 위치 버튼
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp)
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(PickflowColors.gray95)
                        .border(1.dp, PickflowColors.gray80, CircleShape)
                        .clickable { viewModel.moveToCurrentLocation { coords -> moveToUser = coords } }
                        .testTag("location-detail-mylocation"),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_my_location),
                        contentDescription = "현재 위치",
                        modifier = Modifier.size(24.dp),
                    )
                }
            }

            Text(
                text = "핀을 움직여 정확한 위치로 이동해 주세요",
                style = PickflowTypography.bodyMediumBold,
                color = PickflowColors.gray30,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(PickflowColors.spotOrange)
                    .clickable(enabled = !isConfirming) {
                        viewModel.confirm(
                            markerLatitude = markerCoordinate.latitude,
                            markerLongitude = markerCoordinate.longitude,
                            candidate = candidate,
                            onConfirmed = onConfirm,
                        )
                    }
                    .testTag("location-detail-confirm"),
                contentAlignment = Alignment.Center,
            ) {
                if (isConfirming) {
                    CircularProgressIndicator(color = PickflowColors.gray0, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
                } else {
                    Text("이 장소가 맞아요", style = PickflowTypography.bodyLargeBold, color = PickflowColors.gray0)
                }
            }
        }
    }
}

@Composable
private fun LocationPickerMap(
    initial: Coordinates,
    moveTo: Coordinates?,
    onMoved: () -> Unit,
    onCenterChanged: (Coordinates) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapView = remember {
        runCatching { MapView(context) }
            .onFailure { Log.e("LocationPickerMap", "MapView 생성 실패", it) }
            .getOrNull()
    }

    if (mapView == null) {
        Box(modifier.background(PickflowColors.gray90))
        return
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            runCatching {
                when (event) {
                    Lifecycle.Event.ON_CREATE -> mapView.onCreate(null)
                    Lifecycle.Event.ON_START -> mapView.onStart()
                    Lifecycle.Event.ON_RESUME -> mapView.onResume()
                    Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                    Lifecycle.Event.ON_STOP -> mapView.onStop()
                    Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                    else -> Unit
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            runCatching { mapView.onDestroy() }
        }
    }

    var naverMap by remember { mutableStateOf<NaverMap?>(null) }

    AndroidView(
        factory = {
            runCatching {
                mapView.getMapAsync { map ->
                    map.isNightModeEnabled = true
                    map.uiSettings.apply {
                        isZoomControlEnabled = false
                        isCompassEnabled = false
                        isScaleBarEnabled = false
                        isLocationButtonEnabled = false
                    }
                    map.moveCamera(
                        CameraUpdate.toCameraPosition(
                            CameraPosition(LatLng(initial.latitude, initial.longitude), 16.0),
                        ),
                    )
                    map.addOnCameraIdleListener {
                        val target = map.cameraPosition.target
                        onCenterChanged(Coordinates(target.latitude, target.longitude))
                    }
                    naverMap = map
                }
            }.onFailure { Log.e("LocationPickerMap", "getMapAsync 실패", it) }
            mapView
        },
        modifier = modifier.testTag("location-detail-map"),
    )

    LaunchedEffect(moveTo) {
        val target = moveTo ?: return@LaunchedEffect
        naverMap?.moveCamera(
            CameraUpdate.toCameraPosition(
                CameraPosition(LatLng(target.latitude, target.longitude), 16.0),
            ).animate(CameraAnimation.Easing),
        )
        onMoved()
    }
}
