package com.pickflow.android.core.services.impl.compat

import com.pickflow.android.core.services.impl.DefaultSpotMapService
import com.pickflow.android.core.services.protocols.SpotMapMarker
import com.pickflow.android.core.services.protocols.SpotMapService
import com.pickflow.android.core.services.protocols.SpotTheme
import com.pickflow.android.core.services.protocols.ViewportBox
import javax.inject.Inject

/**
 * PV-59 임시 데코레이터 — 백엔드 완료 시 삭제. 배경은 [MoodBackendCompat] 참고.
 *
 * viewport 응답은 마커에 `theme` 필드가 없어(서버 `SpotSummaryDto` 한계) 클라이언트
 * 재필터가 불가능하다. 따라서 서버가 2개 이상을 처리하지 못하는 경우
 * **필터를 걸지 않은 전체 마커**를 그대로 보여주고, 신규 무드 stub 마커만 얹는다.
 */
class MoodCompatSpotMapService @Inject constructor(
    private val delegate: DefaultSpotMapService,
) : SpotMapService {

    override suspend fun fetchInViewport(
        box: ViewportBox,
        themes: Set<SpotTheme>,
    ): List<SpotMapMarker> {
        val serverMarkers = if (MoodBackendCompat.shouldSkipNetwork(themes)) {
            emptyList()
        } else {
            delegate.fetchInViewport(box, MoodBackendCompat.serverQueryThemes(themes))
        }

        return serverMarkers + MoodBackendCompat.stubMarkers(box, themes)
    }
}
