package com.pickflow.android.core.services.impl

import androidx.test.core.app.ApplicationProvider
import com.pickflow.android.core.services.protocols.NewFeature
import com.pickflow.android.core.services.protocols.NewFeatureConfig
import com.pickflow.android.core.services.protocols.NewFeatureConfigProvider
import com.pickflow.android.core.services.protocols.NewFeatureKeys
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * 판정 규칙(PV-79 §2)은 iOS 와 공유하는 계약이라 여기서 못 박는다.
 * Firebase 는 [FakeProvider] 로 갈아끼우므로 네트워크도 google-services.json 도 필요 없다.
 */
@RunWith(RobolectricTestRunner::class)
class PrefsNewFeatureGuideTest {

    private val provider = FakeProvider()

    /** 2026-01-01 00:00:00 UTC 근처의 임의 기준 시각. 값 자체에 의미는 없다. */
    private val now = 1_767_225_600_000L
    private val day = 86_400_000L

    @Before
    fun setUp() {
        context().getSharedPreferences("new_feature_guide", android.content.Context.MODE_PRIVATE)
            .edit().clear().commit()
    }

    @Test
    fun `fetch 전에는 꺼져 있고 fetch 후 활성 구간이면 켜진다`() = runBlocking {
        provider.config = config(NewFeature(V2, startAt = now - day, endAt = now + day))
        val guide = guide()

        assertFalse("아직 아무것도 안 받아왔으면 꺼져 있어야 한다", guide.isActive(V2, now))

        guide.refresh()

        assertTrue(guide.isActive(V2, now))
    }

    @Test
    fun `런칭 기준 - startAt 은 포함하고 endAt 은 포함하지 않는다`() = runBlocking {
        val start = now
        val end = now + 7 * day
        provider.config = config(NewFeature(V2, startAt = start, endAt = end))
        val guide = guide().apply { refresh() }

        assertFalse("시작 1ms 전", guide.isActive(V2, start - 1))
        assertTrue("시작 시점은 포함", guide.isActive(V2, start))
        assertTrue(guide.isActive(V2, end - 1))
        assertFalse("종료 시점은 미포함", guide.isActive(V2, end))
    }

    @Test
    fun `런칭 기준 - endAt 이 없으면 durationDays 로 끝을 잡는다`() = runBlocking {
        provider.config = config(NewFeature(V2, startAt = now, durationDays = 3))
        val guide = guide().apply { refresh() }

        assertTrue(guide.isActive(V2, now + 3 * day - 1))
        assertFalse(guide.isActive(V2, now + 3 * day))
    }

    @Test
    fun `런칭 기준 - endAt 이 있으면 durationDays 는 무시된다`() = runBlocking {
        provider.config = config(
            NewFeature(V2, startAt = now, endAt = now + day, durationDays = 30),
        )
        val guide = guide().apply { refresh() }

        assertFalse("endAt 이 이겼다면 하루 뒤엔 꺼져 있다", guide.isActive(V2, now + day))
    }

    @Test
    fun `startAt 만 있고 끝이 없으면 무기한이 아니라 꺼진다`() = runBlocking {
        provider.config = config(NewFeature(V2, startAt = now - day))
        val guide = guide().apply { refresh() }

        assertFalse(guide.isActive(V2, now))
    }

    @Test
    fun `유저 기준 - 최초 판정 시각을 심고 기간이 지나면 꺼진다`() = runBlocking {
        provider.config = config(NewFeature(V2, durationDays = 14))
        val guide = guide().apply { refresh() }

        assertTrue("최초 판정 시각이 여기서 심긴다", guide.isActive(V2, now))
        assertTrue(guide.isActive(V2, now + 14 * day - 1))
        assertFalse(guide.isActive(V2, now + 14 * day))

        // 앱을 다시 켜도(=새 인스턴스) 심어둔 최초 판정 시각을 그대로 쓴다.
        val restarted = guide().apply { refresh() }
        assertFalse(restarted.isActive(V2, now + 14 * day))
    }

    @Test
    fun `유저 기준 - durationDays 가 없으면 꺼진다`() = runBlocking {
        provider.config = config(NewFeature(V2))
        val guide = guide().apply { refresh() }

        assertFalse(guide.isActive(V2, now))
    }

    @Test
    fun `설정에 없는 key 는 꺼진다`() = runBlocking {
        provider.config = config(NewFeature(NewFeatureKeys.HOME_NEW_BADGE, durationDays = 14))
        val guide = guide().apply { refresh() }

        assertFalse(guide.isActive(V2, now))
    }

    @Test
    fun `fetch 실패 시 마지막 캐시로 판정한다`() = runBlocking {
        provider.config = config(NewFeature(V2, startAt = now - day, endAt = now + 30 * day))
        guide().refresh()

        // 오프라인 재시작 — 원격도 activate 된 값도 없다.
        provider.failing = true
        provider.activated = null
        val offline = guide()
        offline.refresh()

        assertTrue("캐시가 남아 있으면 오프라인에서도 판정은 돈다", offline.isActive(V2, now))
    }

    @Test
    fun `fetch 실패해도 이미 activate 된 값이 있으면 그걸 쓴다`() = runBlocking {
        provider.failing = true
        provider.activated = config(NewFeature(V2, startAt = now - day, endAt = now + day))
        val guide = guide()

        guide.refresh()

        assertTrue(guide.isActive(V2, now))
    }

    @Test
    fun `파라미터가 비어 있으면 전부 꺼진다`() = runBlocking {
        provider.config = NewFeatureConfig()
        val guide = guide().apply { refresh() }

        assertFalse(guide.isActive(V2, now))
        assertFalse(guide.isActive(NewFeatureKeys.HOME_NEW_BADGE, now))
        assertFalse(guide.isActive(NewFeatureKeys.SPOT_OPEN_GUIDE, now))
    }

    @Test
    fun `fetch 가 끝나면 config 가 갱신돼 구독자가 재평가한다`() = runBlocking {
        provider.config = config(NewFeature(V2, startAt = now - day, endAt = now + day))
        val guide = guide()

        assertEquals(null, guide.config.value)

        guide.refresh()

        assertEquals(1, guide.config.value?.features?.size)
    }

    private fun guide() = PrefsNewFeatureGuide(context(), provider)

    private fun context() = ApplicationProvider.getApplicationContext<android.content.Context>()

    private fun config(vararg features: NewFeature) = NewFeatureConfig(features.toList())

    private class FakeProvider : NewFeatureConfigProvider {
        var config: NewFeatureConfig = NewFeatureConfig()
        var activated: NewFeatureConfig? = null
        var failing = false

        override suspend fun fetchFeatureConfig(): NewFeatureConfig {
            if (failing) throw java.io.IOException("offline")
            activated = config
            return config
        }

        override fun activatedFeatureConfig(): NewFeatureConfig? = activated
    }

    private companion object {
        const val V2 = NewFeatureKeys.V2_UPDATE_MODAL
    }
}
