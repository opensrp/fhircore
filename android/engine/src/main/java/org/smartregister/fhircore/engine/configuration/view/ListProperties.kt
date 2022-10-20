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
import org.smartregister.fhircore.engine.configuration.register.RegisterCardConfig
import org.smartregister.fhircore.engine.domain.model.ExtractedResource
import org.smartregister.fhircore.engine.domain.model.ViewType

@Serializable
data class ListProperties(
  override val viewType: ViewType,
  val baseResource: String,
  val relatedResources: List<ExtractedResource> = emptyList(),
  val registerCard: RegisterCardConfig,
  val showDivider: Boolean = true,
  val padding: Int = 0,
  val backgroundColor: String = "#FFFFFF"
) : ViewProperties()
