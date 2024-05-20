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

import ca.uhn.fhir.rest.gclient.DateClientParam
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.logicalId
import com.google.android.fhir.search.Operation
import com.google.android.fhir.search.Order
import com.google.android.fhir.search.StringFilterModifier
import com.google.android.fhir.search.search
import javax.inject.Inject
import javax.inject.Singleton
import org.hl7.fhir.r4.model.Identifier
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Practitioner
import org.hl7.fhir.r4.model.Reference
import org.hl7.fhir.r4.model.RelatedPerson
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.app.ApplicationConfiguration
import org.smartregister.fhircore.engine.data.domain.Guardian
import org.smartregister.fhircore.engine.data.domain.PregnancyStatus
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.domain.model.HealthStatus
import org.smartregister.fhircore.engine.domain.model.ProfileData
import org.smartregister.fhircore.engine.domain.model.RegisterData
import org.smartregister.fhircore.engine.domain.repository.PatientDao
import org.smartregister.fhircore.engine.domain.repository.RegisterDao
import org.smartregister.fhircore.engine.domain.util.PaginationConstant
import org.smartregister.fhircore.engine.util.extension.activeCarePlans
import org.smartregister.fhircore.engine.util.extension.canonical
import org.smartregister.fhircore.engine.util.extension.canonicalName
import org.smartregister.fhircore.engine.util.extension.extractAddress
import org.smartregister.fhircore.engine.util.extension.extractAddressDistrict
import org.smartregister.fhircore.engine.util.extension.extractAddressState
import org.smartregister.fhircore.engine.util.extension.extractAddressText
import org.smartregister.fhircore.engine.util.extension.extractFamilyName
import org.smartregister.fhircore.engine.util.extension.extractGeneralPractitionerReference
import org.smartregister.fhircore.engine.util.extension.extractGivenName
import org.smartregister.fhircore.engine.util.extension.extractHealthStatusFromMeta
import org.smartregister.fhircore.engine.util.extension.extractId
import org.smartregister.fhircore.engine.util.extension.extractName
import org.smartregister.fhircore.engine.util.extension.extractOfficialIdentifier
import org.smartregister.fhircore.engine.util.extension.extractTelecom
import org.smartregister.fhircore.engine.util.extension.familyName
import org.smartregister.fhircore.engine.util.extension.getPregnancyStatus
import org.smartregister.fhircore.engine.util.extension.getResourcesByIds
import org.smartregister.fhircore.engine.util.extension.givenName
import org.smartregister.fhircore.engine.util.extension.loadResource
import org.smartregister.fhircore.engine.util.extension.patientConditions
import org.smartregister.fhircore.engine.util.extension.shouldShowOnProfile
import org.smartregister.fhircore.engine.util.extension.toAgeDisplay
import org.smartregister.fhircore.engine.util.extension.yearsPassed
import timber.log.Timber

@Singleton
class HivRegisterDao
@Inject
constructor(
  val fhirEngine: FhirEngine,
  val defaultRepository: DefaultRepository,
  val configurationRegistry: ConfigurationRegistry,
) : RegisterDao, PatientDao {

  fun isValidPatient(patient: Patient): Boolean =
    patient.active &&
      !patient.hasDeceased() &&
      patient.hasName() &&
      patient.hasGender() &&
      patient.meta.tag.none { it.code.equals(HAPI_MDM_TAG, true) }

  fun hivPatientIdentifier(patient: Patient): String =
    // would either be an ART or HCC number
    patient.extractOfficialIdentifier() ?: ResourceValue.BLANK

  override suspend fun loadRegisterData(
    currentPage: Int,
    loadAll: Boolean,
    appFeatureName: String?,
  ): List<RegisterData> {
    val patients =
      fhirEngine.search<Patient> {
        filter(Patient.ACTIVE, { value = of(true) })
        sort(DateClientParam("_lastUpdated"), Order.DESCENDING)
        if (!loadAll) {
          count = PaginationConstant.DEFAULT_PAGE_SIZE
        }
        from = currentPage * PaginationConstant.DEFAULT_PAGE_SIZE
      }

    return patients
      .map { it.resource }
      .filter(this::isValidPatient)
      .map { transformPatientToHivRegisterData(it) }
      .filterNot { it.healthStatus == HealthStatus.DEFAULT }
  }

  override suspend fun searchByName(
    nameQuery: String,
    currentPage: Int,
    appFeatureName: String?,
  ): List<RegisterData> {
    val patients =
      fhirEngine.search<Patient> {
        filter(
          Patient.NAME,
          {
            modifier = StringFilterModifier.CONTAINS
            value = nameQuery
          },
        )
        filter(Patient.IDENTIFIER, { value = of(Identifier().apply { value = nameQuery }) })
        operation = Operation.OR
        sort(DateClientParam("_lastUpdated"), Order.DESCENDING)
      }

    return patients
      .map { it.resource }
      .mapNotNull { patient ->
        if (isValidPatient(patient)) {
          val transFormedPatient = transformPatientToHivRegisterData(patient)
          if (transFormedPatient.healthStatus != HealthStatus.DEFAULT) {
            return@mapNotNull transFormedPatient
          }
        }
        return@mapNotNull null
      }
  }

  override suspend fun loadProfileData(appFeatureName: String?, resourceId: String): ProfileData {
    val patient = defaultRepository.loadResource<Patient>(resourceId)!!
    val configuration = getApplicationConfiguration()
    val carePlan = patient.activeCarePlans(fhirEngine).firstOrNull()

    return ProfileData.HivProfileData(
      logicalId = patient.logicalId,
      birthdate = patient.birthDate,
      name = patient.extractName(),
      givenName = patient.extractGivenName(),
      familyName = patient.extractFamilyName(),
      identifier = hivPatientIdentifier(patient),
      gender = patient.gender,
      age = patient.birthDate.toAgeDisplay(),
      address = patient.extractAddress(),
      addressDistrict = patient.extractAddressDistrict(),
      addressTracingCatchment = patient.extractAddressState(),
      addressPhysicalLocator = patient.extractAddressText(),
      phoneContacts = patient.extractTelecom(),
      chwAssigned = patient.generalPractitionerFirstRep,
      showIdentifierInProfile = true,
      currentCarePlan = carePlan,
      healthStatus =
        patient.extractHealthStatusFromMeta(configuration.patientTypeFilterTagViaMetaCodingSystem),
      tasks =
        carePlan
          ?.activity
          ?.filter { it.shouldShowOnProfile() }
          ?.sortedWith(compareBy(nullsLast()) { it?.detail?.code?.text?.toBigIntegerOrNull() })
          ?: listOf(),
      conditions = patient.activeConditions(),
      otherPatients = patient.otherChildren(),
      guardians = patient.guardians(),
      observations = patient.observations(),
      practitioners = patient.practitioners(),
    )
  }

  override suspend fun countRegisterData(appFeatureName: String?): Long {
    return fhirEngine
      .search<Patient> { filter(Patient.ACTIVE, { value = of(true) }) }
      .map { it.resource }
      .filter(this::isValidPatient)
      .size
      .toLong()
  }

  override suspend fun loadPatient(patientId: String) = fhirEngine.loadResource<Patient>(patientId)

  override suspend fun loadRelatedPerson(logicalId: String) =
    fhirEngine.loadResource<RelatedPerson>(logicalId)

  override suspend fun loadGuardiansRegisterData(patient: Patient) =
    patient
      .guardians()
      .filterIsInstance<Patient>()
      .map { transformPatientToHivRegisterData(it) }
      .plus(
        patient.guardians().filterIsInstance<RelatedPerson>().map {
          transformRelatedPersonToHivRegisterData(it)
        },
      )

  private fun transformRelatedPersonToHivRegisterData(person: RelatedPerson) =
    RegisterData.HivRegisterData(
      logicalId = person.logicalId,
      identifier = ResourceValue.BLANK,
      name = person.name.canonicalName(),
      gender = person.gender,
      age = person.birthDate.toAgeDisplay(),
      address = person.addressFirstRep.canonical(),
      givenName = person.name.givenName(),
      familyName = person.name.familyName(),
      phoneContacts = if (person.hasTelecom()) person.telecom.map { it.value } else emptyList(),
      chwAssigned = ResourceValue.BLANK,
      healthStatus = HealthStatus.NOT_ON_ART,
    )

  override suspend fun loadRelatedPersonProfileData(logicalId: String): ProfileData =
    transformRelatedPersonToHivProfileData(fhirEngine.loadResource<RelatedPerson>(logicalId)!!)

  private fun transformRelatedPersonToHivProfileData(person: RelatedPerson) =
    ProfileData.HivProfileData(
      logicalId = person.logicalId,
      birthdate = person.birthDate,
      name = person.name.canonicalName(),
      givenName = person.name.givenName(),
      familyName = person.name.familyName(),
      identifier = ResourceValue.BLANK,
      gender = person.gender,
      age = person.birthDate.toAgeDisplay(),
      address = person.addressFirstRep.canonical(),
      phoneContacts = if (person.hasTelecom()) person.telecom.map { it.value } else emptyList(),
      chwAssigned = Reference(), // Empty
      showIdentifierInProfile = false,
      healthStatus = HealthStatus.NOT_ON_ART,
      currentCarePlan = null,
    )

  private suspend fun transformPatientToHivRegisterData(
    patient: Patient,
  ): RegisterData.HivRegisterData {
    val pregnancyStatus = defaultRepository.getPregnancyStatus(patient.logicalId)

    return RegisterData.HivRegisterData(
      logicalId = patient.logicalId,
      identifier = hivPatientIdentifier(patient),
      name = patient.extractName(),
      gender = patient.gender,
      age = patient.birthDate.toAgeDisplay(),
      address = patient.extractAddress(),
      givenName = patient.extractGivenName(),
      familyName = patient.extractFamilyName(),
      phoneContacts = patient.extractTelecom(),
      practitioners = patient.generalPractitioner,
      chwAssigned = patient.extractGeneralPractitionerReference(),
      healthStatus =
        patient.extractHealthStatusFromMeta(
          getApplicationConfiguration().patientTypeFilterTagViaMetaCodingSystem,
        ),
      isPregnant = pregnancyStatus == PregnancyStatus.Pregnant,
      isBreastfeeding = pregnancyStatus == PregnancyStatus.BreastFeeding,
    )
  }

  internal suspend fun Patient.activeConditions() =
    defaultRepository.patientConditions(this.logicalId).filter { condition ->
      condition.clinicalStatus.coding.any { it.code == "active" }
    }

  internal suspend fun Patient.observations() =
    defaultRepository
      .searchResourceFor<Observation>(
        subjectId = this.logicalId,
        subjectParam = Observation.SUBJECT,
        subjectType = ResourceType.Patient,
      )
      .also {
        return if (it.isEmpty().not()) {
          it
            .sortedByDescending { it.effectiveDateTimeType.value }
            .distinctBy { it.code.coding.last().code }
        } else {
          emptyList()
        }
      }

  internal suspend fun Patient.practitioners(): List<Practitioner> {
    return generalPractitioner
      .mapNotNull {
        try {
          val id = it.reference.replace("Practitioner/", "")
          fhirEngine.get(ResourceType.Practitioner, id) as Practitioner
        } catch (e: Exception) {
          Timber.e(e)
          null
        }
      }
      .distinctBy { it.logicalId }
  }

  private suspend fun Patient.otherChildren(): List<Resource> {
    val list: ArrayList<Patient> = arrayListOf()

    val filteredItems =
      defaultRepository.searchResourceFor<Patient>(
        subjectId = logicalId,
        subjectType = ResourceType.Patient,
        subjectParam = Patient.LINK,
      )

    for (item in filteredItems) {
      if (item.isValidChildContact()) list.add(item)
    }
    return list
  }

  private fun Patient.isValidChildContact(): Boolean {
    val healthStatus =
      this.extractHealthStatusFromMeta(
        getApplicationConfiguration().patientTypeFilterTagViaMetaCodingSystem,
      )

    if (
      healthStatus == HealthStatus.CHILD_CONTACT ||
        healthStatus == HealthStatus.EXPOSED_INFANT ||
        healthStatus == HealthStatus.CLIENT_ALREADY_ON_ART ||
        healthStatus == HealthStatus.NEWLY_DIAGNOSED_CLIENT
    ) {
      return this.hasBirthDate() && this.birthDate!!.yearsPassed() <= LINKED_CHILD_AGE_LIMIT
    }

    return false
  }

  internal suspend fun Patient.guardians(): List<Guardian> {
    val patients = mutableListOf<String>()
    val relatedPersons = mutableListOf<String>()
    this.link.forEach {
      if (
        it.type == Patient.LinkType.REFER &&
          it.other.referenceElement.resourceType == ResourceType.RelatedPerson.name
      ) {
        relatedPersons.add(it.other.extractId())
      } else if (
        it.type == Patient.LinkType.REFER &&
          it.other.referenceElement.resourceType == ResourceType.Patient.name
      ) {
        patients.add(it.other.extractId())
      }
    }
    return fhirEngine.getResourcesByIds<Patient>(patients) +
      fhirEngine.getResourcesByIds<RelatedPerson>(relatedPersons)
  }

  private fun getApplicationConfiguration(): ApplicationConfiguration {
    return configurationRegistry.getAppConfigs()
  }

  suspend fun removePatient(patientId: String) {
    val patient =
      defaultRepository.loadResource<Patient>(patientId)!!.apply {
        if (!this.active) throw IllegalStateException("Patient already deleted")
        this.active = false
      }
    defaultRepository.addOrUpdate(true, patient)
  }

  suspend fun transformChildrenPatientToRegisterData(patients: List<Patient>): List<RegisterData> {
    return patients
      .filter(this::isValidPatient)
      .map { patient ->
        val pregnancyStatus = defaultRepository.getPregnancyStatus(patient.logicalId)

        RegisterData.HivRegisterData(
          logicalId = patient.logicalId,
          identifier = hivPatientIdentifier(patient),
          name = patient.extractName(),
          gender = patient.gender,
          age = patient.birthDate.toAgeDisplay(),
          address = patient.extractAddress(),
          givenName = patient.extractGivenName(),
          familyName = patient.extractFamilyName(),
          phoneContacts = patient.extractTelecom(),
          practitioners = patient.generalPractitioner,
          chwAssigned = patient.extractGeneralPractitionerReference(),
          healthStatus =
            patient.extractHealthStatusFromMeta(
              getApplicationConfiguration().patientTypeFilterTagViaMetaCodingSystem,
            ),
          isPregnant = pregnancyStatus == PregnancyStatus.Pregnant,
          isBreastfeeding = pregnancyStatus == PregnancyStatus.BreastFeeding,
        )
      }
      .filterNot { it.healthStatus == HealthStatus.DEFAULT }
  }

  object ResourceValue {
    const val BLANK = ""
  }

  companion object {
    const val HAPI_MDM_TAG = "HAPI-MDM"
    const val LINKED_CHILD_AGE_LIMIT = 15
    const val ORGANISATION_SYSTEM = "http://smartregister.org/fhir/organization-tag"
    const val ORGANISATION_DISPLAY = "Practitioner Organization"
  }
}
