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

package org.smartregister.fhircore.anc.data.family

import com.google.android.fhir.FhirEngine
import com.google.android.fhir.search.search
import javax.inject.Inject
import kotlinx.coroutines.withContext
import org.hl7.fhir.r4.model.CarePlan
import org.hl7.fhir.r4.model.Encounter
import org.hl7.fhir.r4.model.Patient
import org.smartregister.fhircore.anc.data.family.model.FamilyMemberItem
import org.smartregister.fhircore.anc.data.patient.PatientRepository
import org.smartregister.fhircore.anc.ui.family.register.FamilyItemMapper
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.util.DispatcherProvider

class FamilyDetailRepository
@Inject
constructor(
  override val fhirEngine: FhirEngine,
  val familyItemMapper: FamilyItemMapper,
  override val dispatcherProvider: DispatcherProvider,
  val ancPatientRepository: PatientRepository,
  val familyRepository: FamilyRepository
) : DefaultRepository(fhirEngine, dispatcherProvider) {
  suspend fun fetchDemographics(familyId: String): Patient =
    withContext(dispatcherProvider.io()) { fhirEngine.load(Patient::class.java, familyId) }

  suspend fun fetchFamilyMembers(familyId: String): List<FamilyMemberItem> =
    withContext(dispatcherProvider.io()) { familyRepository.searchFamilyMembers(familyId) }

  suspend fun fetchEncounters(familyId: String): List<Encounter> =
    withContext(dispatcherProvider.io()) {
      fhirEngine.search {
        filter(Encounter.SUBJECT, { value = "Patient/$familyId" })
        from = 0
        count = 3
      }
    }

  suspend fun fetchFamilyCarePlans(familyId: String): List<CarePlan> =
    withContext(dispatcherProvider.io()) { ancPatientRepository.searchCarePlan(familyId) }
}
