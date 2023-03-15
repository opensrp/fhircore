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

package org.smartregister.fhircore.engine.configuration.view

import androidx.compose.ui.text.font.FontWeight
import kotlinx.serialization.Serializable
import org.smartregister.fhircore.engine.domain.model.ActionConfig
import org.smartregister.fhircore.engine.domain.model.ViewType

@Serializable
data class CompoundTextProperties(
  override val viewType: ViewType = ViewType.COMPOUND_TEXT,
  override val weight: Float = 0f,
  override val backgroundColor: String? = null,
  override val padding: Int = 0,
  override val borderRadius: Int = 2,
  override val alignment: ViewAlignment = ViewAlignment.NONE,
  override val fillMaxWidth: Boolean = false,
  override val fillMaxHeight: Boolean = false,
  override val clickable: String = "false",
  override val visible: String = "true",
  val primaryText: String? = null,
  val primaryTextColor: String? = null,
  val secondaryText: String? = null,
  val secondaryTextColor: String? = null,
  val separator: String? = null,
  val fontSize: Float = 16.0f,
  val primaryTextBackgroundColor: String? = null,
  val secondaryTextBackgroundColor: String? = null,
  val primaryTextFontWeight: TextFontWeight = TextFontWeight.NORMAL,
  val secondaryTextFontWeight: TextFontWeight = TextFontWeight.NORMAL,
  val primaryTextActions: List<ActionConfig> = emptyList(),
  val secondaryTextActions: List<ActionConfig> = emptyList(),
  val maxLines: Int = Int.MAX_VALUE,
  val colorOpacity: Float = 1f,
  val textCase: TextCase? = null
) : ViewProperties()

enum class TextFontWeight(val fontWeight: FontWeight) {
  THIN(FontWeight.Thin),
  BOLD(FontWeight.Bold),
  EXTRA_BOLD(FontWeight.ExtraBold),
  LIGHT(FontWeight.Light),
  MEDIUM(FontWeight.Medium),
  NORMAL(FontWeight.Normal),
  BLACK(FontWeight.Black),
  EXTRA_LIGHT(FontWeight.ExtraLight),
  SEMI_BOLD(FontWeight.SemiBold),
}

enum class TextCase {
  UPPER_CASE,
  LOWER_CASE,
  CAMEL_CASE,
}
