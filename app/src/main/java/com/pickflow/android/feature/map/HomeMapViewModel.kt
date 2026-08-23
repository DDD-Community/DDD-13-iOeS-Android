package com.pickflow.android.feature.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pickflow.android.common.ui.LoadState
import com.pickflow.android.core.services.protocols.AuthService
import com.pickflow.android.core.services.protocols.BookmarkService
import com.pickflow.android.core.services.protocols.Coordinates
import com.pickflow.android.core.services.protocols.ExternalAppLauncher
import com.pickflow.android.core.services.protocols.LocationService
import com.pickflow.android.core.services.protocols.Spot
import com.pickflow.android.core.services.protocols.SpotListService
import com.pickflow.android.core.services.protocols.SpotMapMarker
import com.pickflow.android.core.services.protocols.SpotMapService
import com.pickflow.android.core.services.protocols.SpotPreview
import com.pickflow.android.core.services.protocols.SpotService
import com.pickflow.android.core.services.protocols.SpotTheme
import com.pickflow.android.core.services.protocols.ViewportBox
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * iOS `MapClusteringViewModel` 1:1 — viewport API → 큐레이션/마이 partition.
 *
 * 클러스터링은 NaverMapView 가 SDK 의 `Clusterer` 로 직접 수행한다(이전엔 자체 grid
 * 알고리즘). ViewModel 은 raw 스팟 목록만 export 한다.
 */
@HiltViewModel
class HomeMapViewModel @Inject constructor(
    private val spotListService: SpotListService,
    private val spotMapService: SpotMapService,
    private val locationService: LocationService,
    private val spotService: SpotService,
    private val authService: AuthService,
    private val bookmarkService: BookmarkService,
    private val externalAppLauncher: ExternalAppLauncher,
) : ViewModel() {

    /** 큐레이션 스팟(클러스터링 대상). NaverMapView 가 SDK Clusterer 에 넣는다. */
    private val _curationSpots = MutableStateFlow<LoadState<List<Spot>>>(LoadState.Idle)
    val curationSpots: StateFlow<LoadState<List<Spot>>> = _curationSpots.asStateFlow()

    /** 마이스팟(클러스터링 미참여). 항상 단일 마커. */
    private val _mySpots = MutableStateFlow<List<MySpotMarker>>(emptyList())
    val mySpots: StateFlow<List<MySpotMarker>> = _mySpots.asStateFlow()

    /** 선택된 spotId — leaf/my 마커의 sunsetOrange stroke 토글에 사용. */
    private val _selectedSpotId = MutableStateFlow<Long?>(null)
    val selectedSpotId: StateFlow<Long?> = _selectedSpotId.asStateFlow()

    private val _zoom = MutableStateFlow(12)
    val zoom: StateFlow<Int> = _zoom.asStateFlow()

    private val _selectedMood = MutableStateFlow<MoodFilter?>(null)
    val selectedMood: StateFlow<MoodFilter?> = _selectedMood.asStateFlow()

    private val _mapListMode = MutableStateFlow(MapListMode.MAP)
    val mapListMode: StateFlow<MapListMode> = _mapListMode.asStateFlow()

    private val _selectedCluster = MutableStateFlow<Cluster?>(null)
    val selectedCluster: StateFlow<Cluster?> = _selectedCluster.asStateFlow()

    /** 바텀시트에 표시할 preview(GET /v1/spots/{id}/preview) 결과. */
    private val _selectedPreview = MutableStateFlow<LoadState<SpotPreview>>(LoadState.Idle)
    val selectedPreview: StateFlow<LoadState<SpotPreview>> = _selectedPreview.asStateFlow()

    /** 바텀시트 선택 스팟의 북마크 상태(낙관적 토글). */
    private val _selectedBookmarked = MutableStateFlow(false)
    val selectedBookmarked: StateFlow<Boolean> = _selectedBookmarked.asStateFlow()

    /** 비로그인 상태에서 저장 시도 시 로그인 유도 팝업 노출 플래그. */
    private val _sheetLoginPrompt = MutableStateFlow(false)
    val sheetLoginPrompt: StateFlow<Boolean> = _sheetLoginPrompt.asStateFlow()

    /** preview distance 계산용 마지막 위치(베스트에포트). */
    private var lastKnownCoordinates: Coordinates? = null

    /** 사용자가 "현재 위치" 버튼을 눌렀을 때 지도가 카메라를 이동할 좌표. */
    private val _cameraTarget = MutableStateFlow<Coordinates?>(null)
    val cameraTarget: StateFlow<Coordinates?> = _cameraTarget.asStateFlow()

    /** 마커 탭 시 바텀시트 위쪽 영역 중앙으로 정렬할 좌표(시트 높이만큼 하단 패딩 적용). */
    private val _focusTarget = MutableStateFlow<Coordinates?>(null)
    val focusTarget: StateFlow<Coordinates?> = _focusTarget.asStateFlow()

    /** spotById 등 조회용 — 큐레이션 + 마이 모두 누적. */
    private var loadedSpots: List<Spot> = emptyList()

    /** 마지막으로 본 뷰포트 — 무드 필터 변경 시 재요청용. */
    private var lastViewport: ViewportBox? = null

    fun load() {
        viewModelScope.launch {
            _curationSpots.value = LoadState.Loading
            _curationSpots.value = runCatching {
                val all = spotListService.fetch(theme = themeForMood(_selectedMood.value), page = 0).items
                loadedSpots = all
                _mySpots.value = emptyList() // 초기 fetch 는 isMySpot 정보 없음 → 전부 큐레이션 취급
                all
            }.fold(
                onSuccess = { if (it.isEmpty()) LoadState.Empty else LoadState.Loaded(it) },
                onFailure = { loadedSpots = emptyList(); LoadState.Failed(it) },
            )
        }
    }

    /**
     * 지도의 카메라가 멈출 때마다 호출 — viewport API 응답을 isMySpot 으로 partition.
     *
     * `isMySpot=false` → 큐레이션 → [_curationSpots] 로 emit (SDK Clusterer 입력)
     * `isMySpot=true`  → 마이스팟 → [_mySpots] 로 직접 emit (클러스터링 미참여)
     */
    fun onViewportChanged(box: ViewportBox, zoomLevel: Int) {
        lastViewport = box
        _zoom.value = zoomLevel
        viewModelScope.launch {
            _curationSpots.value = LoadState.Loading
            _curationSpots.value = runCatching {
                val markers = spotMapService.fetchInViewport(box, themeForMood(_selectedMood.value))
                val (mineMarkers, curationMarkers) = markers.partition { it.isMySpot }
                _mySpots.value = mineMarkers.map(SpotMapMarker::toMySpotMarker)
                val curationSpots = curationMarkers.map(SpotMapMarker::toSpot)
                loadedSpots = curationSpots + mineMarkers.map(SpotMapMarker::toSpot)
                curationSpots
            }.fold(
                onSuccess = { if (it.isEmpty()) LoadState.Empty else LoadState.Loaded(it) },
                onFailure = {
                    loadedSpots = emptyList()
                    _mySpots.value = emptyList()
                    LoadState.Failed(it)
                },
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
        val firstId = cluster.spotIds.firstOrNull()?.toLongOrNull()
        _selectedSpotId.value = firstId
        _focusTarget.value = Coordinates(cluster.latitude, cluster.longitude)
        firstId?.let { loadPreview(it) }
    }

    /** 단일 leaf/마이 마커가 탭됐을 때. iOS `handleSpotTap` 1:1. */
    fun selectSpot(spotId: Long) {
        _selectedSpotId.value = spotId
        val asString = spotId.toString()
        loadedSpots.firstOrNull { it.id == asString }?.let { spot ->
            _selectedCluster.value = Cluster(
                latitude = spot.latitude,
                longitude = spot.longitude,
                count = 1,
                spotIds = listOf(asString),
            )
            _focusTarget.value = Coordinates(spot.latitude, spot.longitude)
        }
        loadPreview(spotId)
    }

    /** NaverMapView 가 포커스 카메라 이동을 처리한 뒤 호출. */
    fun consumeFocusTarget() {
        _focusTarget.value = null
    }

    /** 선택된 스팟의 preview 정보를 비동기 로드해 바텀시트에 노출. */
    private fun loadPreview(spotId: Long) {
        _selectedBookmarked.value = false
        viewModelScope.launch {
            _selectedPreview.value = LoadState.Loading
            if (lastKnownCoordinates == null) {
                lastKnownCoordinates = runCatching { locationService.currentLocation() }.getOrNull()
            }
            _selectedPreview.value = runCatching {
                spotService.preview(spotId.toString(), lastKnownCoordinates)
            }.fold(
                onSuccess = {
                    _selectedBookmarked.value = it.isBookmarked
                    LoadState.Loaded(it)
                },
                onFailure = { LoadState.Failed(it) },
            )
        }
    }

    fun dismissCluster() {
        _selectedCluster.value = null
        _selectedSpotId.value = null
        _selectedPreview.value = LoadState.Idle
    }

    // 북마크 요청 in-flight 가드 — 연타하면 add/remove 가 교차 실행돼 서버 상태가 뒤집힌다.
    private var isBookmarkInFlight = false

    /** 바텀시트 "저장하기" — 비로그인 시 시트를 닫고 로그인 유도 팝업 노출. */
    fun bookmarkSelected() {
        if (isBookmarkInFlight) return
        isBookmarkInFlight = true
        viewModelScope.launch {
            try {
                if (!authService.isLoggedIn()) {
                    // 시트를 닫고(모달 위에 팝업을 띄울 수 없으므로) 로그인 유도 팝업으로 전환.
                    _selectedCluster.value = null
                    _selectedPreview.value = LoadState.Idle
                    _sheetLoginPrompt.value = true
                    return@launch
                }
                val id = _selectedSpotId.value?.toString() ?: return@launch
                val wasBookmarked = _selectedBookmarked.value
                _selectedBookmarked.value = !wasBookmarked // 낙관적 토글
                runCatching {
                    if (wasBookmarked) bookmarkService.remove(id) else bookmarkService.add(id)
                }.onFailure { _selectedBookmarked.value = wasBookmarked }
            } finally {
                isBookmarkInFlight = false
            }
        }
    }

    /** 바텀시트 "길 안내 받기" — 외부 지도 앱 실행. */
    fun routeToSelected() {
        val cluster = _selectedCluster.value ?: return
        val name = (_selectedPreview.value as? LoadState.Loaded)?.value?.name.orEmpty()
        viewModelScope.launch {
            externalAppLauncher.openMap(cluster.latitude, cluster.longitude, name)
        }
    }

    fun dismissSheetLoginPrompt() {
        _sheetLoginPrompt.value = false
    }

    /** 스팟 등록 버튼 — 비로그인 시 로그인 유도 팝업, 로그인 시에만 등록 화면 이동. */
    fun requestRegistration(onAllowed: () -> Unit) {
        viewModelScope.launch {
            if (authService.isLoggedIn()) onAllowed()
            else _sheetLoginPrompt.value = true
        }
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

private fun SpotMapMarker.toSpot(): Spot = Spot(
    id = spotId.toString(),
    name = "",
    theme = SpotTheme.SUNSET,
    latitude = coordinates.latitude,
    longitude = coordinates.longitude,
    imageUrl = imageUrl,
)

private fun SpotMapMarker.toMySpotMarker(): MySpotMarker = MySpotMarker(
    spotId = spotId,
    coordinates = coordinates,
    imageUrl = imageUrl,
)

/** iOS `MapListMode` 와 동일 — 세그먼트 토글 라벨용 표시명 포함. */
enum class MapListMode(val displayName: String) {
    MAP("지도"),
    LIST("리스트"),
}
