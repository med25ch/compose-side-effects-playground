package com.sideeffects.playground.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.text.SimpleDateFormat
import java.util.*

class RecompositionCounter {
    var count = 0
        private set

    fun increment(): Int {
        count++
        return count
    }
}

@Composable
fun rememberRecompositionCounter(): RecompositionCounter {
    return remember { RecompositionCounter() }
}

fun currentTime(): String {
    val sdf = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
    return sdf.format(Date())
}

fun currentTimeShort(): String {
    val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    return sdf.format(Date())
}

// Simulates a network delay
suspend fun simulateNetworkCall(delayMs: Long = 2000): Result<String> {
    kotlinx.coroutines.delay(delayMs)
    return if (Math.random() > 0.2) {
        Result.success("Data loaded at ${currentTimeShort()}")
    } else {
        Result.failure(Exception("Network error: timeout"))
    }
}
