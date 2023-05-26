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

package org.smartregister.fhircore.quest.data.patient.model

import org.smartregister.fhircore.engine.appfeature.model.HealthModule
import org.smartregister.fhircore.engine.data.local.RegisterFilter

data class PatientPagingSourceState(
  val healthModule: HealthModule = HealthModule.DEFAULT,
  val appFeatureName: String? = null,
  val currentPage: Int = 0,
  val loadAll: Boolean = false,
  val searchFilter: String? = null,
  val filters: RegisterFilter? = null
) {
  val requiresFilter = filters != null
}
