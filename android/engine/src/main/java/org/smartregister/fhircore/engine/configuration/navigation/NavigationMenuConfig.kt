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

package org.smartregister.fhircore.engine.configuration.navigation

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import org.smartregister.fhircore.engine.domain.model.ActionConfig
import org.smartregister.fhircore.engine.util.extension.interpolate

const val ICON_TYPE_LOCAL = "local"
const val ICON_TYPE_REMOTE = "remote"

@Serializable
@Parcelize
data class NavigationMenuConfig(
  val id: String,
  val visible: Boolean = true,
  val enabled: String = "true",
  val menuIconConfig: ImageConfig? = null,
  val display: String,
  val showCount: Boolean = false,
  val animate: Boolean = true,
  val actions: List<ActionConfig>? = null,
) : Parcelable, java.io.Serializable

@Serializable
@Parcelize
data class ImageConfig(
  var type: String = ICON_TYPE_LOCAL,
  val reference: String? = null,
  val color: String? = null,
  val alpha: Float = 1.0f,
  val imageType: ImageType = ImageType.SVG,
  val contentScale: ContentScaleType = ContentScaleType.FIT,
) : Parcelable, java.io.Serializable {
  fun interpolate(computedValuesMap: Map<String, Any>): ImageConfig {
    return this.copy(
      reference = this.reference?.interpolate(computedValuesMap),
      type = this.type.interpolate(computedValuesMap),
    )
  }
}

enum class ImageType {
  JPEG,
  PNG,
  SVG,
}

enum class ContentScaleType {
  FIT,
  CROP,
  FILLHEIGHT,
  INSIDE,
  NONE,
  FILLBOUNDS,
}
