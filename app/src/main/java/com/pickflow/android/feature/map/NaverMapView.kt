package com.pickflow.android.feature.map

import android.content.Context
import android.graphics.PointF
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.CameraPosition
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.MapView
import com.naver.maps.map.NaverMap
import com.naver.maps.map.overlay.Marker
import com.naver.maps.map.overlay.Overlay
import com.pickflow.android.common.designsystem.PickflowColors
import com.pickflow.android.common.designsystem.PickflowTypography
import com.pickflow.android.core.services.protocols.Cluster

// 디버그 fixture(왕십리·영동대교·잠원) 영역이 한 화면에 보이도록 — iOS NaverMapView 와 동일한 카메라/줌.
private val INITIAL_CAMERA = LatLng(37.538, 127.038)
private const val INITIAL_ZOOM = 12.5

/**
 * Naver Map 렌더링. 클러스터를 커스텀 핀 마커로 그리고 탭을 콜백으로 전달한다.
 * MapView 생성/네이티브 초기화가 불가능한 환경(테스트 등)에서는 placeholder로
 * graceful degrade 한다.
 */
@Composable
fun NaverMapView(
    clusters: List<Cluster>,
    onClusterTap: (Cluster) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapView = remember {
        runCatching { MapView(context) }
            .onFailure { Log.e("NaverMapView", "MapView 생성 실패 — placeholder로 대체", it) }
            .getOrNull()
    }

    if (mapView == null) {
        MapUnavailablePlaceholder(modifier)
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
    val markers = remember { mutableListOf<Marker>() }

    AndroidView(
        factory = {
            runCatching {
                mapView.getMapAsync { map ->
                    configureMap(map)
                    naverMap = map
                }
            }.onFailure { Log.e("NaverMapView", "getMapAsync 실패", it) }
            mapView
        },
        modifier = modifier.testTag("naver-map"),
        update = {
            naverMap?.let { map ->
                runCatching {
                    renderMarkers(map, markers, clusters, context, onClusterTap)
                }.onFailure { Log.e("NaverMapView", "마커 렌더 실패", it) }
            }
        },
    )
}

/**
 * 지도 최초 로드 시 1회 설정 — iOS `NaverMapView` 와 동일하게 나이트모드를 켜고
 * 기본 UI 컨트롤(줌·나침반·스케일바·위치 버튼)을 모두 숨긴 뒤,
 * 디버그 fixture 영역으로 초기 카메라를 맞춘다.
 */
private fun configureMap(naverMap: NaverMap) {
    naverMap.isNightModeEnabled = true
    naverMap.uiSettings.apply {
        isZoomControlEnabled = false
        isCompassEnabled = false
        isScaleBarEnabled = false
        isLocationButtonEnabled = false
    }
    naverMap.moveCamera(
        CameraUpdate.toCameraPosition(CameraPosition(INITIAL_CAMERA, INITIAL_ZOOM)),
    )
}

/** 기존 마커를 제거하고 클러스터를 핀 마커로 다시 그린다. */
private fun renderMarkers(
    naverMap: NaverMap,
    markers: MutableList<Marker>,
    clusters: List<Cluster>,
    context: Context,
    onClusterTap: (Cluster) -> Unit,
) {
    markers.forEach { it.map = null }
    markers.clear()

    clusters.forEach { cluster ->
        val marker = Marker().apply {
            position = LatLng(cluster.latitude, cluster.longitude)
            icon = if (cluster.count >= 2) {
                MapMarkerIcons.clusterIcon(context, cluster.count)
            } else {
                MapMarkerIcons.spotIcon(context)
            }
            width = Marker.SIZE_AUTO
            height = Marker.SIZE_AUTO
            anchor = PointF(0.5f, 0.5f) // iOS와 동일하게 원형 핀을 좌표 중앙에 정렬
            onClickListener = Overlay.OnClickListener {
                onClusterTap(cluster)
                true
            }
            map = naverMap
        }
        markers.add(marker)
    }
}

@Composable
private fun MapUnavailablePlaceholder(modifier: Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PickflowColors.themeReflection)
            .testTag("naver-map-placeholder"),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "지도를 불러올 수 없어요",
            style = PickflowTypography.bodyMedium,
            color = PickflowColors.gray30,
            textAlign = TextAlign.Center,
        )
    }
}
