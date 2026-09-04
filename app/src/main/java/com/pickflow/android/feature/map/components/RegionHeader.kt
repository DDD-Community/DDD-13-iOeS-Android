package com.pickflow.android.feature.map.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.pickflow.android.R
import com.pickflow.android.common.designsystem.PickflowColors
import com.pickflow.android.common.designsystem.PickflowTypography
import com.pickflow.android.core.services.protocols.Region

/**
 * 탐색 탭 헤더의 좌측 브랜드 영역 — `PICKFLOW` 로고 + 지역명 + 펼침 화살표.
 *
 * 지도와 리스트는 같은 화면의 두 모드라 헤더가 **픽셀 단위로 같아야** 한다. 두 화면이
 * 각자 그리면 로고 높이나 지역명 크기가 갈라지므로 여기 하나로 묶어 양쪽이 함께 쓴다.
 *
 * 지역명 타이포는 Figma `Heading/medium`(Pretendard SemiBold 22 / line-height 120% /
 * gray0) = [PickflowTypography.headingMedium].
 */
@Composable
fun RegionHeader(
    region: Region,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(R.drawable.logo),
            contentDescription = "PICKFLOW",
            modifier = Modifier.height(24.dp),
        )
        Row(
            modifier = Modifier
                .padding(start = 6.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onClick)
                .padding(horizontal = 4.dp, vertical = 2.dp)
                .testTag(testTag),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = region.displayName,
                style = PickflowTypography.headingMedium,
                color = PickflowColors.gray0,
            )
            Icon(
                imageVector = Icons.Filled.KeyboardArrowDown,
                contentDescription = "지역 선택",
                tint = PickflowColors.gray0,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
