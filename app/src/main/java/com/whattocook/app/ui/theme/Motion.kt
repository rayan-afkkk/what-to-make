package com.whattocook.app.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring

/** Shared motion tokens (Material 3 easing curves) so every transition feels the same. */
object Motion {
    /** Most UI movement: quick start, long gentle settle. */
    val Emphasized: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    /** Things arriving on screen. */
    val EmphasizedDecelerate: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

    /** Things leaving the screen. */
    val EmphasizedAccelerate: Easing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

    const val SHORT = 150
    const val MEDIUM = 300
    const val LONG = 400

    /** Soft, non-bouncy spring for press feedback and small UI changes. */
    fun <T> gentleSpring() = spring<T>(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)
}
