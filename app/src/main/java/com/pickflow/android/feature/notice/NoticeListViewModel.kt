package com.pickflow.android.feature.notice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pickflow.android.BuildConfig
import com.pickflow.android.common.ui.LoadState
import com.pickflow.android.core.services.protocols.BoardPost
import com.pickflow.android.core.services.protocols.BoardService
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 공지사항 게시판 목록 ViewModel.
 *
 * `BuildConfig.NOTICE_BOARD_MASTER_ID` 로 지정된 단일 마스터의 게시글을 페이지 누적
 * 로드한다. BE 응답에서 pinned=true 항목은 상단 고정 — 클라에서 별도 정렬 X.
 */
@HiltViewModel
class NoticeListViewModel @Inject constructor(
    private val boardService: BoardService,
) : ViewModel() {

    private val _posts = MutableStateFlow<LoadState<List<BoardPost>>>(LoadState.Idle)
    val posts: StateFlow<LoadState<List<BoardPost>>> = _posts.asStateFlow()

    private val _isLoadingNextPage = MutableStateFlow(false)
    val isLoadingNextPage: StateFlow<Boolean> = _isLoadingNextPage.asStateFlow()

    private val masterId: Long = BuildConfig.NOTICE_BOARD_MASTER_ID
    private var nextPage: Int = 0
    private var hasMore: Boolean = true
    private val accumulated = mutableListOf<BoardPost>()

    fun refresh() {
        nextPage = 0
        hasMore = true
        accumulated.clear()
        _isLoadingNextPage.value = false
        loadPage()
    }

    fun loadNextPage() {
        if (!hasMore || _isLoadingNextPage.value) return
        if (accumulated.isEmpty()) return
        loadPage()
    }

    private fun loadPage() {
        val isFirstPage = accumulated.isEmpty()
        viewModelScope.launch {
            if (isFirstPage) {
                _posts.value = LoadState.Loading
            } else {
                _isLoadingNextPage.value = true
            }
            runCatching { boardService.posts(masterId = masterId, page = nextPage) }
                .onSuccess { page ->
                    accumulated.addAll(page.items)
                    hasMore = page.hasNext
                    nextPage = page.page + 1
                    emitState()
                }
                .onFailure {
                    if (isFirstPage) _posts.value = LoadState.Failed(it)
                    // 다음 페이지 실패는 누적 리스트 유지 — iOS와 동일 정책.
                }
            if (!isFirstPage) _isLoadingNextPage.value = false
        }
    }

    private fun emitState() {
        _posts.value = when {
            accumulated.isEmpty() -> LoadState.Empty
            else -> LoadState.Loaded(accumulated.toList())
        }
    }
}
