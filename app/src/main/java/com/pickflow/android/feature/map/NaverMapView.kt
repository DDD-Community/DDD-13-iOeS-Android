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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
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
import com.naver.maps.map.CameraAnimation
import com.naver.maps.map.CameraPosition
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.MapView
import com.naver.maps.map.NaverMap
import com.naver.maps.map.overlay.Marker
import com.naver.maps.map.overlay.Overlay
import com.pickflow.android.common.designsystem.PickflowColors
import com.pickflow.android.common.designsystem.PickflowTypography
import com.pickflow.android.core.services.protocols.Cluster
import com.pickflow.android.core.services.protocols.Coordinates
import com.pickflow.android.core.services.protocols.ViewportBox

private val INITIAL_CAMERA = LatLng(37.538, 127.038)
private const val INITIAL_ZOOM = 12.5

@Composable
fun NaverMapView(
    clusters: List<Cluster>,
    onClusterTap: (Cluster) -> Unit,
    modifier: Modifier = Modifier,
    onViewportChanged: ((ViewportBox, Int) -> Unit)? = null,
    cameraTarget: Coordinates? = null,
    onCameraTargetConsumed: () -> Unit = {},
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val onViewport = rememberUpdatedState(onViewportChanged)
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
                    map.addOnCameraIdleListener {
                        val cb = onViewport.value ?: return@addOnCameraIdleListener
                        val box = map.toViewportBox() ?: return@addOnCameraIdleListener
                        cb(box, map.cameraPosition.zoom.toInt())
                    }
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

    LaunchedEffect(cameraTarget) {
        val target = cameraTarget ?: return@LaunchedEffect
        naverMap?.moveCamera(
            CameraUpdate.toCameraPosition(
                CameraPosition(LatLng(target.latitude, target.longitude), 15.0),
            ).animate(CameraAnimation.Easing),
        )
        onCameraTargetConsumed()
    }
}

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

private fun NaverMap.toViewportBox(): ViewportBox? {
    val bounds = runCatching { contentBounds }.getOrNull() ?: return null
    val ne = bounds.northEast
    val sw = bounds.southWest
    val topLeft = Coordinates(ne.latitude, sw.longitude)
    val topRight = Coordinates(ne.latitude, ne.longitude)
    val bottomLeft = Coordinates(sw.latitude, sw.longitude)
    val bottomRight = Coordinates(sw.latitude, ne.longitude)
    return ViewportBox(topLeft, topRight, bottomLeft, bottomRight)
}

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
            anchor = PointF(0.5f, 0.5f)
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
