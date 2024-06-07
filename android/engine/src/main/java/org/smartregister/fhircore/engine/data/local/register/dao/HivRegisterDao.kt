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
import com.google.android.fhir.SearchResult
import com.google.android.fhir.datacapture.extensions.logicalId
import com.google.android.fhir.search.Operation
import com.google.android.fhir.search.Order
import com.google.android.fhir.search.Search
import com.google.android.fhir.search.StringFilterModifier
import com.google.android.fhir.search.revInclude
import com.google.android.fhir.search.search
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.hl7.fhir.r4.model.CodeType
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.Condition
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Practitioner
import org.hl7.fhir.r4.model.Reference
import org.hl7.fhir.r4.model.RelatedPerson
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
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
import org.smartregister.fhircore.engine.util.extension.activePatientConditions
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
  configurationRegistry: Provider<ConfigurationRegistry>,
) : RegisterDao, PatientDao {

  private val patientTypeMetaTagCodingSystem by lazy {
    configurationRegistry.get().getAppConfigs().patientTypeFilterTagViaMetaCodingSystem
  }

  fun isValidPatient(patient: Patient): Boolean =
    patient.active &&
      !patient.hasDeceased() &&
      patient.hasName() &&
      patient.hasGender() &&
      patient.meta.tag.none { it.code.equals(HAPI_MDM_TAG, true) }

  fun hivPatientIdentifier(patient: Patient): String =
    // would either be an ART or HCC number
    patient.extractOfficialIdentifier() ?: ResourceValue.BLANK

  private suspend fun searchRegisterData(block: Search.() -> Unit = {}) =
    fhirEngine.search<Patient> {
      filter(Patient.ACTIVE, { value = of(true) })
      filter(Patient.DECEASED, { value = of(false) })
      filter(
        Patient.GENDER,
        { value = of("male") },
        { value = of("female") },
        operation = Operation.OR,
      )

      revInclude<Condition>(Condition.SUBJECT) {
        filter(
          Condition.CODE,
          { value = of(CodeType("77386006")) },
          { value = of(CodeType("413712001")) },
          operation = Operation.OR,
        )
        filter(
          Condition.VERIFICATION_STATUS,
          {
            value =
              of(
                CodeableConcept()
                  .addCoding(
                    Coding(
                      "https://terminology.hl7.org/CodeSystem/condition-ver-status",
                      "confirmed",
                      null,
                    ),
                  ),
              )
          },
        )
        filter(
          Condition.CLINICAL_STATUS,
          {
            value =
              of(
                CodeableConcept()
                  .addCoding(
                    Coding(
                      "https://terminology.hl7.org/CodeSystem/condition-clinical",
                      "active",
                      null,
                    ),
                  ),
              )
          },
        )
      }
      sort(DateClientParam("_lastUpdated"), Order.DESCENDING)

      block.invoke(this)
    }

  private fun List<SearchResult<Patient>>.filterValidPatientResult() = filter {
    val patient = it.resource
    isValidPatient(patient) &&
      patient.extractHealthStatusFromMeta(patientTypeMetaTagCodingSystem) != HealthStatus.DEFAULT
  }

  private fun List<SearchResult<Patient>>.transformToHivRegisterData() = map { searchResult ->
    val subject = searchResult.resource
    val conditions =
      searchResult.revIncluded?.get(ResourceType.Condition to Condition.SUBJECT.paramName)?.map {
        it as Condition
      } ?: emptyList()
    transformPatientToHivRegisterData(subject, conditions.getPregnancyStatus())
  }

  override suspend fun loadRegisterData(
    currentPage: Int,
    loadAll: Boolean,
    appFeatureName: String?,
  ): List<RegisterData> {
    val patients = searchRegisterData {
      if (!loadAll) {
        count = PaginationConstant.DEFAULT_PAGE_SIZE + PaginationConstant.EXTRA_ITEM_COUNT
      }
      if (currentPage > 0) {
        from = currentPage * PaginationConstant.DEFAULT_PAGE_SIZE
      }
    }

    return patients.filterValidPatientResult().transformToHivRegisterData()
  }

  override suspend fun searchByName(
    nameQuery: String,
    currentPage: Int,
    appFeatureName: String?,
  ): List<RegisterData> {
    val patientSearchedByName = searchRegisterData {
      filter(
        Patient.NAME,
        {
          modifier = StringFilterModifier.CONTAINS
          value = nameQuery
        },
      )
      count = PaginationConstant.DEFAULT_PAGE_SIZE
      from = currentPage * PaginationConstant.DEFAULT_PAGE_SIZE
    }

    val patientSearchedByIdentifier = searchRegisterData {
      filter(Patient.IDENTIFIER, { value = of(nameQuery) })
      count = PaginationConstant.DEFAULT_PAGE_SIZE
      from = currentPage * PaginationConstant.DEFAULT_PAGE_SIZE
    }

    val patientSearchResults =
      if (nameQuery.contains(Regex("[0-9]"))) patientSearchedByIdentifier + patientSearchedByName
      else patientSearchedByName + patientSearchedByIdentifier

    return patientSearchResults.filterValidPatientResult().transformToHivRegisterData()
  }

  override suspend fun loadProfileData(appFeatureName: String?, resourceId: String): ProfileData {
    val patient = defaultRepository.loadResource<Patient>(resourceId)!!
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
      healthStatus = patient.extractHealthStatusFromMeta(patientTypeMetaTagCodingSystem),
      tasks =
        carePlan
          ?.activity
          ?.filter { it.shouldShowOnProfile() }
          ?.sortedWith(compareBy(nullsLast()) { it?.detail?.code?.text?.toBigIntegerOrNull() })
          ?: listOf(),
      conditions = defaultRepository.activePatientConditions(patient.logicalId),
      otherPatients = patient.otherChildren(),
      guardians = patient.guardians(),
      observations = patient.observations(),
      practitioners = patient.practitioners(),
    )
  }

  override suspend fun countRegisterData(): Flow<Long> {
    var counter = 0L
    var offset = 0
    val pageCount = 100

    return flow {
      do {
        val patients =
          fhirEngine.search<Patient> {
            filter(Patient.ACTIVE, { value = of(true) })
            filter(Patient.DECEASED, { value = of(false) })
            filter(
              Patient.GENDER,
              { value = of("male") },
              { value = of("female") },
              operation = Operation.OR,
            )
            from = offset
            count = pageCount
          }
        offset += patients.size
        val validPatientCount =
          patients
            .map { it.resource }
            .filter(this@HivRegisterDao::isValidPatient)
            .filterNot {
              it.extractHealthStatusFromMeta(patientTypeMetaTagCodingSystem) == HealthStatus.DEFAULT
            }
            .size
        counter += validPatientCount
        emit(counter)
      } while (patients.isNotEmpty())
    }
  }

  override suspend fun loadPatient(patientId: String) = fhirEngine.loadResource<Patient>(patientId)

  override suspend fun loadRelatedPerson(logicalId: String) =
    fhirEngine.loadResource<RelatedPerson>(logicalId)

  override suspend fun loadGuardiansRegisterData(patient: Patient) =
    patient
      .guardians()
      .filterIsInstance<Patient>()
      .map {
        val pregnancyStatus =
          defaultRepository.activePatientConditions(patient.logicalId).getPregnancyStatus()
        transformPatientToHivRegisterData(it, pregnancyStatus)
      }
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

  private fun transformPatientToHivRegisterData(
    patient: Patient,
    pregnancyStatus: PregnancyStatus = PregnancyStatus.None,
  ): RegisterData.HivRegisterData {
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
      healthStatus = patient.extractHealthStatusFromMeta(patientTypeMetaTagCodingSystem),
      isPregnant = pregnancyStatus == PregnancyStatus.Pregnant,
      isBreastfeeding = pregnancyStatus == PregnancyStatus.BreastFeeding,
    )
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
    val healthStatus = this.extractHealthStatusFromMeta(patientTypeMetaTagCodingSystem)

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
              patientTypeMetaTagCodingSystem,
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
