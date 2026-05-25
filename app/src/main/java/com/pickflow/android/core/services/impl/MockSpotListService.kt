package com.pickflow.android.core.services.impl

import com.pickflow.android.core.services.protocols.Spot
import com.pickflow.android.core.services.protocols.SpotListService
import com.pickflow.android.core.services.protocols.SpotPage
import com.pickflow.android.core.services.protocols.SpotTheme
import javax.inject.Inject

class MockSpotListService @Inject constructor() : SpotListService {

    private val all: List<Spot> = (1..50).map { idx ->
        Spot(
            id = "spot-$idx",
            name = "Mock Spot $idx",
            theme = if (idx % 2 == 0) SpotTheme.SUNSET else SpotTheme.YUNSEUL,
            latitude = 37.5 + idx * 0.001,
            longitude = 127.0 + idx * 0.001,
        )
    }

    override suspend fun fetch(theme: SpotTheme?, cursor: String?, limit: Int): SpotPage {
        val filtered = if (theme == null) all else all.filter { it.theme == theme }
        val startIndex = cursor?.toIntOrNull() ?: 0
        val endExclusive = (startIndex + limit).coerceAtMost(filtered.size)
        val page = filtered.subList(startIndex, endExclusive)
        val nextCursor = if (endExclusive < filtered.size) endExclusive.toString() else null
        return SpotPage(items = page, nextCursor = nextCursor)
    }
}
