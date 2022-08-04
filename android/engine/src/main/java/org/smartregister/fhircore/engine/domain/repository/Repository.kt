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

package org.smartregister.fhircore.engine.domain.repository

import org.smartregister.fhircore.engine.domain.model.ResourceData

/** Common repository for register. */
interface Repository {
  suspend fun loadRegisterData(currentPage: Int, registerId: String): List<ResourceData>

  /** Return the count for the register content. The register is identified by its [registerId] */
  suspend fun countRegisterData(registerId: String): Long

  /** This function returns data displayed on the users profile */
  suspend fun loadProfileData(profileId: String, resourceId: String): ResourceData?
}
