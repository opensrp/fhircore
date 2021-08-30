package org.smartregister.fhircore.engine.ui.theme

import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.ui.graphics.Color

val DividerColor = Color.LightGray
val SubtitleTextColor = Color(0xFF7A7A7A)
val LighterGreyColor = Color(0xFFAEAEAE)
val SuccessColor = Color(0xFF1DB11B)
val OverdueColor = Color(0xFFFF333F)
val WarningColor = Color(0xFFFFA500)

private val PrimaryColor = Color(0xFF005084)
private val PrimaryVariantColor = Color(0xFF003D66)
private val ErrorColor = Color(0xFFDD0000)

val LightColors =
  lightColors(primary = PrimaryColor, primaryVariant = PrimaryVariantColor, error = ErrorColor)

val DarkColors =
  darkColors(primary = PrimaryColor, primaryVariant = PrimaryVariantColor, error = ErrorColor)
