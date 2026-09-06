package com.pickflow.android.core.services.impl

import androidx.test.core.app.ApplicationProvider
import com.pickflow.android.core.services.protocols.TokenStore
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * "봤음" 이 **계정별로 갈리는지** 가 핵심이다. V2 안내(기기 단위)와 헷갈리면 같은 기기를 쓰는
 * 두 사람 중 한 명이 안내를 영영 못 받는다.
 */
@RunWith(RobolectricTestRunner::class)
class PrefsSpotOpenGuideStoreTest {

    private val tokenStore = FakeTokenStore()

    @Before
    fun setUp() {
        context().getSharedPreferences("spot_open_guide", android.content.Context.MODE_PRIVATE)
            .edit().clear().commit()
    }

    @Test
    fun `비로그인이면 판정 대상이 아니다`() = runBlocking {
        assertNull(store().hasSeen())
    }

    @Test
    fun `로그인했고 처음이면 아직 안 봤다`() = runBlocking {
        tokenStore.saveUserId("user-a")

        assertEquals(false, store().hasSeen())
    }

    @Test
    fun `봤다고 표시하면 다음부터는 봤다고 나온다`() = runBlocking {
        tokenStore.saveUserId("user-a")
        val store = store()

        store.markSeen()

        assertEquals(true, store.hasSeen())
    }

    @Test
    fun `계정이 다르면 각자 한 번씩 본다`() = runBlocking {
        tokenStore.saveUserId("user-a")
        val store = store()
        store.markSeen()

        // 같은 기기에서 다른 계정으로 갈아탄다.
        tokenStore.saveUserId("user-b")

        assertEquals("A 가 봤다고 B 까지 본 게 되면 안 된다", false, store.hasSeen())
    }

    @Test
    fun `로그아웃 후 같은 계정으로 다시 들어와도 봤음이 유지된다`() = runBlocking {
        tokenStore.saveUserId("user-a")
        val store = store()
        store.markSeen()

        tokenStore.clear()
        assertNull("로그아웃 상태에서는 판정하지 않는다", store.hasSeen())

        tokenStore.saveUserId("user-a")
        assertEquals(true, store.hasSeen())
    }

    @Test
    fun `비로그인에서 봤다고 표시해도 아무 일도 없다`() = runBlocking {
        val store = store()

        store.markSeen()
        tokenStore.saveUserId("user-a")

        assertFalse("로그아웃 상태의 표시가 아무 계정에도 새면 안 된다", store.hasSeen() == true)
    }

    @Test
    fun `앱을 다시 켜도 봤음이 남는다`() = runBlocking {
        tokenStore.saveUserId("user-a")
        store().markSeen()

        assertTrue(store().hasSeen() == true)
    }

    private fun store() = PrefsSpotOpenGuideStore(context(), tokenStore)

    private fun context() = ApplicationProvider.getApplicationContext<android.content.Context>()

    private class FakeTokenStore : TokenStore {
        private var user: String? = null
        override suspend fun save(accessToken: String, refreshToken: String?) = Unit
        override suspend fun accessToken(): String? = null
        override suspend fun refreshToken(): String? = null
        override suspend fun saveUserId(userId: String) { user = userId }
        override suspend fun userId(): String? = user
        override suspend fun clear() { user = null }
    }
}
