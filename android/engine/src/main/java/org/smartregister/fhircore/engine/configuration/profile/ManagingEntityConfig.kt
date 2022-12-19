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

package org.smartregister.fhircore.engine.configuration.profile

import kotlinx.serialization.Serializable
import org.smartregister.fhircore.engine.domain.model.ExtractedResource

/**
 * @property infoFhirPathExpression FHIRPath expression used to extract content from the managing
 * entity resource e.g. the names of the Patient who can be a managing entity
 * @property fhirPathResource config for indicating the type of resource used for the ManagingEntity
 * and the FHIR path expression for filtering the resources eligible for being ManagingEntities e.g.
 * patients of a particular age
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
  val infoFhirPathExpression: String,
  val fhirPathResource: ExtractedResource,
  val dialogTitle: String? = null,
  val dialogWarningMessage: String? = null,
  val dialogContentMessage: String? = null
)
