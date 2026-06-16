package com.pickflow.android.feature.notice

import androidx.compose.runtime.Composable
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.resources.Density
import com.android.resources.ScreenOrientation
import com.pickflow.android.common.designsystem.PickflowTheme
import com.pickflow.android.common.ui.LoadState
import com.pickflow.android.core.services.protocols.BoardPost
import com.pickflow.android.core.services.protocols.BoardPostDetail
import org.junit.Rule
import org.junit.Test

class NoticeSnapshotTest {

    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = device(393, 852))

    @Test fun notice_list_loaded_mixed_dark() = snapshot {
        NoticeListContent(
            state = LoadState.Loaded(
                listOf(
                    BoardPost(1L, "[공지] 서비스 점검 안내", "2026-01-15", pinned = true),
                    BoardPost(2L, "신규 기능 업데이트 v1.1.0", "2026-01-10", pinned = false),
                    BoardPost(3L, "이용약관 변경 안내", "2026-01-05", pinned = false),
                ),
            ),
        )
    }

    @Test fun notice_list_empty_dark() = snapshot { NoticeListContent(state = LoadState.Empty) }

    @Test fun notice_list_failed_dark() = snapshot {
        NoticeListContent(state = LoadState.Failed(RuntimeException("네트워크 오류")))
    }

    @Test fun notice_detail_loaded_dark() = snapshot {
        NoticeDetailContentScreen(
            state = LoadState.Loaded(
                BoardPostDetail(
                    masterId = 1L,
                    postId = 1L,
                    title = "[공지] 서비스 점검 안내",
                    createdAt = "2026-01-15",
                    content = "안녕하세요. 서비스 점검이 예정되어 있어 안내드립니다.\n\n점검 시간: 2026-01-20 02:00 ~ 04:00",
                ),
            ),
        )
    }

    @Test fun notice_detail_failed_dark() = snapshot {
        NoticeDetailContentScreen(state = LoadState.Failed(RuntimeException("불러오기 실패")))
    }

    private fun snapshot(content: @Composable () -> Unit) {
        paparazzi.snapshot { PickflowTheme { content() } }
    }

    private companion object {
        fun device(wDp: Int, hDp: Int): DeviceConfig = DeviceConfig.PIXEL_5.copy(
            screenWidth = wDp * 2,
            screenHeight = hDp * 2,
            xdpi = 320,
            ydpi = 320,
            density = Density.XHIGH,
            orientation = if (wDp > hDp) ScreenOrientation.LANDSCAPE else ScreenOrientation.PORTRAIT,
        )
    }
}
