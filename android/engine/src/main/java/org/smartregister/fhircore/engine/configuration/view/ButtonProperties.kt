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

import kotlinx.serialization.Serializable
import org.smartregister.fhircore.engine.domain.model.ActionConfig
import org.smartregister.fhircore.engine.domain.model.ViewType

@Serializable
data class ButtonProperties(
  override val viewType: ViewType = ViewType.BUTTON,
  override val weight: Float = 0f,
  override val backgroundColor: String? = null,
  override val padding: Int = 0,
  override val borderRadius: Int = 2,
  override val alignment: ViewAlignment = ViewAlignment.NONE,
  override val fillMaxWidth: Boolean = true,
  override val fillMaxHeight: Boolean = false,
  override val clickable: String = "false",
  override val visible: String = "true",
  val enabled: String = "true",
  val text: String? = null,
  val status: String,
  val smallSized: Boolean = false,
  val fontSize: Float = 14.0f,
  val actions: List<ActionConfig> = emptyList(),
  val buttonType: ButtonType = ButtonType.MEDIUM,
) : ViewProperties()

enum class ButtonType {
  TINY,
  MEDIUM,
  BIG
}
