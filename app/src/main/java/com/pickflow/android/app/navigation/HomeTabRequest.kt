package com.pickflow.android.app.navigation

import com.pickflow.android.feature.archive.ArchiveTab
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * HOME 바깥 라우트(예: 스팟 등록 완료)에서 HOME 의 하단 탭/보관함 내부 탭 전환을
 * 요청하는 채널. [DeepLinkState] 와 동일한 set → consume 패턴.
 */
object HomeTabRequest {
    data class Request(val tab: HomeTab, val archiveTab: ArchiveTab? = null)

    private val _pending = MutableStateFlow<Request?>(null)
    val pending: StateFlow<Request?> = _pending.asStateFlow()

    fun request(tab: HomeTab, archiveTab: ArchiveTab? = null) {
        _pending.value = Request(tab, archiveTab)
    }

    fun clear() { _pending.value = null }
}
