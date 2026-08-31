package com.pixelquest.app.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally

/**
 * Shared retro pixel-appropriate screen transition specifications.
 * Uses snappy linear slide + crisp fade (150ms) to maintain the 8-bit arcade feel.
 */
object PixelTransitions {
    const val DURATION_NORMAL = 150
    const val DURATION_SNAPPY = 100

    val Enter: EnterTransition = slideInHorizontally(
        initialOffsetX = { fullWidth -> fullWidth / 4 },
        animationSpec = tween(durationMillis = DURATION_NORMAL, easing = LinearEasing)
    ) + fadeIn(
        animationSpec = tween(durationMillis = DURATION_SNAPPY, easing = LinearEasing)
    )

    val Exit: ExitTransition = slideOutHorizontally(
        targetOffsetX = { fullWidth -> -fullWidth / 4 },
        animationSpec = tween(durationMillis = DURATION_NORMAL, easing = LinearEasing)
    ) + fadeOut(
        animationSpec = tween(durationMillis = DURATION_SNAPPY, easing = LinearEasing)
    )

    val PopEnter: EnterTransition = slideInHorizontally(
        initialOffsetX = { fullWidth -> -fullWidth / 4 },
        animationSpec = tween(durationMillis = DURATION_NORMAL, easing = LinearEasing)
    ) + fadeIn(
        animationSpec = tween(durationMillis = DURATION_SNAPPY, easing = LinearEasing)
    )

    val PopExit: ExitTransition = slideOutHorizontally(
        targetOffsetX = { fullWidth -> fullWidth / 4 },
        animationSpec = tween(durationMillis = DURATION_NORMAL, easing = LinearEasing)
    ) + fadeOut(
        animationSpec = tween(durationMillis = DURATION_SNAPPY, easing = LinearEasing)
    )

    val ModalEnter: EnterTransition = androidx.compose.animation.slideInVertically(
        initialOffsetY = { fullHeight -> fullHeight / 3 },
        animationSpec = tween(durationMillis = DURATION_NORMAL, easing = LinearEasing)
    ) + fadeIn(
        animationSpec = tween(durationMillis = DURATION_SNAPPY, easing = LinearEasing)
    )

    val ModalExit: ExitTransition = androidx.compose.animation.slideOutVertically(
        targetOffsetY = { fullHeight -> fullHeight / 3 },
        animationSpec = tween(durationMillis = DURATION_NORMAL, easing = LinearEasing)
    ) + fadeOut(
        animationSpec = tween(durationMillis = DURATION_SNAPPY, easing = LinearEasing)
    )

    val ModalPopEnter: EnterTransition = fadeIn(
        animationSpec = tween(durationMillis = DURATION_SNAPPY, easing = LinearEasing)
    )

    val ModalPopExit: ExitTransition = androidx.compose.animation.slideOutVertically(
        targetOffsetY = { fullHeight -> fullHeight / 3 },
        animationSpec = tween(durationMillis = DURATION_NORMAL, easing = LinearEasing)
    ) + fadeOut(
        animationSpec = tween(durationMillis = DURATION_SNAPPY, easing = LinearEasing)
    )
}
