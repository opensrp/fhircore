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
import androidx.compose.foundation.layout.Arrangement
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import org.smartregister.fhircore.engine.domain.model.ActionConfig
import org.smartregister.fhircore.engine.domain.model.ViewType
import org.smartregister.fhircore.engine.util.extension.interpolate

@Serializable
@Parcelize
data class RowProperties(
  override val viewType: ViewType,
  override val weight: Float = 0f,
  override val backgroundColor: String? = null,
  override val padding: Int = 0,
  override val borderRadius: Int = 0,
  override val alignment: ViewAlignment = ViewAlignment.NONE,
  override val fillMaxWidth: Boolean = false,
  override val fillMaxHeight: Boolean = false,
  override val clickable: String = "false",
  override val visible: String = "true",
  override val opacity: Float? = null,
  val spacedBy: Int = 8,
  val arrangement: RowArrangement? = null,
  val wrapContent: Boolean = false,
  val children: List<ViewProperties> = emptyList(),
  val actions: List<ActionConfig> = emptyList(),
) : ViewProperties(), Parcelable {
  override fun interpolate(computedValuesMap: Map<String, Any>): RowProperties {
    return this.copy(
      backgroundColor = backgroundColor?.interpolate(computedValuesMap),
      visible = visible.interpolate(computedValuesMap),
      clickable = clickable.interpolate(computedValuesMap),
    )
  }
}

enum class RowArrangement(val position: Arrangement.Horizontal) {
  SPACE_BETWEEN(Arrangement.SpaceBetween),
  SPACE_AROUND(Arrangement.SpaceAround),
  SPACE_EVENLY(Arrangement.SpaceEvenly),
  CENTER(Arrangement.Center),
  START(Arrangement.Start),
  END(Arrangement.End),
}
