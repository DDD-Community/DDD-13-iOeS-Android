package com.pickflow.android.feature.withdrawal.model

/** iOS `WithdrawalReason` enum 1:1 이식 — 회원탈퇴 사유 7종. */
enum class WithdrawalReason(val displayText: String) {
    InsufficientSpots("원하는 스팟이 부족해요"),
    DifficultToUse("앱 사용이 어려워요"),
    RarelyUsed("자주 사용하지 않아요"),
    BugsOrIssues("오류나 불편함이 있어요"),
    NewAccount("새 계정을 만들고 싶어요"),
    PrivacyConcerns("개인정보가 걱정돼요"),
    Other("기타"),
}
