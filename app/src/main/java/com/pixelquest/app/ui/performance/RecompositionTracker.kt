package com.pixelquest.app.ui.performance

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import com.pixelquest.app.BuildConfig

/**
 * Debug-only recomposition counter helper for performance auditing.
 */
@Composable
fun LogRecomposition(tag: String) {
    if (BuildConfig.DEBUG) {
        val count = remember { intArrayOf(0) }
        SideEffect {
            count[0]++
            println("RecompositionCounter: [$tag] recomposed ${count[0]} times")
        }
    }
}
