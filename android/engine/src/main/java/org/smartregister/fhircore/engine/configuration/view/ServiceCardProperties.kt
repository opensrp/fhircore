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

import kotlinx.serialization.Serializable
import org.smartregister.fhircore.engine.domain.model.ActionConfig
import org.smartregister.fhircore.engine.domain.model.ViewType

@Serializable
data class ServiceCardProperties(
  override val viewType: ViewType = ViewType.SERVICE_CARD,
  override val weight: Float = 0f,
  override val backgroundColor: String? = "#FFFFFF",
  override val padding: Int = 0,
  override val borderRadius: Int = 2,
  override val alignment: ViewAlignment = ViewAlignment.NONE,
  override val fillMaxWidth: Boolean = false,
  override val fillMaxHeight: Boolean = false,
  val details: List<CompoundTextProperties> = emptyList(),
  val showVerticalDivider: Boolean = false,
  val serviceMemberIcons: String? = null,
  val serviceButton: ButtonProperties? = null,
  val services: List<ButtonProperties>? = null,
  val actions: List<ActionConfig> = emptyList()
) : ViewProperties()
