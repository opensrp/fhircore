/*
 * Copyright 2021 Ona Systems, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.smartregister.fhircore.engine.ui.theme

import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.ui.graphics.Color

val DividerColor = Color.LightGray
val SubtitleTextColor = Color(0xFF7A7A7A)
val LighterGreyColor = Color(0xFFAEAEAE)
val SuccessColor = Color(0xFF1DB11B)
val OverdueColor = Color(0xFFFF333F)
val OverdueLightColor = Color(0xFFF9CFD1)
val WarningColor = Color(0xFFFFA500)
val DueColor = Color(0xFF0075EB)
val DueLightColor = Color(0xFFC1E1EC)

private val PrimaryColor = Color(0xFF005084)
private val PrimaryVariantColor = Color(0xFF003D66)
private val ErrorColor = Color(0xFFDD0000)

val LightColors =
  lightColors(primary = PrimaryColor, primaryVariant = PrimaryVariantColor, error = ErrorColor)

val DarkColors =
  darkColors(primary = PrimaryColor, primaryVariant = PrimaryVariantColor, error = ErrorColor)
