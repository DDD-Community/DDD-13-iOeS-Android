package com.pickflow.android.feature.spotdetail

/**
 * 상세 화면 하나짜리 토스트 자리에 띄울 내용.
 *
 * 메시지만 있으면 충분했는데 PV-143 에서 **추천 성공/실패만 체크 아이콘 없이** 띄우기로
 * 해서 아이콘 유무가 메시지와 함께 움직여야 했다. 별도 StateFlow 로 쪼개면 둘이
 * 어긋날 수 있어 한 값으로 묶는다.
 */
data class SpotDetailToast(
    val message: String,
    /** 문구 앞 체크 아이콘 표시 여부. 기본은 표시(등록 완료·제보 접수 등). */
    val hasCheckIcon: Boolean = true,
)
