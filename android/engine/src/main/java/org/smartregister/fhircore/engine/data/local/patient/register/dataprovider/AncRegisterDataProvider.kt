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

package org.smartregister.fhircore.engine.data.local.patient.register.dataprovider

import com.google.android.fhir.FhirEngine
import javax.inject.Inject
import javax.inject.Singleton
import org.smartregister.fhircore.engine.domain.model.PatientProfileData
import org.smartregister.fhircore.engine.domain.model.RegisterRow
import org.smartregister.fhircore.engine.domain.repository.RegisterDataProvider

@Singleton
class AncRegisterDataProvider @Inject constructor(val fhirEngine: FhirEngine) :
  RegisterDataProvider {

  override suspend fun provideRegisterData(
    currentPage: Int,
    loadAll: Boolean,
    appFeatureName: String?
  ): List<RegisterRow> {
    return emptyList()
  }

  override suspend fun provideRegisterDataCount(appFeatureName: String?): Long {
    // TODO("Return count for Anc register clients")
    return 0
  }

  override suspend fun provideProfileData(
    appFeatureName: String?,
    patientId: String
  ): PatientProfileData? {
    return null
  }
}
