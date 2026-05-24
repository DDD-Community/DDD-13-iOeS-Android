package com.pickflow.android.feature.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pickflow.android.common.ui.LoadState
import com.pickflow.android.core.services.protocols.Cluster
import com.pickflow.android.core.services.protocols.ClusteringService
import com.pickflow.android.core.services.protocols.Spot
import com.pickflow.android.core.services.protocols.SpotListService
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class HomeMapViewModel @Inject constructor(
    private val spotListService: SpotListService,
    private val clusteringService: ClusteringService,
) : ViewModel() {

    private val _clusters = MutableStateFlow<LoadState<List<Cluster>>>(LoadState.Idle)
    val clusters: StateFlow<LoadState<List<Cluster>>> = _clusters.asStateFlow()

    private val _zoom = MutableStateFlow(10)
    val zoom: StateFlow<Int> = _zoom.asStateFlow()

    private val _selectedMood = MutableStateFlow<MoodFilter?>(null)
    val selectedMood: StateFlow<MoodFilter?> = _selectedMood.asStateFlow()

    private val _mapListMode = MutableStateFlow(MapListMode.MAP)
    val mapListMode: StateFlow<MapListMode> = _mapListMode.asStateFlow()

    private val _selectedCluster = MutableStateFlow<Cluster?>(null)
    val selectedCluster: StateFlow<Cluster?> = _selectedCluster.asStateFlow()

    /** 클러스터링에 쓰인 스팟 목록 — 바텀시트가 spotId로 조회. */
    private var loadedSpots: List<Spot> = emptyList()

    fun load() {
        viewModelScope.launch {
            _clusters.value = LoadState.Loading
            _clusters.value = runCatching {
                val all: List<Spot> = spotListService.fetch(null, null, 100).items
                // mock 데이터엔 무드 개념이 없어 인덱스 패리티로 무드를 배정해 필터링한다.
                val filtered = when (_selectedMood.value) {
                    null -> all
                    MoodFilter.Sunset -> all.filterIndexed { i, _ -> i % 2 == 0 }
                    MoodFilter.Reflection -> all.filterIndexed { i, _ -> i % 2 == 1 }
                }
                loadedSpots = filtered
                clusteringService.cluster(filtered, _zoom.value)
            }.fold(
                onSuccess = { if (it.isEmpty()) LoadState.Empty else LoadState.Loaded(it) },
                onFailure = { loadedSpots = emptyList(); LoadState.Failed(it) },
            )
        }
    }

    fun setZoom(level: Int) {
        _zoom.value = level
        load()
    }

    /** 같은 무드를 다시 누르면 해제(iOS `selectedMood = nil` 토글과 동일). */
    fun selectMood(mood: MoodFilter) {
        _selectedMood.value = if (_selectedMood.value == mood) null else mood
        load()
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

    /** 바텀시트가 클러스터의 대표 spotId로 원본 Spot을 조회한다. */
    fun spotById(id: String): Spot? = loadedSpots.firstOrNull { it.id == id }
}

/** iOS `MapListMode` 와 동일 — 세그먼트 토글 라벨용 표시명 포함. */
enum class MapListMode(val displayName: String) {
    MAP("지도"),
    LIST("리스트"),
}
