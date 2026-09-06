package com.pickflow.android.core.services.impl

import com.pickflow.android.core.services.protocols.NewFeatureConfig
import com.pickflow.android.core.services.protocols.NewFeatureGuide
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 판정 규칙은 [PrefsNewFeatureGuideTest] 가 본다. 다른 테스트는 켜짐/꺼짐만 필요해서 이걸 쓴다.
 *
 * [activeAfterRefresh] 를 다르게 주면 "fetch 가 끝나야 켜지는" 상황을 흉내 낼 수 있다.
 */
class FakeNewFeatureGuide(
    private var active: Boolean = true,
    private val activeAfterRefresh: Boolean = active,
) : NewFeatureGuide {

    private val _config = MutableStateFlow<NewFeatureConfig?>(null)
    override val config: StateFlow<NewFeatureConfig?> = _config.asStateFlow()

    var refreshCount = 0
        private set

    override fun isActive(key: String, now: Long): Boolean = active

    override suspend fun refresh() {
        refreshCount++
        active = activeAfterRefresh
        _config.value = NewFeatureConfig()
    }
}
