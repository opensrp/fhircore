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

package org.smartregister.fhircore.engine.domain.model

import kotlinx.serialization.Serializable

/*
 * This class is used to configure the conditions under which a CarePlan and its associated Tasks
 * have their statuses updated.
 *
 * @param fhirPathExpression: A FHIRPath expression that is evaluated against the
 *   QuestionnaireResponse or resource referenced by [fhirPathResourceId] to return a boolean value.
 * @param fhirPathResource: If set, the type of resource to evaluate against.
 * @param fhirPathResourceId: If set, the id of the resource to evaluate against.
 * @param operation: The operation to perform on the CarePlan and its associated Tasks.
 */
@Serializable
data class CarePlanConfig(
  val fhirPathExpression: String? = null,
  val fhirPathResource: String? = null,
  val fhirPathResourceId: String? = null
) : java.io.Serializable
