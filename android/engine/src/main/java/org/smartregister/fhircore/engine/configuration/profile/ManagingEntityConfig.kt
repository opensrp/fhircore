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

package org.smartregister.fhircore.engine.configuration.profile

import kotlinx.serialization.Serializable
import org.hl7.fhir.r4.model.ResourceType

/**
 * @property nameFhirPathExpression FHIRPath expression used to extract the name of the
 * managingEntity Example: An expression to extract the given name from the Patient resource (
 *
 * @property eligibilityCriteriaFhirPathExpression A conditional FHIRPath expression used to
 * determine the criteria for being a managing entity
 *
 * Example: An expression for checking whether the age of birth is greater than or equal to 18
 * (Patient.active and (Patient.birthDate <= today() - 18 'years')
 *
 * @property resourceType config for indicating the type of resource used for the ManagingEntity
 * @property dialogTitle The dialog title for selecting managing entity (can be regular or
 * translatable string)
 * @property dialogWarningMessage A warning message displayed on the view for selecting managing
 * entity (can be regular or translatable string)
 *
 * @property dialogContentMessage A message displayed to the user informing them about the action
 * they are about to perform
 */
@Serializable
data class ManagingEntityConfig(
  val nameFhirPathExpression: String? = null,
  val eligibilityCriteriaFhirPathExpression: String? = "",
  val resourceType: ResourceType? = null,
  val dialogTitle: String? = null,
  val dialogWarningMessage: String? = null,
  val dialogContentMessage: String? = null,
  val noMembersErrorMessage: String = "",
  val managingEntityReassignedMessage: String = ""
)
