package com.pickflow.android.core.services.impl

import com.pickflow.android.core.network.api.SpotApi
import com.pickflow.android.core.network.mapper.toSpotPage
import com.pickflow.android.core.network.unwrap
import com.pickflow.android.core.services.protocols.Coordinates
import com.pickflow.android.core.services.protocols.SpotListService
import com.pickflow.android.core.services.protocols.SpotPage
import com.pickflow.android.core.services.protocols.SpotSort
import com.pickflow.android.core.services.protocols.SpotTheme
import javax.inject.Inject

class DefaultSpotListService @Inject constructor(
    private val spotApi: SpotApi,
) : SpotListService {
    override suspend fun fetch(
        themes: Set<SpotTheme>,
        page: Int,
        coordinates: Coordinates?,
        sort: SpotSort,
    ): SpotPage = spotApi.getSpots(
        page = page,
        theme = themes.toQueryValues(),
        // 서버 검증: 위/경도는 소수점 6자리까지 허용 → truncate.
        latitude = coordinates?.latitude?.toSixDecimal(),
        longitude = coordinates?.longitude?.toSixDecimal(),
        sort = sort.name,
    ).unwrap().toSpotPage()
}

/** 소수점 6자리로 잘라낸 좌표(서버 위/경도 검증 6자리 한도용). */
internal fun Double.toSixDecimal(): Double =
    kotlin.math.floor(this * 1_000_000.0) / 1_000_000.0

/**
 * 무드 필터 Set → `theme` 반복 쿼리 값. 빈 Set 은 null 이라 파라미터가 아예 붙지 않는다(전체 조회).
 *
 * Set 순회 순서가 아니라 [SpotTheme] 선언 순서로 정렬해 요청 URL 을 결정적으로 만든다.
 * 전송 문자열은 `name` 기반 — 값 자체는 PV-59 백엔드 확정시 변경 가능성 있음.
 */
internal fun Set<SpotTheme>.toQueryValues(): List<String>? =
    takeIf { it.isNotEmpty() }?.let { selected -> SpotTheme.entries.filter { it in selected }.map { it.name } }

