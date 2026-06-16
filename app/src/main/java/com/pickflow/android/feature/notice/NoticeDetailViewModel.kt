package com.pickflow.android.feature.notice

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pickflow.android.BuildConfig
import com.pickflow.android.app.navigation.PickflowRoute
import com.pickflow.android.common.ui.LoadState
import com.pickflow.android.core.services.protocols.BoardPostDetail
import com.pickflow.android.core.services.protocols.BoardService
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 공지사항 게시글 상세 ViewModel — `BoardService.detail(masterId, postId)` 1회 호출.
 *
 * postId 는 NavHost 의 `navArgument(ARG_NOTICE_POST_ID)` 에서 [SavedStateHandle] 로 전달.
 */
@HiltViewModel
class NoticeDetailViewModel @Inject constructor(
    private val boardService: BoardService,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val masterId: Long = BuildConfig.NOTICE_BOARD_MASTER_ID
    private val postId: Long = savedStateHandle.get<Long>(PickflowRoute.ARG_NOTICE_POST_ID) ?: 0L

    private val _post = MutableStateFlow<LoadState<BoardPostDetail>>(LoadState.Idle)
    val post: StateFlow<LoadState<BoardPostDetail>> = _post.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _post.value = LoadState.Loading
            _post.value = runCatching { boardService.detail(masterId, postId) }
                .fold(
                    onSuccess = { LoadState.Loaded(it) },
                    onFailure = { LoadState.Failed(it) },
                )
        }
    }
}
