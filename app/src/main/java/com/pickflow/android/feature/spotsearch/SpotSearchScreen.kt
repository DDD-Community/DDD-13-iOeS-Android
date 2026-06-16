package com.pickflow.android.feature.spotsearch

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pickflow.android.common.designsystem.PickflowColors
import com.pickflow.android.common.designsystem.PickflowTypography
import com.pickflow.android.common.ui.LoadState
import com.pickflow.android.core.services.protocols.AddressSuggestion

/**
 * iOS `SpotSearchView` 1:1 — 헤더 + 검색바 + (idle/empty 플레이스홀더 | 결과 리스트).
 * 결과 탭 시 [onSelectResult] → 장소 상세(핀 조정)로 이동.
 */
@Composable
fun SpotSearchScreen(
    onBack: () -> Unit,
    onSelectResult: (AddressSuggestion) -> Unit,
    viewModel: SpotSearchViewModel = hiltViewModel(),
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val suggestions by viewModel.suggestions.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PickflowColors.gray95)
            .testTag("spotsearch-screen"),
    ) {
        // 헤더
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(PickflowColors.gray95)
                .statusBarsPadding()
                .padding(horizontal = 16.dp)
                .height(48.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clickable(onClick = onBack)
                    .testTag("search-back"),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로가기", tint = PickflowColors.gray0)
            }
            Text(
                text = "장소 검색",
                style = PickflowTypography.headingMedium,
                color = PickflowColors.gray0,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.size(44.dp))
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp),
        ) {
            SearchBar(value = query, onValueChange = viewModel::onQueryChanged)
            Spacer(Modifier.height(28.dp))

            when (val state = suggestions) {
                LoadState.Idle -> EmptyPlaceholder("당신이 발견한 일상 속 반짝임,\n어디인가요?")
                LoadState.Loading -> Box(Modifier.fillMaxWidth().padding(top = 120.dp), Alignment.Center) {
                    CircularProgressIndicator(color = PickflowColors.gray0, strokeWidth = 2.dp)
                }
                LoadState.Empty -> EmptyPlaceholder("검색 결과가 없어요.\n다른 장소를 검색해 보세요.")
                is LoadState.Failed -> EmptyPlaceholder("검색 중 오류가 발생했어요.\n다시 시도해 주세요.")
                is LoadState.Loaded -> LazyColumn(modifier = Modifier.testTag("search-results")) {
                    items(state.value) { item ->
                        ResultItem(
                            item = item,
                            distanceText = viewModel.distanceText(item),
                            onClick = { onSelectResult(item) },
                        )
                        HorizontalDivider(color = PickflowColors.gray80)
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchBar(value: String, onValueChange: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(PickflowColors.spotInputBackground)
            .padding(horizontal = 14.dp)
            .testTag("search-field"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(Icons.Filled.Search, contentDescription = null, tint = PickflowColors.gray50, modifier = Modifier.size(20.dp))
        Box(modifier = Modifier.weight(1f)) {
            if (value.isEmpty()) {
                Text("장소를 검색해 보세요", style = PickflowTypography.bodyMedium, color = PickflowColors.spotSecondaryText)
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(color = PickflowColors.gray0, fontSize = PickflowTypography.bodyMedium.fontSize),
                cursorBrush = SolidColor(PickflowColors.spotOrange),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ResultItem(item: AddressSuggestion, distanceText: String?, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = item.name,
                style = PickflowTypography.bodyLargeBold,
                color = PickflowColors.gray0,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = item.fullAddress,
                style = PickflowTypography.bodySmall,
                color = PickflowColors.spotTertiaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (distanceText != null) {
            Text(distanceText, style = PickflowTypography.labelMedium, color = PickflowColors.gray30)
        }
    }
}

@Composable
private fun EmptyPlaceholder(message: String) {
    Box(modifier = Modifier.fillMaxWidth().padding(top = 100.dp), contentAlignment = Alignment.Center) {
        Text(
            text = message,
            style = PickflowTypography.bodyMedium,
            color = PickflowColors.gray40,
            textAlign = TextAlign.Center,
        )
    }
}
