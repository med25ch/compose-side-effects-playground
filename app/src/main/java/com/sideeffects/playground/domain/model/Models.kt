package com.sideeffects.playground.domain.model

data class User(
    val id: String,
    val name: String,
    val email: String
)

sealed class LoadState<out T> {
    data object Idle : LoadState<Nothing>()
    data object Loading : LoadState<Nothing>()
    data class Success<T>(val data: T) : LoadState<T>()
    data class Error(val message: String) : LoadState<Nothing>()
}

data class TimerState(
    val seconds: Int = 0,
    val isRunning: Boolean = false
)

data class ScrollInfo(
    val firstVisibleIndex: Int = 0,
    val offset: Int = 0,
    val isScrollingUp: Boolean = false
)
