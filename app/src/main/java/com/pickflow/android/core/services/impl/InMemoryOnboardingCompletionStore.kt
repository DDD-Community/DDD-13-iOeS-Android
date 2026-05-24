package com.pickflow.android.core.services.impl

import com.pickflow.android.core.services.protocols.OnboardingCompletionStore
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InMemoryOnboardingCompletionStore @Inject constructor() : OnboardingCompletionStore {
    private val completed = AtomicBoolean(false)
    override suspend fun isCompleted(): Boolean = completed.get()
    override suspend fun markCompleted() {
        completed.set(true)
    }
}
