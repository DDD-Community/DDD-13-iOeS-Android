package com.pickflow.android.feature.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pickflow.android.common.ui.LoadState
import com.pickflow.android.core.services.protocols.Cluster
import com.pickflow.android.core.services.protocols.ClusteringService
import com.pickflow.android.core.services.protocols.Coordinates
import com.pickflow.android.core.services.protocols.LocationService
import com.pickflow.android.core.services.protocols.Spot
import com.pickflow.android.core.services.protocols.SpotListService
import com.pickflow.android.core.services.protocols.SpotMapService
import com.pickflow.android.core.services.protocols.SpotTheme
import com.pickflow.android.core.services.protocols.ViewportBox
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class HomeMapViewModel @Inject constructor(
    private val spotListService: SpotListService,
    private val spotMapService: SpotMapService,
    private val clusteringService: ClusteringService,
    private val locationService: LocationService,
) : ViewModel() {

    private val _clusters = MutableStateFlow<LoadState<List<Cluster>>>(LoadState.Idle)
    val clusters: StateFlow<LoadState<List<Cluster>>> = _clusters.asStateFlow()

    private val _zoom = MutableStateFlow(12)
    val zoom: StateFlow<Int> = _zoom.asStateFlow()

    private val _selectedMood = MutableStateFlow<MoodFilter?>(null)
    val selectedMood: StateFlow<MoodFilter?> = _selectedMood.asStateFlow()

    private val _mapListMode = MutableStateFlow(MapListMode.MAP)
    val mapListMode: StateFlow<MapListMode> = _mapListMode.asStateFlow()

    private val _selectedCluster = MutableStateFlow<Cluster?>(null)
    val selectedCluster: StateFlow<Cluster?> = _selectedCluster.asStateFlow()

    /** 사용자가 "현재 위치" 버튼을 눌렀을 때 지도가 카메라를 이동할 좌표. */
    private val _cameraTarget = MutableStateFlow<Coordinates?>(null)
    val cameraTarget: StateFlow<Coordinates?> = _cameraTarget.asStateFlow()

    /** 클러스터링/바텀시트 조회에 쓰이는 현재 표시 중 스팟 목록. */
    private var loadedSpots: List<Spot> = emptyList()

    /** 마지막으로 본 뷰포트 — 무드 필터 변경 시 재요청용. */
    private var lastViewport: ViewportBox? = null

    fun load() {
        viewModelScope.launch {
            _clusters.value = LoadState.Loading
            _clusters.value = runCatching {
                val all = spotListService.fetch(theme = themeForMood(_selectedMood.value), page = 0).items
                loadedSpots = all
                clusteringService.cluster(all, _zoom.value)
            }.fold(
                onSuccess = { if (it.isEmpty()) LoadState.Empty else LoadState.Loaded(it) },
                onFailure = { loadedSpots = emptyList(); LoadState.Failed(it) },
            )
        }
    }

    /** 지도의 카메라가 멈출 때마다 호출 — viewport API로 마커를 갱신한다. */
    fun onViewportChanged(box: ViewportBox, zoomLevel: Int) {
        lastViewport = box
        _zoom.value = zoomLevel
        viewModelScope.launch {
            _clusters.value = LoadState.Loading
            _clusters.value = runCatching {
                val markers = spotMapService.fetchInViewport(box, themeForMood(_selectedMood.value))
                val spots = markers.map { it.toSpot() }
                loadedSpots = spots
                clusteringService.cluster(spots, zoomLevel)
            }.fold(
                onSuccess = { if (it.isEmpty()) LoadState.Empty else LoadState.Loaded(it) },
                onFailure = { loadedSpots = emptyList(); LoadState.Failed(it) },
            )
        }
    }

    fun setZoom(level: Int) {
        _zoom.value = level
        lastViewport?.let { onViewportChanged(it, level) } ?: load()
    }

    fun selectMood(mood: MoodFilter) {
        _selectedMood.value = if (_selectedMood.value == mood) null else mood
        lastViewport?.let { onViewportChanged(it, _zoom.value) } ?: load()
    }

    fun selectMapListMode(mode: MapListMode) {
        _mapListMode.value = mode
    }

    fun selectCluster(cluster: Cluster) {
        _selectedCluster.value = cluster
    }

    fun dismissCluster() {
        _selectedCluster.value = null
    }

    /** "현재 위치" 버튼 — LocationService에서 좌표를 받아 카메라를 옮긴다. */
    fun moveToCurrentLocation() {
        viewModelScope.launch {
            runCatching { locationService.currentLocation() }
                .getOrNull()
                ?.let { _cameraTarget.value = it }
        }
    }

    /** NaverMapView 가 카메라 이동을 처리한 뒤 호출 — 동일 좌표 재이동 방지. */
    fun consumeCameraTarget() {
        _cameraTarget.value = null
    }

    fun spotById(id: String): Spot? = loadedSpots.firstOrNull { it.id == id }

    private fun themeForMood(mood: MoodFilter?): SpotTheme? = when (mood) {
        MoodFilter.Sunset -> SpotTheme.SUNSET
        MoodFilter.Reflection -> SpotTheme.YUNSEUL
        null -> null
    }
}

private fun com.pickflow.android.core.services.protocols.SpotMapMarker.toSpot(): Spot = Spot(
    id = spotId.toString(),
    name = "",
    theme = SpotTheme.SUNSET,
    latitude = coordinates.latitude,
    longitude = coordinates.longitude,
    imageUrl = imageUrl,
)

/** iOS `MapListMode` 와 동일 — 세그먼트 토글 라벨용 표시명 포함. */
enum class MapListMode(val displayName: String) {
    MAP("지도"),
    LIST("리스트"),
}
