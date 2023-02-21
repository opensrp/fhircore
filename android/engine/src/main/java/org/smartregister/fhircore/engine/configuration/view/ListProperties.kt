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
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.configuration.register.NoResultsConfig
import org.smartregister.fhircore.engine.configuration.register.RegisterCardConfig
import org.smartregister.fhircore.engine.domain.model.ExtractedResource
import org.smartregister.fhircore.engine.domain.model.ViewType

@Serializable
data class ListProperties(
  override val viewType: ViewType,
  override val weight: Float = 0f,
  override val backgroundColor: String? = "#FFFFFF",
  override val padding: Int = 0,
  override val borderRadius: Int = 2,
  override val alignment: ViewAlignment = ViewAlignment.NONE,
  override val fillMaxWidth: Boolean = false,
  override val fillMaxHeight: Boolean = false,
  override val clickable: String = "false",
  override val visible: String = "true",
  val id: String = "listId",
  val registerCard: RegisterCardConfig,
  val showDivider: Boolean = true,
  val emptyList: NoResultsConfig? = null,
  val orientation: ListOrientation = ListOrientation.VERTICAL,
  val resources: List<ListResource> = emptyList(),
  val practitionerList: List<PractitionerConfig> = emptyList()
) : ViewProperties()

enum class ListOrientation {
  VERTICAL,
  HORIZONTAL
}

@Serializable
data class ListResource(
  val id: String,
  val relatedResourceId: String? = null,
  val resourceType: ResourceType,
  val conditionalFhirPathExpression: String? = null,
  val practitionerFhirPathExpression: String? = null,
  val relatedResources: List<ExtractedResource> = emptyList()
)

@Serializable
data class PractitionerConfig(
  val filterByPractitionerId:Boolean?=false,
  val practitionerFhirPathExpression: String? = null
)
