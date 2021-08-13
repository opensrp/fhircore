package org.smartregister.fhircore.engine.ui.theme

import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.ui.graphics.Color

private val PrimaryColor = Color(0xFF005084)
private val PrimaryDarkColor = Color(0xFF003D66)
private val ErrorColor = Color(0xFFDD0000)

val LightColors = lightColors(
    primary = PrimaryColor,
    error = ErrorColor
)

val DarkColors = darkColors(
    primary = PrimaryColor,
    error = ErrorColor
)