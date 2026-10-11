package com.whattocook.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

object Radii {
    val small = 12.dp
    val medium = 16.dp
    val large = 18.dp
    val xLarge = 20.dp
}

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(Radii.small),
    medium = RoundedCornerShape(Radii.medium),
    large = RoundedCornerShape(Radii.large),
    extraLarge = RoundedCornerShape(Radii.xLarge),
)

/** Spacing scale used across screens. */
object Spacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
    /** Horizontal screen gutter. */
    val screen = 18.dp
}
