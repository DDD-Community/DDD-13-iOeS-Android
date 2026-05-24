package com.pickflow.android.core.services.protocols

interface OnboardingCompletionStore {
    suspend fun isCompleted(): Boolean
    suspend fun markCompleted()
}
