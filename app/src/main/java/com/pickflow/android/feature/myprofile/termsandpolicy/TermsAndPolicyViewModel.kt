package com.pickflow.android.feature.myprofile.termsandpolicy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pickflow.android.core.services.protocols.AppVersionService
import com.pickflow.android.core.services.protocols.TermsPolicy
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * iOS `TermsAndPolicyListView` 데이터 1:1 — app config(임시 `/v1/app/config/ios`) 의
 * `termsPolicies`(서비스 이용약관 / 위치정보 서비스 이용약관 / 개인정보처리방침) 목록.
 * 문서 본문은 화면 내 인앱 웹뷰(`TermsAndPolicyDocScreen`, 데스크탑 UA)로 표시.
 */
@HiltViewModel
class TermsAndPolicyViewModel @Inject constructor(
    private val appVersionService: AppVersionService,
) : ViewModel() {

    private val _documents = MutableStateFlow<List<TermsPolicy>>(emptyList())
    val documents: StateFlow<List<TermsPolicy>> = _documents.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _documents.value = runCatching { appVersionService.fetchAndroidVersionPolicy() }
                .getOrNull()
                ?.termsPolicies
                .orEmpty()
        }
    }
}
