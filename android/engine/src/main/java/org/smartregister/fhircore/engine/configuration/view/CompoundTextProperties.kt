/*
 * Copyright 2021-2024 Ona Systems, Inc
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

import android.os.Parcelable
import androidx.compose.ui.text.font.FontWeight
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import org.smartregister.fhircore.engine.domain.model.ActionConfig
import org.smartregister.fhircore.engine.domain.model.ViewType
import org.smartregister.fhircore.engine.util.extension.interpolate

@Serializable
@Parcelize
data class CompoundTextProperties(
  override val viewType: ViewType = ViewType.COMPOUND_TEXT,
  override val weight: Float = 0f,
  override val backgroundColor: String? = null,
  override val padding: Int = 0,
  override val borderRadius: Int = 2,
  override val alignment: ViewAlignment = ViewAlignment.NONE,
  override val fillMaxWidth: Boolean = false,
  override val fillMaxHeight: Boolean = false,
  override val opacity: Float? = null,
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
  val textCase: TextCase? = null,
  val overflow: TextOverFlow? = null,
  val letterSpacing: Int = 0,
) : ViewProperties(), Parcelable {
  override fun interpolate(computedValuesMap: Map<String, Any>): CompoundTextProperties {
    return this.copy(
      backgroundColor = backgroundColor?.interpolate(computedValuesMap),
      visible = visible.interpolate(computedValuesMap),
      primaryText = primaryText?.interpolate(computedValuesMap),
      secondaryText = secondaryText?.interpolate(computedValuesMap),
      primaryTextColor = primaryTextColor?.interpolate(computedValuesMap),
      primaryTextBackgroundColor = primaryTextBackgroundColor?.interpolate(computedValuesMap),
      secondaryTextColor = secondaryTextColor?.interpolate(computedValuesMap),
      secondaryTextBackgroundColor = secondaryTextBackgroundColor?.interpolate(computedValuesMap),
      separator = separator?.interpolate(computedValuesMap),
    )
  }
}

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
  TITLE_CASE,
}

enum class TextOverFlow {
  CLIP,
  ELLIPSIS,
  VISIBLE,
}
