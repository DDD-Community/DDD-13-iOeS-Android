package com.pickflow.android.common.ui

sealed class LoadState<out T> {
    data object Idle : LoadState<Nothing>()
    data object Loading : LoadState<Nothing>()
    data class Loaded<T>(val value: T) : LoadState<T>()
    data object Empty : LoadState<Nothing>()
    data class Failed(val error: Throwable) : LoadState<Nothing>()
}
