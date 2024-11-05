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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import org.smartregister.fhircore.engine.configuration.navigation.ImageConfig
import org.smartregister.fhircore.engine.domain.model.ActionConfig
import org.smartregister.fhircore.engine.domain.model.ViewType
import org.smartregister.fhircore.engine.util.extension.interpolate

@Serializable
@Parcelize
data class ImageProperties(
  override val viewType: ViewType = ViewType.IMAGE,
  override val weight: Float = 0f,
  override val backgroundColor: String? = null,
  override val padding: Int = -1,
  override val borderRadius: Int = 2,
  override val alignment: ViewAlignment = ViewAlignment.NONE,
  override val fillMaxWidth: Boolean = false,
  override val fillMaxHeight: Boolean = false,
  override val clickable: String = "false",
  override val visible: String = "true",
  override val opacity: Float? = null,
  val tint: String? = null,
  val text: String? = null,
  val imageConfig: ImageConfig? = null,
  val size: Int? = 22,
  val shape: ImageShape? = null,
  val textColor: String? = null,
  val actions: List<ActionConfig> = emptyList(),
) : ViewProperties(), Parcelable {
  override fun interpolate(computedValuesMap: Map<String, Any>): ViewProperties {
    return this.copy(
      visible = visible.interpolate(computedValuesMap),
      imageConfig =
        imageConfig?.copy(
          reference = imageConfig.reference?.interpolate(computedValuesMap),
          type = imageConfig.type.interpolate(computedValuesMap),
        ),
      tint = this.tint?.interpolate(computedValuesMap),
      backgroundColor = this.backgroundColor?.interpolate(computedValuesMap),
      text = this.text?.interpolate(computedValuesMap),
    )
  }
}

enum class ImageShape(val composeShape: Shape) {
  CIRCLE(CircleShape),
  RECTANGLE(RectangleShape),
}
