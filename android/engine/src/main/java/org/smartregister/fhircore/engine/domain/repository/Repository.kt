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

package org.smartregister.fhircore.engine.domain.repository

import org.smartregister.fhircore.engine.domain.model.ActionParameter
import org.smartregister.fhircore.engine.domain.model.FhirResourceConfig
import org.smartregister.fhircore.engine.domain.model.ResourceData

/** This class provides common functionalities used in the register */
interface Repository {

  /**
   * This function loads the desired register configuration using the provided [registerId]. The
   * data query extracted from the retrieved configuration is used to filter the register data (FHIR
   * resources wrapped in [ResourceData]
   */
  suspend fun loadRegisterData(currentPage: Int, registerId: String): List<ResourceData>

  /**
   * This function uses the provided [registerId] to retrieve the register configuration from the
   * registry, then proceeds to count the data based on the configured query parameters
   */
  suspend fun countRegisterData(registerId: String): Long

  /**
   * This function returns data used on the profile for the given [resourceId]. Profile
   * configuration is identified by the [profileId] and contains the queries for filtering the
   * profile data. Data is loaded based on the [FhirResourceConfig]. When none is provided the
   * configurations identified by the [profileId] are used.
   */
  suspend fun loadProfileData(
    profileId: String,
    resourceId: String,
    fhirResourceConfig: FhirResourceConfig? = null,
    params: Array<ActionParameter>?
  ): ResourceData?
}
