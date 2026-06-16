package com.pickflow.android.feature.myprofile.termsandpolicy

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pickflow.android.common.designsystem.PickflowColors
import com.pickflow.android.common.designsystem.PickflowTypography
import com.pickflow.android.core.services.protocols.TermsPolicy

/**
 * iOS `TermsAndPolicyListView` 1:1 — 약관/정책 문서 목록.
 * 각 행 탭 시 해당 문서 URL 을 Custom Tab 으로 연다.
 */
@Composable
fun TermsAndPolicyListScreen(
    onBack: () -> Unit,
    viewModel: TermsAndPolicyViewModel = hiltViewModel(),
) {
    val documents by viewModel.documents.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.load() }

    // iOS `navigationDestination(item:)` 대응 — 문서 선택 시 인앱 웹뷰로 전환.
    var selectedDoc by remember { mutableStateOf<TermsPolicy?>(null) }
    selectedDoc?.let { doc ->
        TermsAndPolicyDocScreen(title = doc.title, url = doc.url, onBack = { selectedDoc = null })
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PickflowColors.gray95)
            .testTag("terms-policy-screen"),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "약관 및 정책",
                style = PickflowTypography.headingSmall,
                color = PickflowColors.gray0,
                textAlign = TextAlign.Center,
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "뒤로가기",
                tint = PickflowColors.gray0,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .size(24.dp)
                    .clickable(onClick = onBack)
                    .testTag("terms-policy-back"),
            )
        }

        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
            documents.forEach { document ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clickable { selectedDoc = document }
                        .testTag("terms-policy-row"),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = document.title,
                        style = PickflowTypography.bodyLarge,
                        color = PickflowColors.gray0,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = PickflowColors.gray0,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}
