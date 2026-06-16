package com.pickflow.android.feature.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.CameraAnimation
import com.naver.maps.map.CameraPosition
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.MapView
import com.naver.maps.map.NaverMap
import com.naver.maps.map.clustering.Clusterer
import com.naver.maps.map.clustering.ClusterMarkerInfo
import com.naver.maps.map.clustering.DefaultClusterMarkerUpdater
import com.naver.maps.map.clustering.DefaultLeafMarkerUpdater
import com.naver.maps.map.clustering.LeafMarkerInfo
import com.naver.maps.map.overlay.Marker
import com.naver.maps.map.overlay.Overlay
import com.naver.maps.map.overlay.OverlayImage
import kotlinx.coroutines.launch
import com.pickflow.android.common.designsystem.PickflowColors
import com.pickflow.android.common.designsystem.PickflowTypography
import com.pickflow.android.core.services.protocols.Coordinates
import com.pickflow.android.core.services.protocols.Spot
import com.pickflow.android.core.services.protocols.ViewportBox
import com.pickflow.android.feature.map.clustering.MapSpotClusterKey

private val INITIAL_CAMERA = LatLng(37.538, 127.038)
private const val INITIAL_ZOOM = 12.5

/**
 * Naver Maps Android SDK 의 [Clusterer] 를 사용한 지도 뷰.
 *
 * iOS `NaverMapView` 1:1 — 큐레이션 스팟은 SDK 가 화면-픽셀 거리 기반으로 클러스터링,
 * 마이스팟은 클러스터링 외부에서 별도 [Marker] 로 직접 add/remove.
 *
 * zIndex: 큐레이션 leaf = 200, 마이스팟 = 250, 큐레이션 cluster = 300.
 */
@Composable
fun NaverMapView(
    curationSpots: List<Spot>,
    onSpotTap: (Long) -> Unit,
    onClusterTap: (Cluster) -> Unit,
    modifier: Modifier = Modifier,
    onViewportChanged: ((ViewportBox, Int) -> Unit)? = null,
    cameraTarget: Coordinates? = null,
    onCameraTargetConsumed: () -> Unit = {},
    mySpots: List<MySpotMarker> = emptyList(),
    selectedSpotId: Long? = null,
    focusTarget: Coordinates? = null,
    onFocusConsumed: () -> Unit = {},
    bottomInsetFraction: Float = 0f,
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
    val mySpotMarkers = remember { mutableListOf<Marker>() }
    val markerScope = rememberCoroutineScope()
    // imageUrl → 로드된 비트맵 캐시(중복 다운로드 방지).
    val markerBitmapCache = remember { mutableMapOf<String, Bitmap>() }

    // SDK Clusterer + 현재 데이터 스냅샷. 데이터/선택 변경 시 갱신.
    val clustererRef = remember { mutableStateOf<Clusterer<MapSpotClusterKey>?>(null) }
    val onSpotTapState = rememberUpdatedState(onSpotTap)
    val onClusterTapState = rememberUpdatedState(onClusterTap)
    val selectedIdState = rememberUpdatedState(selectedSpotId)

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
                    updateCurationClusterer(
                        map = map,
                        ref = clustererRef,
                        spots = curationSpots,
                        context = context,
                        selectedIdProvider = { selectedIdState.value },
                        onSpotTap = { id -> onSpotTapState.value(id) },
                        onClusterTap = { cluster -> onClusterTapState.value(cluster) },
                        bitmapCache = markerBitmapCache,
                        scope = markerScope,
                    )
                    renderMySpotMarkers(map, mySpotMarkers, mySpots, context, selectedSpotId, onSpotTap)
                }.onFailure { Log.e("NaverMapView", "마커 렌더 실패", it) }
            }
        },
    )

    LaunchedEffect(cameraTarget) {
        val target = cameraTarget ?: return@LaunchedEffect
        val map = naverMap ?: return@LaunchedEffect
        map.moveCamera(
            CameraUpdate.toCameraPosition(
                CameraPosition(LatLng(target.latitude, target.longitude), 15.0),
            ).animate(CameraAnimation.Easing),
        )
        // 내 위치 핀(locationOverlay) 표시 — iOS `updateUserLocation` 1:1.
        val dotPx = (18 * context.resources.displayMetrics.density).toInt()
        map.locationOverlay.apply {
            position = LatLng(target.latitude, target.longitude)
            icon = userLocationDotImage
            iconWidth = dotPx
            iconHeight = dotPx
            circleColor = PickflowColors.sunsetOrange.copy(alpha = 0.25f).toArgb()
            isVisible = true
        }
        onCameraTargetConsumed()
    }

    // 바텀시트가 떠 있는 동안 지도 하단 영역을 패딩으로 확보 → 카메라 센터가 시트 위쪽으로 이동.
    LaunchedEffect(bottomInsetFraction) {
        val map = naverMap ?: return@LaunchedEffect
        val bottomPx = (mapView.height * bottomInsetFraction).toInt().coerceAtLeast(0)
        runCatching { map.setContentPadding(0, 0, 0, bottomPx) }
    }

    // 마커 탭 시 해당 좌표를 (하단 패딩이 적용된) 가시 영역 중앙으로 정렬.
    LaunchedEffect(focusTarget) {
        val target = focusTarget ?: return@LaunchedEffect
        val map = naverMap ?: return@LaunchedEffect
        val bottomPx = (mapView.height * bottomInsetFraction).toInt().coerceAtLeast(0)
        runCatching { map.setContentPadding(0, 0, 0, bottomPx) }
        runCatching {
            map.moveCamera(
                CameraUpdate.scrollTo(LatLng(target.latitude, target.longitude))
                    .animate(CameraAnimation.Easing),
            )
        }
        onFocusConsumed()
    }
}

/**
 * 내 위치 핀 아이콘 — iOS `userLocationDotImage` 1:1.
 * 18pt 기준: 흰 원 + 안쪽 disc(테두리 ~2.5pt) sunsetOrange. 3x 해상도로 렌더.
 */
private val userLocationDotImage: OverlayImage by lazy {
    val size = 54 // 18 * 3x
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val radius = size / 2f
    paint.color = android.graphics.Color.WHITE
    canvas.drawCircle(radius, radius, radius, paint)
    paint.color = PickflowColors.sunsetOrange.toArgb()
    val inset = size * (2.5f / 18f)
    canvas.drawCircle(radius, radius, radius - inset, paint)
    OverlayImage.fromBitmap(bitmap)
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

/**
 * 큐레이션 스팟을 SDK [Clusterer] 에 반영.
 *
 * iOS `updateCurationClusterer` 1:1 — 스팟 ID 해시가 동일하면 clusterer 재생성을 건너뛴다.
 * leaf/cluster 마커 아이콘은 본 프로젝트의 커스텀(검정 그라데이션 / sunsetOrange) 로 교체하고,
 * cluster tap 시 자식 leaf 의 spotId 목록을 모아 [Cluster] 로 [onClusterTap] 콜백한다.
 */
private fun updateCurationClusterer(
    map: NaverMap,
    ref: androidx.compose.runtime.MutableState<Clusterer<MapSpotClusterKey>?>,
    spots: List<Spot>,
    context: Context,
    selectedIdProvider: () -> Long?,
    onSpotTap: (Long) -> Unit,
    onClusterTap: (Cluster) -> Unit,
    bitmapCache: MutableMap<String, Bitmap>,
    scope: kotlinx.coroutines.CoroutineScope,
) {
    // spotId → imageUrl (viewport 응답). leaf 마커에 사진을 적용하기 위함.
    val imageUrlBySpotId: Map<Long, String> = spots.mapNotNull { spot ->
        val id = spot.id.toLongOrNull() ?: return@mapNotNull null
        val url = spot.imageUrl?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
        id to url
    }.toMap()
    // 선택된 spotId 를 hash 에 포함 → 마커 선택/해제 시 clusterer 가 재생성되어
    // leaf/cluster updater 가 sunsetOrange stroke(width 4) 를 다시 그린다.
    val newHash = (spots.joinToString("|") { it.id } + "#sel=${selectedIdProvider()}").hashCode()
    val existing = ref.value
    if (existing != null && existing.tagOf("hash") == newHash) {
        // 데이터 + 선택 상태 모두 동일 — clusterer 재생성 불필요.
        return
    }

    existing?.map = null

    val clusterer: Clusterer<MapSpotClusterKey> = Clusterer.ComplexBuilder<MapSpotClusterKey>()
        .minClusteringZoom(0)
        .maxClusteringZoom(15)
        .leafMarkerUpdater(object : DefaultLeafMarkerUpdater() {
            override fun updateLeafMarker(info: LeafMarkerInfo, marker: Marker) {
                super.updateLeafMarker(info, marker)
                val key = info.key as? MapSpotClusterKey ?: return
                val isSelected = key.spotId == selectedIdProvider()
                marker.tag = key.spotId
                val url = imageUrlBySpotId[key.spotId]
                val cached = url?.let { bitmapCache[it] }
                marker.icon = when {
                    cached != null -> MapMarkerIcons.spotPhotoIcon(context, cached, isSelected)
                    else -> MapMarkerIcons.spotIcon(context, isSelected)
                }
                // 미캐시 사진은 비동기 로드 후 (마커가 여전히 같은 spot 이면) 아이콘 교체.
                if (cached == null && url != null) {
                    val targetSpotId = key.spotId
                    scope.launch {
                        val bmp = loadBitmap(context, url) ?: return@launch
                        bitmapCache[url] = bmp
                        if (marker.tag == targetSpotId) {
                            marker.icon = MapMarkerIcons.spotPhotoIcon(
                                context, bmp, targetSpotId == selectedIdProvider(),
                            )
                        }
                    }
                }
                marker.width = Marker.SIZE_AUTO
                marker.height = Marker.SIZE_AUTO
                marker.anchor = PointF(0.5f, 0.5f)
                marker.zIndex = 200
                marker.onClickListener = Overlay.OnClickListener {
                    onSpotTap(key.spotId)
                    true
                }
            }
        })
        .clusterMarkerUpdater(object : DefaultClusterMarkerUpdater() {
            override fun updateClusterMarker(info: ClusterMarkerInfo, marker: Marker) {
                super.updateClusterMarker(info, marker)
                // 기본 updater 가 설정하는 캡션 숫자를 제거 → 우리가 그린 아이콘 숫자와 겹침 방지.
                marker.captionText = ""
                marker.icon = MapMarkerIcons.clusterIcon(context, info.size)
                marker.width = Marker.SIZE_AUTO
                marker.height = Marker.SIZE_AUTO
                marker.anchor = PointF(0.5f, 0.5f)
                marker.zIndex = 300
                // 클러스터 탭 → 바텀시트가 아니라 해당 위치로 확대(클러스터가 하위 단위로 펼쳐짐).
                marker.onClickListener = Overlay.OnClickListener {
                    map.moveCamera(
                        CameraUpdate.scrollAndZoomTo(info.position, map.cameraPosition.zoom + 2.0)
                            .animate(CameraAnimation.Easing),
                    )
                    true
                }
            }
        })
        .tagMergeStrategy { cluster ->
            // 자식 노드들의 spotId 를 평탄화해 ClusterMarkerInfo.getTag() 로 노출.
            // 직속 자식이 Leaf 면 tag 는 Long(spotId), Cluster 면 이미 List<Long> 이므로 둘 다 처리.
            cluster.children.flatMap { node ->
                when (val tag = node.tag) {
                    is Long -> listOf(tag)
                    is List<*> -> tag.filterIsInstance<Long>()
                    else -> emptyList()
                }
            }
        }
        .build()

    val keyTagMap = spots.associate { spot ->
        MapSpotClusterKey(
            spotId = spot.id.toLongOrNull() ?: spot.id.hashCode().toLong(),
            position = LatLng(spot.latitude, spot.longitude),
        ) to (spot.id.toLongOrNull() ?: 0L)
    }
    clusterer.addAll(keyTagMap)
    clusterer.map = map
    clusterer.setTagOf("hash", newHash)
    ref.value = clusterer
}

/**
 * 마이스팟 마커 렌더 — 클러스터링 미참여 별도 [Marker] 리스트. zIndex 250.
 */
private fun renderMySpotMarkers(
    naverMap: NaverMap,
    markers: MutableList<Marker>,
    mySpots: List<MySpotMarker>,
    context: Context,
    selectedSpotId: Long?,
    onTap: (Long) -> Unit,
) {
    markers.forEach { it.map = null }
    markers.clear()

    mySpots.forEach { spot ->
        val isSelected = spot.spotId == selectedSpotId
        val marker = Marker().apply {
            position = LatLng(spot.coordinates.latitude, spot.coordinates.longitude)
            icon = MapMarkerIcons.mySpotIcon(context, isSelected)
            width = Marker.SIZE_AUTO
            height = Marker.SIZE_AUTO
            anchor = PointF(0.5f, 0.5f)
            zIndex = 250
            onClickListener = Overlay.OnClickListener {
                onTap(spot.spotId)
                true
            }
            map = naverMap
        }
        markers.add(marker)
    }
}

// MARK: - Clusterer hash tag helper (재생성 회피)

private val clustererTags = java.util.WeakHashMap<Clusterer<*>, MutableMap<String, Any?>>()

private fun Clusterer<*>.setTagOf(key: String, value: Any?) {
    val map = clustererTags.getOrPut(this) { mutableMapOf() }
    map[key] = value
}

private fun Clusterer<*>.tagOf(key: String): Any? = clustererTags[this]?.get(key)

/** Coil 로 원격 이미지를 비트맵으로 로드. 실패 시 null. */
private suspend fun loadBitmap(context: Context, url: String): Bitmap? {
    val request = ImageRequest.Builder(context)
        .data(url)
        .allowHardware(false) // Canvas 에 그리기 위해 소프트웨어 비트맵 필요.
        .build()
    val result = runCatching { context.imageLoader.execute(request) }.getOrNull()
    return (result as? SuccessResult)?.drawable?.toBitmap()
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
