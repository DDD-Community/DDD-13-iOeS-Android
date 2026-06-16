package com.pickflow.android.core.services.impl

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import com.pickflow.android.core.services.protocols.ExternalAppLauncher
import dagger.hilt.android.qualifiers.ApplicationContext
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

/**
 * iOS `ExternalAppLauncher` 1:1 — 네이버 지도 앱 길찾기 딥링크.
 *
 * `nmap://route/public?dlat=...&dlng=...&dname=...&appname=<pkg>`
 *  - 앱 설치 → 길찾기 화면으로 점프
 *  - 미설치 → Play Store(market://details?id=com.nhn.android.nmap)로 폴백
 */
@Singleton
class AndroidExternalAppLauncher @Inject constructor(
    @ApplicationContext private val context: Context,
) : ExternalAppLauncher {

    override suspend fun openMap(latitude: Double, longitude: Double, label: String) {
        val encodedName = URLEncoder.encode(label, "UTF-8")
        val appName = context.packageName
        val routeUri = Uri.parse(
            "nmap://route/public?dlat=$latitude&dlng=$longitude&dname=$encodedName&appname=$appName",
        )
        val routeIntent = Intent(Intent.ACTION_VIEW, routeUri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (routeIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(routeIntent)
            return
        }
        // 폴백: Play Store 의 네이버지도 페이지.
        val storeUri = Uri.parse("market://details?id=$NAVER_MAP_PACKAGE")
        val storeIntent = Intent(Intent.ACTION_VIEW, storeUri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (storeIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(storeIntent)
        } else {
            context.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=$NAVER_MAP_PACKAGE"),
                ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) },
            )
        }
    }

    override suspend fun openUrl(url: String) {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
        )
    }

    override suspend fun openCustomTab(url: String) {
        if (url.isBlank()) return
        val intent = CustomTabsIntent.Builder()
            .setShowTitle(true)
            .build()
        intent.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.launchUrl(context, Uri.parse(url))
    }

    override suspend fun dial(phoneNumber: String) {
        context.startActivity(
            Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneNumber")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
        )
    }

    private companion object {
        const val NAVER_MAP_PACKAGE = "com.nhn.android.nmap"
    }
}
