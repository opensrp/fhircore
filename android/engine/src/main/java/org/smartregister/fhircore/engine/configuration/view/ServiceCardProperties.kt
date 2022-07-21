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
import org.smartregister.fhircore.engine.configuration.QuestionnaireConfig
import org.smartregister.fhircore.engine.domain.model.ServiceStatus
import org.smartregister.fhircore.engine.domain.model.ViewType

@Serializable
data class ServiceCardProperties(
  override val viewType: ViewType = ViewType.SERVICE_CARD,
  val details: List<CompoundTextProperties> = emptyList(),
  val showVerticalDivider: Boolean = false,
  val serviceMemberIcons: String? = null,
  val serviceButton: ServiceButton? = null
) : RegisterCardViewProperties()

@Serializable
data class ServiceButton(
  val visible: Boolean? = null,
  val text: String? = null,
  val status: String = ServiceStatus.UPCOMING.name,
  val smallSized: Boolean = false,
  val questionnaire: QuestionnaireConfig? = null
)
