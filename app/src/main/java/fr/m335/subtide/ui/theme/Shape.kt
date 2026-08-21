package fr.m335.subtide.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** Radius = 0 everywhere — the signature brutalist detail of the style. */
private val Zero = RoundedCornerShape(0.dp)

val SubwaveShapes = Shapes(
    extraSmall = Zero,
    small = Zero,
    medium = Zero,
    large = Zero,
    extraLarge = Zero,
)
