package com.pickflow.android.core.services.impl.compat

import com.pickflow.android.core.services.impl.DefaultSpotListService
import com.pickflow.android.core.services.protocols.Coordinates
import com.pickflow.android.core.services.protocols.SpotListService
import com.pickflow.android.core.services.protocols.SpotPage
import com.pickflow.android.core.services.protocols.SpotSort
import com.pickflow.android.core.services.protocols.SpotTheme
import javax.inject.Inject

/**
 * PV-59 임시 데코레이터 — 백엔드 완료 시 삭제. 배경은 [MoodBackendCompat] 참고.
 *
 * 실서버 호출은 [DefaultSpotListService]에 그대로 위임하고, 서버가 처리하지 못하는
 * 부분(다중 필터 / 신규 무드 2종)만 이 계층에서 메운다.
 */
class MoodCompatSpotListService @Inject constructor(
    private val delegate: DefaultSpotListService,
) : SpotListService {

    override suspend fun fetch(
        themes: Set<SpotTheme>,
        page: Int,
        coordinates: Coordinates?,
        sort: SpotSort,
    ): SpotPage {
        val serverPage = if (MoodBackendCompat.shouldSkipNetwork(themes)) {
            null
        } else {
            delegate.fetch(
                themes = MoodBackendCompat.serverQueryThemes(themes),
                page = page,
                coordinates = coordinates,
                sort = sort,
            )
        }

        val serverItems = MoodBackendCompat.filterServerItems(
            items = serverPage?.items.orEmpty(),
            selected = themes,
            themeOf = { it.theme },
        )

        // stub 은 첫 페이지에만 붙인다 — 매 페이지 붙이면 그리드 key 가 중복된다.
        val stubs = if (page == 0) MoodBackendCompat.stubSpots(themes) else emptyList()

        return SpotPage(
            items = serverItems + stubs,
            page = serverPage?.page ?: page,
            hasNext = serverPage?.hasNext ?: false,
        )
    }
}
