package com.pickflow.android.core.services.protocols

interface OnboardingCompletionStore {
    suspend fun isCompleted(): Boolean

    /** true = 온보딩을 이미 봤음. 개발자 모드에서 false로 되돌려 재노출시킬 수 있다. */
    suspend fun setCompleted(completed: Boolean)
}
