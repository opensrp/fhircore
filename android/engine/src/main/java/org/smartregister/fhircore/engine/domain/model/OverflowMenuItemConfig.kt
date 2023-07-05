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

package org.smartregister.fhircore.engine.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import org.smartregister.fhircore.engine.configuration.navigation.ImageConfig
import org.smartregister.fhircore.engine.util.extension.BLACK_COLOR_HEX_CODE
import org.smartregister.fhircore.engine.util.extension.TRUE
import org.smartregister.fhircore.engine.util.extension.interpolate

@Serializable
@Parcelize
data class OverflowMenuItemConfig(
  val id: Int = 1,
  val title: String = "",
  val confirmAction: Boolean = false,
  val icon: ImageConfig? = null,
  val titleColor: String = BLACK_COLOR_HEX_CODE,
  val backgroundColor: String? = null,
  val visible: String,
  val showSeparator: Boolean = false,
  val enabled: String = TRUE,
  val actions: List<ActionConfig> = emptyList()
) : Parcelable, java.io.Serializable {
  fun interpolate(computedValuesMap: Map<String, Any>): OverflowMenuItemConfig {
    return this.copy(
      title = title.interpolate(computedValuesMap),
      enabled = enabled.interpolate(computedValuesMap),
      visible = visible.interpolate(computedValuesMap),
      titleColor = titleColor.interpolate(computedValuesMap),
      backgroundColor = backgroundColor?.interpolate(computedValuesMap),
      icon = icon?.interpolate(computedValuesMap)
    )
  }
}
