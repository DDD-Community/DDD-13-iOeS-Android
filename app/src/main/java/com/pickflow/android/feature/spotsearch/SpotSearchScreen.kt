package com.pickflow.android.feature.spotsearch

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pickflow.android.common.designsystem.PickflowColors
import com.pickflow.android.common.designsystem.PickflowTypography
import com.pickflow.android.common.ui.LoadStateContent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpotSearchScreen(
    onBack: () -> Unit,
    viewModel: SpotSearchViewModel = hiltViewModel(),
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val suggestions by viewModel.suggestions.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = PickflowColors.gray95,
        topBar = {
            TopAppBar(
                title = { Text("장소 검색", style = PickflowTypography.bodyLargeBold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("search-back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PickflowColors.gray95,
                    titleContentColor = PickflowColors.gray0,
                    navigationIconContentColor = PickflowColors.gray0,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("spotsearch-screen"),
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = viewModel::onQueryChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .testTag("search-field"),
                placeholder = { Text("주소 또는 장소를 입력하세요") },
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = PickflowColors.gray0,
                    unfocusedTextColor = PickflowColors.gray0,
                    focusedContainerColor = PickflowColors.gray90,
                    unfocusedContainerColor = PickflowColors.gray90,
                    focusedBorderColor = PickflowColors.sunsetOrange,
                    unfocusedBorderColor = PickflowColors.gray70,
                ),
            )
            LoadStateContent(
                state = suggestions,
                emptyMessage = "검색 결과가 없어요.",
            ) { list ->
                LazyColumn(modifier = Modifier.testTag("search-results")) {
                    items(list) { item ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { }
                                .background(PickflowColors.gray95)
                                .padding(horizontal = 20.dp, vertical = 14.dp),
                        ) {
                            Text(
                                text = item.displayName,
                                style = PickflowTypography.bodyMedium,
                                color = PickflowColors.gray0,
                            )
                            Text(
                                text = "${item.latitude}, ${item.longitude}",
                                style = PickflowTypography.labelSmall,
                                color = PickflowColors.gray40,
                            )
                        }
                        HorizontalDivider(color = PickflowColors.gray80)
                    }
                }
            }
        }
    }
}
