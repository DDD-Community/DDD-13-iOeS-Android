package com.pickflow.android.core.network.dto.spot

import kotlinx.serialization.Serializable

@Serializable
data class SpotDetailResponseDto(
    val spotId: Long = 0L,
    val name: String = "",
    val comment: String = "",
    val theme: String = "SUNSET",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val address: String = "",
    val addressRoad: String? = null,
    val addressJibun: String? = null,
    val imageUrl: String? = null,
    val recordedDate: String = "",
    val recordedTime: String = "",
    val weatherSky: String? = null,
    val precipitation: String? = null,
    val precipitationProbability: Int = 0,
    val congestionLevel: String? = null,
    val sunsetTime: String? = null,
    val astronomyDate: String? = null,
    val weatherUpdatedAt: String? = null,
    val congestionUpdatedAt: String? = null,
    val parkingInfo: String? = null,
    val bookmarkCount: Long = 0L,
    val isBookmarked: Boolean = false,
    val isMySpot: Boolean = false,
    /** 유저스팟 공개 상태(PUBLISHED 등). 현재 화면에서 쓰지 않고 수신만 한다. */
    val status: String = "",
    val isCurated: Boolean = false,
    val likeCount: Long = 0L,
    val isLiked: Boolean = false,
    /** 추천 버튼 노출 여부. 내 스팟 등 추천 불가 대상은 false. */
    val isLikeable: Boolean = false,
    // NOTE: 응답에 rejection 필드가 있으나 항상 null 로만 관측돼 형태를 알 수 없다.
    // 잘못 선언하면 파싱이 깨지므로 스펙 확인 전까지 ignoreUnknownKeys 로 흘려보낸다.
)
