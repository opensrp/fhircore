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

package org.smartregister.fhircore.engine.data.local.register.dao

import com.google.android.fhir.FhirEngine
import com.google.android.fhir.logicalId
import com.google.android.fhir.search.Order
import com.google.android.fhir.search.search
import org.hl7.fhir.r4.model.CarePlan
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Task
import javax.inject.Inject
import javax.inject.Singleton
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.app.AppConfigClassification
import org.smartregister.fhircore.engine.configuration.app.ApplicationConfiguration
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.domain.model.ProfileData
import org.smartregister.fhircore.engine.domain.model.RegisterData
import org.smartregister.fhircore.engine.domain.repository.RegisterDao
import org.smartregister.fhircore.engine.domain.util.PaginationConstant
import org.smartregister.fhircore.engine.util.extension.extractAddress
import org.smartregister.fhircore.engine.util.extension.extractGeneralPractitionerReference
import org.smartregister.fhircore.engine.util.extension.extractHealthStatusFromMeta
import org.smartregister.fhircore.engine.util.extension.extractName
import org.smartregister.fhircore.engine.util.extension.extractOfficialIdentifier
import org.smartregister.fhircore.engine.util.extension.extractTelecom
import org.smartregister.fhircore.engine.util.extension.toAgeDisplay

@Singleton
class HivRegisterDao
@Inject
constructor(
  val fhirEngine: FhirEngine,
  val defaultRepository: DefaultRepository,
  val configurationRegistry: ConfigurationRegistry
) : RegisterDao {

  override suspend fun loadRegisterData(
    currentPage: Int,
    loadAll: Boolean,
    appFeatureName: String?
  ): List<RegisterData> {
    val patients =
      fhirEngine.search<Patient> {
        filter(Patient.ACTIVE, { value = of(true) })
        sort(Patient.NAME, Order.ASCENDING)
        count =
          if (loadAll) countRegisterData(appFeatureName).toInt()
          else PaginationConstant.DEFAULT_PAGE_SIZE
        from = currentPage * PaginationConstant.DEFAULT_PAGE_SIZE
      }

    return patients.filterNot { it.gender == null }.map { patient ->
      RegisterData.HivRegisterData(
          logicalId = patient.logicalId,
          name = patient.extractName(),
          gender = patient.gender,
          age = patient.birthDate.toAgeDisplay(),
          address = patient.extractAddress(),
          familyName = if (patient.hasName()) patient.nameFirstRep.family else null,
          phoneContacts = patient.extractTelecom(),
          chwAssigned = patient.extractGeneralPractitionerReference(),
          healthStatus =
          patient.extractHealthStatusFromMeta(
              getApplicationConfiguration().patientTypeFilterTagViaMetaCodingSystem
          )
      )
    }
  }

  override suspend fun loadProfileData(appFeatureName: String?, resourceId: String): ProfileData {
    val patient = defaultRepository.loadResource<Patient>(resourceId)!!

    return ProfileData.HivProfileData(
      logicalId = patient.logicalId,
      birthdate = patient.birthDate,
      name = patient.extractName(),
      identifier = patient.extractOfficialIdentifier(),
      gender = patient.gender,
      age = patient.birthDate.toAgeDisplay(),
      address = patient.extractAddress(),
      phoneContacts = patient.extractTelecom(),
      chwAssigned = patient.generalPractitionerFirstRep,
      showIdentifierInProfile = true,
      showDOBInProfile = false,
      healthStatus =
        patient.extractHealthStatusFromMeta(
          getApplicationConfiguration().patientTypeFilterTagViaMetaCodingSystem
        ),
      tasks =
        defaultRepository.searchResourceFor<Task>(
            subjectId = resourceId,
            subjectParam = Task.SUBJECT
          )
          .sortedBy { it.executionPeriod.start.time },
      services =
        defaultRepository.searchResourceFor(subjectId = resourceId, subjectParam = CarePlan.SUBJECT)
    )
  }

  override suspend fun countRegisterData(appFeatureName: String?): Long {
    return fhirEngine
        .search<Patient> {
          filter(Patient.ACTIVE, { value = of(true) })
        }
        .filter { it.gender != null && it.active && !it.name.isNullOrEmpty() }
        .size
        .toLong()
  }

  fun getRegisterDataFilters(id: String) = configurationRegistry.retrieveDataFilterConfiguration(id)

  fun getApplicationConfiguration(): ApplicationConfiguration {
    return configurationRegistry.retrieveConfiguration(AppConfigClassification.APPLICATION)
  }
}
