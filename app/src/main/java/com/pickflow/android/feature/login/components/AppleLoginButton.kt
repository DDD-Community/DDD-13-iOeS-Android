package com.pickflow.android.feature.login.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.pickflow.android.R
import com.pickflow.android.common.designsystem.PickflowColors
import com.pickflow.android.common.designsystem.PickflowTypography

/**
 * Apple 로그인 CTA 버튼. iOS `AppleLoginButton` 1:1 이식.
 *
 * iOS 원본은 `apple.logo` SF Symbol을 쓰지만, Android에서는
 * `R.drawable.ic_apple` 벡터 드로어블 에셋으로 치환한다.
 */
@Composable
fun AppleLoginButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    enabled: Boolean = true,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .alpha(if (isLoading || !enabled) 0.5f else 1f)
            .clip(RoundedCornerShape(8.dp))
            .background(PickflowColors.gray0)
            .clickable(enabled = enabled && !isLoading, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (isLoading) {
            // Paparazzi는 indeterminate 애니메이션을 0프레임으로 캡처하므로
            // 스냅샷 결정성을 위해 정적 호(arc)로 렌더한다.
            CircularProgressIndicator(
                progress = { 0.75f },
                color = PickflowColors.gray90,
                strokeWidth = 3.dp,
                modifier = Modifier.size(24.dp),
            )
        } else {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_apple),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                )
                Text(
                    text = "Apple로 로그인",
                    style = PickflowTypography.bodyLargeBold,
                    color = PickflowColors.gray90,
                )
            }
        }
    }
}
