/*
 * Copyright 2021-2023 Ona Systems, Inc
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

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * TODO fix issue with ktfmt formatting annotated high order functions. Current workaround below:
 * lambda in this format content: (@Composable() () -> Unit) to allow spotlessApply
 *
 * To enable app theme set darkTheme = isSystemInDarkTheme
 */
//@Composable
//fun AppTheme(darkTheme: Boolean = false, content: (@Composable() () -> Unit)) {
//  MaterialTheme(colors = if (darkTheme) DarkColors else LightColors, content = content)
//}

class ThemeViewModel : ViewModel() {
  var isSystemInDarkTheme by mutableStateOf(false)
    private set

  fun updateTheme(isDarkTheme: Boolean) {
    isSystemInDarkTheme = isDarkTheme
    val theme = if (isSystemInDarkTheme) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
    AppCompatDelegate.setDefaultNightMode(theme)
  }
}

@Composable
fun AppTheme(content: @Composable () -> Unit) {
  val themeViewModel: ThemeViewModel = viewModel()

  // Set the app's theme based on the system theme
  val theme = if (themeViewModel.isSystemInDarkTheme) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
  AppCompatDelegate.setDefaultNightMode(theme)

  CompositionLocalProvider(LocalThemeViewModel provides themeViewModel) {
    content()
  }
}

val LocalThemeViewModel = staticCompositionLocalOf<ThemeViewModel> {
  error("No ThemeViewModel provided")
}

@Composable
fun ChildComposable() {
  val themeViewModel = LocalThemeViewModel.current

  // Access and observe the system theme state from the ThemeViewModel

  val isSystemInDarkTheme by themeViewModel::isSystemInDarkTheme

  // Use the system theme state in your composables
  // ...
}