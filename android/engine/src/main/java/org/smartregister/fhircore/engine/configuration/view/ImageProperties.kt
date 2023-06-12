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

import kotlinx.serialization.Serializable
import org.smartregister.fhircore.engine.configuration.navigation.ImageConfig
import org.smartregister.fhircore.engine.domain.model.ViewType
import org.smartregister.fhircore.engine.util.extension.interpolate

@Serializable
data class ImageProperties(
  override val viewType: ViewType = ViewType.IMAGE,
  override val weight: Float = 0f,
  override val backgroundColor: String? = null,
  override val padding: Int = 0,
  override val borderRadius: Int = 2,
  override val alignment: ViewAlignment = ViewAlignment.NONE,
  override val fillMaxWidth: Boolean = false,
  override val fillMaxHeight: Boolean = false,
  override val clickable: String = "false",
  override val visible: String = "true",
  val tint: String? = null,
  val imageConfig: ImageConfig? = null,
  val size: Int? = null
) : ViewProperties() {
  override fun interpolate(computedValuesMap: Map<String, Any>): ViewProperties {
    val imageConfigReference = imageConfig?.reference?.interpolate(computedValuesMap)
    imageConfig?.reference = imageConfigReference
    return this.copy(imageConfig = imageConfig, tint = this.tint?.interpolate(computedValuesMap))
  }
}
