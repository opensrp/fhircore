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

val DividerColor = Color(0xFFdddddd)
val SubtitleTextColor = Color(0xFF7A7A7A)
val GreyTextColor = Color(0xFF5A5A5A)
val SuccessColor = Color(0xFF1DB11B)
val OverdueColor = Color(0xFFFF333F)
val DangerColor = Color(0xFFFF333F)
val InfoColor = Color(0xFF006EB8)
val DefaultColor = Color(0xFF6F7274)
val OverdueDarkRedColor = Color(0xFFDF0E1A)
val OverdueLightColor = Color(0xFFF9CFD1)
val WarningColor = Color(0xFFFFA500)
val LoginBackgroundColor = Color(0xFF091D2B)
val LoginDarkColor = Color(0xFF272727)
val LoginFieldBackgroundColor = Color(0xFF273844)
val LoginButtonColor = Color(0xFF006EB8)
val BlueTextColor = Color(0xFF006EB8)
val DueLightColor = Color(0xFFC1E1EC)
val LighterBlue = Color(0xFFE0F0FF)
val ProgressBarBlueColor = Color(0xFF0075EB)
val SideMenuDarkColor = Color(0xFF2C2C2C)
val SideMenuTopItemDarkColor = Color(0xFF242424)
val SideMenuBottomItemDarkColor = Color(0xFF404040)
val AppTitleColor = Color(0xFF929496)
val StatusTextColor = Color(0xFF6F7274)
val PersonalDataBackgroundColor = Color(0xFFF5F5F5)
val PatientProfileSectionsBackgroundColor = Color(0xFFF2F4F7)
val MenuActionButtonTextColor = Color(0xFF28B8F9)
val MenuItemColor = Color(0xFFBFBFBF)

private val PrimaryColor = Color(0xFF005084)
private val PrimaryVariantColor = Color(0xFF003D66)
private val ErrorColor = Color(0xFFDD0000)

val LightColors =
  lightColors(primary = PrimaryColor, primaryVariant = PrimaryVariantColor, error = ErrorColor)

val DarkColors =
  darkColors(primary = PrimaryColor, primaryVariant = PrimaryVariantColor, error = ErrorColor)
