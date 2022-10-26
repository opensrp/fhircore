/*
 * Copyright 2021 Ona Systems, Inc
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

import androidx.compose.foundation.layout.Arrangement
import kotlinx.serialization.Serializable
import org.smartregister.fhircore.engine.domain.model.ViewType

@Serializable
class ViewGroupProperties(
  override val viewType: ViewType,
  val backgroundColor: String? = null,
  val padding: Int = 0,
  val fillMaxWidth: Boolean = false,
  val fillMaxHeight: Boolean = false,
  val fillMaxSize: Boolean = false,
  val verticalArrangement: VerticalViewArrangement? = null,
  val horizontalArrangement: HorizontalViewArrangement? = null,
  val children: List<ViewProperties> = emptyList(),
  val wrapContent: Boolean = false,
  val borderRadius: Int = 2
) : ViewProperties()

enum class VerticalViewArrangement(val position: Arrangement.Vertical) {
  SPACE_BETWEEN(Arrangement.SpaceBetween),
  SPACE_AROUND(Arrangement.SpaceAround),
  SPACE_EVENLY(Arrangement.SpaceEvenly),
  CENTER(Arrangement.Center),
  TOP(Arrangement.Top),
  BOTTOM(Arrangement.Bottom)
}

enum class HorizontalViewArrangement(val position: Arrangement.Horizontal) {
  SPACE_BETWEEN(Arrangement.SpaceBetween),
  SPACE_AROUND(Arrangement.SpaceAround),
  SPACE_EVENLY(Arrangement.SpaceEvenly),
  CENTER(Arrangement.Center),
  START(Arrangement.Start),
  END(Arrangement.End),
}
