package com.pickflow.android.feature.myprofile.termsandpolicy

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.pickflow.android.common.designsystem.PickflowColors
import com.pickflow.android.common.designsystem.PickflowTypography

// iOS `TermsAndPolicyView` 1:1 — Notion 공개 페이지는 모바일 UA 에서 임베드 PDF 를
// 파일 블록으로만 노출하므로, 데스크탑 UA 로 요청해 본문(PDF)이 인라인으로 렌더되게 한다.
private const val DESKTOP_USER_AGENT =
    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 " +
        "(KHTML, like Gecko) Version/17.0 Safari/605.1.15"

/** 약관/정책 문서 인앱 웹뷰. iOS 와 동일하게 데스크탑 UA 로 PDF 본문을 인라인 렌더. */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun TermsAndPolicyDocScreen(
    title: String,
    url: String,
    onBack: () -> Unit,
) {
    var isLoading by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PickflowColors.gray95),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(title, style = PickflowTypography.headingSmall, color = PickflowColors.gray0, textAlign = TextAlign.Center)
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "뒤로가기",
                tint = PickflowColors.gray0,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .size(24.dp)
                    .clickable(onClick = onBack),
            )
        }

        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        // Compose AndroidView 안에서 WebView 가 0×0 으로 측정돼 빈 화면이
                        // 보이는 사례가 있어 layoutParams 를 명시한다.
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                        )
                        with(settings) {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            userAgentString = DESKTOP_USER_AGENT
                            // 데스크탑 UA 로 받은 Notion 페이지를 데스크탑 레이아웃 그대로 렌더링.
                            useWideViewPort = true
                            loadWithOverviewMode = true
                            // Notion 이 내부 리소스를 혼합 스킴으로 로드해도 차단하지 않게.
                            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            // Notion 임베드 PDF 가 데스크탑 UA 에서 본문으로 렌더되도록 zoom 허용.
                            builtInZoomControls = true
                            displayZoomControls = false
                        }
                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                isLoading = false
                            }
                            override fun onReceivedError(
                                view: WebView?,
                                request: android.webkit.WebResourceRequest?,
                                error: android.webkit.WebResourceError?,
                            ) {
                                isLoading = false
                            }
                        }
                        loadUrl(url)
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )
            if (isLoading) {
                CircularProgressIndicator(
                    color = PickflowColors.gray0,
                    strokeWidth = 2.dp,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
        }
    }
}
