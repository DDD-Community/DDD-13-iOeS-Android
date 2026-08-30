package com.pickflow.android.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pickflow.android.core.services.protocols.OnboardingCompletionStore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val completionStore: OnboardingCompletionStore,
) : ViewModel() {

    private val _pageIndex = MutableStateFlow(0)
    val pageIndex: StateFlow<Int> = _pageIndex.asStateFlow()

    private val _completed = MutableStateFlow(false)
    val completed: StateFlow<Boolean> = _completed.asStateFlow()

    val pages: List<OnboardingPage> = OnboardingPage.entries

    fun next() {
        val nextIndex = _pageIndex.value + 1
        if (nextIndex >= pages.size) finish() else _pageIndex.value = nextIndex
    }

    fun previous() {
        if (_pageIndex.value > 0) _pageIndex.value -= 1
    }

    /** 스와이프 페이저가 보고하는 현재 페이지를 반영한다(범위 밖 값은 clamp). */
    fun setPage(index: Int) {
        _pageIndex.value = index.coerceIn(0, pages.lastIndex)
    }

    fun finish() {
        viewModelScope.launch {
            completionStore.setCompleted(true)
            _completed.value = true
        }
    }

}

enum class OnboardingPage(val titleKey: String) {
    INTRO("onboarding.intro"),
    EXPLORE("onboarding.explore"),
    REGISTER("onboarding.register"),
    READY("onboarding.ready"),
}
