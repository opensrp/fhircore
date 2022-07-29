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
import javax.inject.Inject
import javax.inject.Singleton
import org.hl7.fhir.r4.model.CarePlan
import org.hl7.fhir.r4.model.Condition
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.ResourceType
import org.hl7.fhir.r4.model.Task
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
import org.smartregister.fhircore.engine.util.extension.extractSecondaryIdentifier
import org.smartregister.fhircore.engine.util.extension.extractTelecom
import org.smartregister.fhircore.engine.util.extension.hasActivePregnancy
import org.smartregister.fhircore.engine.util.extension.toAgeDisplay

@Singleton
class HivRegisterDao
@Inject
constructor(
  val fhirEngine: FhirEngine,
  val defaultRepository: DefaultRepository,
  val configurationRegistry: ConfigurationRegistry
) : RegisterDao {

  fun isValidPatient(patient: Patient): Boolean =
    patient.hasName() &&
      patient.hasGender() &&
      patient.meta.tag.none { it.code.equals(HAPI_MDM_TAG, true) }

  fun hivPatientIdentifier(patient: Patient): String =
    // would either be an ART or HCC number
    patient.extractOfficialIdentifier()
      ?: patient.extractSecondaryIdentifier() ?: patient.identifierFirstRep.value ?: ""

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

    return patients.filter(this::isValidPatient).map { patient ->
      RegisterData.HivRegisterData(
        logicalId = patient.logicalId,
        identifier = hivPatientIdentifier(patient),
        name = patient.extractName(),
        gender = patient.gender,
        age = patient.birthDate.toAgeDisplay(),
        address = patient.extractAddress(),
        familyName = if (patient.hasName()) patient.nameFirstRep.family else null,
        phoneContacts = patient.extractTelecom(),
        practitioners = patient.generalPractitioner,
        chwAssigned = patient.extractGeneralPractitionerReference(),
        healthStatus =
          patient.extractHealthStatusFromMeta(
            getApplicationConfiguration().patientTypeFilterTagViaMetaCodingSystem
          ),
        isPregnant = patient.isPregnant()
      )
    }
  }

  override suspend fun loadProfileData(appFeatureName: String?, resourceId: String): ProfileData {
    val patient = defaultRepository.loadResource<Patient>(resourceId)!!

    return ProfileData.HivProfileData(
      logicalId = patient.logicalId,
      birthdate = patient.birthDate,
      name = patient.extractName(),
      identifier = hivPatientIdentifier(patient),
      gender = patient.gender,
      age = patient.birthDate.toAgeDisplay(),
      address = patient.extractAddress(),
      phoneContacts = patient.extractTelecom(),
      chwAssigned = patient.generalPractitionerFirstRep,
      showIdentifierInProfile = true,
      healthStatus =
        patient.extractHealthStatusFromMeta(
          getApplicationConfiguration().patientTypeFilterTagViaMetaCodingSystem
        ),
      tasks =
        defaultRepository.searchResourceFor<Task>(
            subjectId = resourceId,
            subjectType = ResourceType.Patient,
            subjectParam = Task.SUBJECT
          )
          .sortedBy { it.executionPeriod.start.time },
      services =
        defaultRepository.searchResourceFor<CarePlan>(
          subjectId = resourceId,
          subjectType = ResourceType.Patient,
          subjectParam = CarePlan.SUBJECT
        )
    )
  }

  override suspend fun countRegisterData(appFeatureName: String?): Long {
    return fhirEngine
      .search<Patient> { filter(Patient.ACTIVE, { value = of(true) }) }
      .filter(this::isValidPatient)
      .size
      .toLong()
  }

  internal suspend fun Patient.isPregnant() = patientConditions(this.logicalId).hasActivePregnancy()

  internal suspend fun patientConditions(patientId: String) =
    defaultRepository.searchResourceFor<Condition>(
      subjectId = patientId,
      subjectParam = Condition.SUBJECT,
      subjectType = ResourceType.Patient
    )

  fun getRegisterDataFilters(id: String) = configurationRegistry.retrieveDataFilterConfiguration(id)

  fun getApplicationConfiguration(): ApplicationConfiguration {
    return configurationRegistry.retrieveConfiguration(AppConfigClassification.APPLICATION)
  }

  companion object {
    const val HAPI_MDM_TAG = "HAPI-MDM"
  }
}
