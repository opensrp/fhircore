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

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable

/**
 * TODO fix issue with ktfmt formatting annotated high order functions. Current workaround below:
 * lambda in this format content: (@Composable() () -> Unit) to allow spotlessApply
 *
 * To enable app theme set darkTheme = isSystemInDarkTheme
 */
@Composable
fun AppTheme(darkTheme: Boolean = false, content: (@Composable() () -> Unit)) {
  MaterialTheme(colors = if (darkTheme) DarkColors else LightColors, content = content)
}
