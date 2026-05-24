package com.pickflow.android.feature.spotdetail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pickflow.android.common.designsystem.PickflowColors
import com.pickflow.android.common.designsystem.PickflowTypography

/** iOS recordedTime 배지 색 `Color(red:1, green:161/255, blue:0)`. */
private val RecordedTimeColor = Color(0xFFFFA100)

/**
 * iOS `SpotPhotoSection` 1:1 이식 — 사진 박스 + 주소 행.
 *
 * 사진은 사용자 업로드 에셋 자리라 gray90 박스로 자리만 잡는다.
 */
@Composable
fun SpotPhotoSection(spot: SpotDetailData, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(PickflowColors.gray90),
        ) {
            if (spot.hasImage) {
                Text(
                    text = spot.recordedTime,
                    style = PickflowTypography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = RecordedTimeColor,
                    modifier = Modifier
                        .padding(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.5f))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.LocationOn,
                contentDescription = null,
                tint = PickflowColors.gray50,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = spot.address,
                style = PickflowTypography.labelMedium,
                color = PickflowColors.gray30,
                maxLines = 1,
            )
            Spacer(Modifier.weight(1f))
        }
    }
}
