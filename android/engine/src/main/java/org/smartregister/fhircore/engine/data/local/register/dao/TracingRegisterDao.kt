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

import ca.uhn.fhir.rest.gclient.TokenClientParam
import ca.uhn.fhir.rest.param.ParamPrefixEnum
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.logicalId
import com.google.android.fhir.search.Operation
import com.google.android.fhir.search.Search
import com.google.android.fhir.search.count
import com.google.android.fhir.search.has
import com.google.android.fhir.search.search
import java.util.Date
import org.hl7.fhir.r4.model.CarePlan
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.DateTimeType
import org.hl7.fhir.r4.model.Identifier
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Practitioner
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.ResourceType
import org.hl7.fhir.r4.model.Task
import org.hl7.fhir.utilities.json.JSONUtil.has
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.app.AppConfigClassification
import org.smartregister.fhircore.engine.configuration.app.ApplicationConfiguration
import org.smartregister.fhircore.engine.data.domain.Guardian
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.domain.model.ProfileData
import org.smartregister.fhircore.engine.domain.model.RegisterData
import org.smartregister.fhircore.engine.domain.repository.RegisterDao
import org.smartregister.fhircore.engine.domain.util.PaginationConstant
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.engine.util.extension.extractAddress
import org.smartregister.fhircore.engine.util.extension.extractAddressDistrict
import org.smartregister.fhircore.engine.util.extension.extractAddressState
import org.smartregister.fhircore.engine.util.extension.extractAddressText
import org.smartregister.fhircore.engine.util.extension.extractFamilyName
import org.smartregister.fhircore.engine.util.extension.extractHealthStatusFromMeta
import org.smartregister.fhircore.engine.util.extension.extractName
import org.smartregister.fhircore.engine.util.extension.extractTelecom
import org.smartregister.fhircore.engine.util.extension.referenceValue
import org.smartregister.fhircore.engine.util.extension.toAgeDisplay
import timber.log.Timber

abstract class TracingRegisterDao
constructor(
  open val fhirEngine: FhirEngine,
  val defaultRepository: DefaultRepository,
  val configurationRegistry: ConfigurationRegistry,
  val dispatcherProvider: DefaultDispatcherProvider
) : RegisterDao {
  protected abstract val tracingCoding: Coding

  private fun Search.validTasksFilters() {
    filter(TokenClientParam("_tag"), { value = of(tracingCoding) })
    filter(
      Task.STATUS,
      { value = of(Task.TaskStatus.READY.toCode()) },
      { value = of(Task.TaskStatus.INPROGRESS.toCode()) },
      operation = Operation.OR
    )
    filter(
      Task.PERIOD,
      {
        value = of(DateTimeType.now())
        prefix = ParamPrefixEnum.GREATERTHAN
      }
    )
  }

  override suspend fun loadRegisterData(
    currentPage: Int,
    loadAll: Boolean,
    appFeatureName: String?
  ): List<RegisterData> {

    val tracingPatients =
      fhirEngine.search<Patient> {
        has<Task>(Task.SUBJECT) { validTasksFilters() }
        count =
          if (loadAll) countRegisterData(appFeatureName).toInt()
          else PaginationConstant.DEFAULT_PAGE_SIZE
        from = currentPage * PaginationConstant.DEFAULT_PAGE_SIZE
      }

    return tracingPatients
      .map { it.toTracingRegisterData() }
      .filter { it.reasons.any() }
      .sortedWith(compareBy({ it.attempts }, { it.lastAttemptDate }, { it.firstAdded }))
  }

  override suspend fun loadProfileData(appFeatureName: String?, resourceId: String): ProfileData? {
    val patient = defaultRepository.loadResource<Patient>(resourceId)
    return patient?.let {
      val metaCodingSystemTag =
        configurationRegistry.retrieveConfiguration<ApplicationConfiguration>(
            AppConfigClassification.APPLICATION
          )
          .patientTypeFilterTagViaMetaCodingSystem
      val tasks = validTasks(patient)
      ProfileData.TracingProfileData(
        logicalId = patient.logicalId,
        birthdate = patient.birthDate,
        name = patient.extractName(),
        gender = patient.gender,
        age = patient.birthDate.toAgeDisplay(),
        address = patient.extractAddress(),
        addressDistrict = patient.extractAddressDistrict(),
        addressTracingCatchment = patient.extractAddressState(),
        addressPhysicalLocator = patient.extractAddressText(),
        phoneContacts = patient.extractTelecom(),
        chwAssigned = patient.generalPractitionerFirstRep,
        showIdentifierInProfile = true,
        healthStatus = patient.extractHealthStatusFromMeta(metaCodingSystemTag),
        tasks = tasks,
        services = patient.activeCarePlans(),
        conditions = patient.activeConditions(),
        guardians = patient.guardians(),
        observations = patient.observations(),
        practitioners = patient.practitioners()
      )
    }
  }

  suspend fun Patient.activeCarePlans() =
    defaultRepository.searchResourceFor<CarePlan>(
        subjectId = this.logicalId,
        subjectType = ResourceType.Patient,
        subjectParam = CarePlan.SUBJECT
      )
      .filter { carePlan -> carePlan.status.equals(CarePlan.CarePlanStatus.ACTIVE) }

  suspend fun Patient.activeConditions() =
    defaultRepository.patientConditions(this.logicalId).filter { condition ->
      condition.clinicalStatus.coding.any { it.code == "active" }
    }

  suspend fun Patient.observations() =
    defaultRepository.searchResourceFor<Observation>(
      subjectId = this.logicalId,
      subjectParam = Observation.SUBJECT,
      subjectType = ResourceType.Patient
    )

  suspend fun Patient.practitioners(): List<Practitioner> {
    return generalPractitioner.mapNotNull {
      try {
        val id = it.reference.replace("Practitioner/", "")
        fhirEngine.get(ResourceType.Practitioner, id) as Practitioner
      } catch (e: Exception) {
        Timber.e(e)
        null
      }
    }
  }

  suspend fun Patient.guardians(): List<Guardian> =
    this.link
      .filter {
        (it.other.referenceElement.resourceType == ResourceType.RelatedPerson.name).or(
          it.type == Patient.LinkType.REFER &&
            it.other.referenceElement.resourceType == ResourceType.Patient.name
        )
      }
      .map { defaultRepository.loadResource(it.other) }

  override suspend fun countRegisterData(appFeatureName: String?): Long {
    val patients = fhirEngine.search<Patient> { has<Task>(Task.SUBJECT) { validTasksFilters() } }
    return patients.count { validTasks(it).any() }.toLong()
  }

  private fun applicationConfiguration(): ApplicationConfiguration =
    configurationRegistry.retrieveConfiguration(AppConfigClassification.APPLICATION)

  private suspend fun validTasks(patient: Patient): List<Task> {
    val patientTasks =
      fhirEngine.search<Task> {
        validTasksFilters()
        filter(Task.SUBJECT, { value = patient.referenceValue() })
      }
    return patientTasks.filter {
      it.executionPeriod.hasStart() && it.executionPeriod.start.before(Date())
    }
  }

  private suspend fun Patient.toTracingRegisterData(): RegisterData.TracingRegisterData {
    val tasks = validTasks(this)
    val oldestTaskDate = tasks.minOfOrNull { it.authoredOn }
    val taskAttempts =
      tasks
        .flatMap {
          fhirEngine.search<QuestionnaireResponse> {
            filter(
              QuestionnaireResponse.SUBJECT,
              { value = this@toTracingRegisterData.referenceValue() }
            )
            filter(QuestionnaireResponse.QUESTIONNAIRE, { value = it.reasonReference.reference })
            filter(QuestionnaireResponse.AUTHORED, { value = of(DateTimeType(oldestTaskDate)) })
          }
        }
        .distinct()

    return RegisterData.TracingRegisterData(
      logicalId = this.logicalId,
      name = this.extractName(),
      identifier =
        this.identifier.firstOrNull { it.use == Identifier.IdentifierUse.OFFICIAL }?.value,
      gender = this.gender,
      familyName = this.extractFamilyName(),
      healthStatus =
        this.extractHealthStatusFromMeta(
          applicationConfiguration().patientTypeFilterTagViaMetaCodingSystem
        ),
      isPregnant = defaultRepository.isPatientPregnant(this),
      isBreastfeeding = defaultRepository.isPatientBreastfeeding(this),
      attempts = taskAttempts.size,
      lastAttemptDate = taskAttempts.maxOfOrNull { it.authored },
      firstAdded = oldestTaskDate,
      reasons =
        tasks.flatMap { task ->
          task.reasonCode.coding.map { it.display ?: it.code }.ifEmpty {
            listOf(task.reasonCode.text)
          }
        }
    )
  }
}
