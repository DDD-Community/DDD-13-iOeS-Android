package com.pickflow.android.core.network.dto.appversion

import com.pickflow.android.core.services.protocols.AppVersionPolicy
import com.pickflow.android.core.services.protocols.TermsPolicy
import kotlinx.serialization.Serializable

@Serializable
data class AppVersionPolicyDto(
    val minimumVersion: String = "",
    val latestVersion: String = "",
    val forceUpdate: Boolean = false,
    val storeUrl: String = "",
    val supportEmail: String? = null,
    val termsPolicies: List<TermsPolicyDto>? = null,
)

@Serializable
data class TermsPolicyDto(
    val type: String = "",
    val title: String = "",
    val url: String = "",
)

fun AppVersionPolicyDto.toDomain(): AppVersionPolicy = AppVersionPolicy(
    minimumVersion = minimumVersion,
    latestVersion = latestVersion,
    forceUpdate = forceUpdate,
    storeUrl = storeUrl,
    supportEmail = supportEmail,
    termsPolicies = termsPolicies?.map { it.toDomain() },
)

fun TermsPolicyDto.toDomain(): TermsPolicy = TermsPolicy(
    type = type,
    title = title,
    url = url,
)
