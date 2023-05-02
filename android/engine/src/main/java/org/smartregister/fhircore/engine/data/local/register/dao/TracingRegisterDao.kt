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
import com.google.android.fhir.get
import com.google.android.fhir.logicalId
import com.google.android.fhir.search.Operation
import com.google.android.fhir.search.Order
import com.google.android.fhir.search.Search
import com.google.android.fhir.search.count
import com.google.android.fhir.search.filter.ReferenceParamFilterCriterion
import com.google.android.fhir.search.filter.TokenParamFilterCriterion
import com.google.android.fhir.search.has
import com.google.android.fhir.search.search
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import org.hl7.fhir.r4.model.Appointment
import org.hl7.fhir.r4.model.CarePlan
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.DateTimeType
import org.hl7.fhir.r4.model.Identifier
import org.hl7.fhir.r4.model.ListResource
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Practitioner
import org.hl7.fhir.r4.model.ResourceType
import org.hl7.fhir.r4.model.Task
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.app.AppConfigClassification
import org.smartregister.fhircore.engine.configuration.app.ApplicationConfiguration
import org.smartregister.fhircore.engine.data.domain.Guardian
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.data.local.RegisterFilter
import org.smartregister.fhircore.engine.data.local.TracingAgeFilterEnum
import org.smartregister.fhircore.engine.data.local.TracingRegisterFilter
import org.smartregister.fhircore.engine.data.local.tracing.TracingRepository
import org.smartregister.fhircore.engine.domain.model.ProfileData
import org.smartregister.fhircore.engine.domain.model.RegisterData
import org.smartregister.fhircore.engine.domain.repository.RegisterDao
import org.smartregister.fhircore.engine.domain.util.PaginationConstant
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.engine.util.LOGGED_IN_PRACTITIONER
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.extension.asDdMmmYyyy
import org.smartregister.fhircore.engine.util.extension.asReference
import org.smartregister.fhircore.engine.util.extension.extractAddress
import org.smartregister.fhircore.engine.util.extension.extractAddressDistrict
import org.smartregister.fhircore.engine.util.extension.extractAddressState
import org.smartregister.fhircore.engine.util.extension.extractAddressText
import org.smartregister.fhircore.engine.util.extension.extractFamilyName
import org.smartregister.fhircore.engine.util.extension.extractHealthStatusFromMeta
import org.smartregister.fhircore.engine.util.extension.extractName
import org.smartregister.fhircore.engine.util.extension.extractOfficialIdentifier
import org.smartregister.fhircore.engine.util.extension.extractTelecom
import org.smartregister.fhircore.engine.util.extension.referenceValue
import org.smartregister.fhircore.engine.util.extension.safeSubList
import org.smartregister.fhircore.engine.util.extension.toAgeDisplay
import timber.log.Timber

abstract class TracingRegisterDao
constructor(
  open val fhirEngine: FhirEngine,
  val defaultRepository: DefaultRepository,
  private val tracingRepository: TracingRepository,
  val configurationRegistry: ConfigurationRegistry,
  val dispatcherProvider: DefaultDispatcherProvider,
  val sharedPreferencesHelper: SharedPreferencesHelper
) : RegisterDao {
  protected abstract val tracingCoding: Coding

  private val alternateTracingCoding = lazy {
    tracingCoding.copy().apply { system = "http://snomed.info/sct" }
  }
  fun hivPatientIdentifier(patient: Patient): String =
    // would either be an ART or HCC number
    patient.extractOfficialIdentifier() ?: HivRegisterDao.ResourceValue.BLANK

  private val currentPractitioner by lazy {
    sharedPreferencesHelper.read<Practitioner>(
      key = LOGGED_IN_PRACTITIONER,
      decodeFhirResource = true
    )
  }

  private fun Search.validTasksFilters() {
    filter(
      TokenClientParam("_tag"),
      { value = of(tracingCoding) },
      { value = of(alternateTracingCoding.value) },
      operation = Operation.OR
    )
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

  private suspend fun searchRegister(
    filters: RegisterFilter,
    loadAll: Boolean,
    page: Int = -1
  ): List<Pair<Patient, Iterable<Task>>> {
    filters as TracingRegisterFilter
    val patients: List<Patient> =
      fhirEngine
        .search<Patient> {
          if (!loadAll) count = PaginationConstant.DEFAULT_PAGE_SIZE

          if (page >= 0) from = page * PaginationConstant.DEFAULT_PAGE_SIZE

          has<Task>(Task.SUBJECT) { validTasksFilters() }

          filters.patientCategory?.let {
            val paramQueries: List<(TokenParamFilterCriterion.() -> Unit)> =
              it.flatMap { healthStatus ->
                val coding: Coding =
                  Coding().apply {
                    system = "https://d-tree.org"
                    code = healthStatus.name.lowercase().replace("_", "-")
                  }
                val alternativeCoding: Coding =
                  Coding().apply {
                    system = "https://d-tree.org"
                    code = healthStatus.name.lowercase()
                  }

                return@flatMap listOf<Coding>(coding, alternativeCoding).map<
                  Coding, TokenParamFilterCriterion.() -> Unit> { c -> { value = of(c) } }
              }

            filter(TokenClientParam("_tag"), *paramQueries.toTypedArray(), operation = Operation.OR)
          }

          if (filters.isAssignedToMe) {
            filter(Patient.GENERAL_PRACTITIONER, { value = currentPractitioner!!.referenceValue() })
          }
        }
        .filter {
          if (filters.age != null) {
            val today = LocalDate.now().atStartOfDay(ZoneId.systemDefault())
            when (filters.age) {
              TracingAgeFilterEnum.ZERO_TO_2 -> {
                val date = Date.from(today.minusYears(2L).toInstant())
                it.birthDate.after(date)
              }
              TracingAgeFilterEnum.ZERO_TO_18 -> {
                val date = Date.from(today.minusYears(18L).toInstant())
                it.birthDate.after(date)
              }
              TracingAgeFilterEnum.`18_PLUS` -> {
                val date = Date.from(today.minusYears(18L).toInstant())
                it.birthDate.time <= date.time
              }
            }
          } else true
        }

    val patientrefs =
      patients
        .map<Patient, (ReferenceParamFilterCriterion.() -> Unit)> {
          return@map { value = it.referenceValue() }
        }
        .toTypedArray()

    val tasks: List<Task> =
      if (patientrefs.isNotEmpty()) {
        fhirEngine
          .search<Task> {
            validTasksFilters()
            filter(Task.SUBJECT, *patientrefs, operation = Operation.OR)
          }
          .filter {
            it.status in listOf(Task.TaskStatus.INPROGRESS, Task.TaskStatus.READY) &&
              it.executionPeriod.hasStart() &&
              it.executionPeriod
                .start
                .before(Date())
                .or(it.executionPeriod.start.asDdMmmYyyy() == Date().asDdMmmYyyy())
          }
      } else emptyList()

    val filteredTasks =
      filters.reasonCode?.let { reasonCode ->
        tasks.filter { it.reasonCode.coding.any { coding -> coding.code == reasonCode } }
      }
        ?: tasks
    val groupedTasks = filteredTasks.groupBy { it.`for`.reference }

    return patients
      .filter {
        val ref = it.asReference().reference
        ref in groupedTasks && groupedTasks[ref] != null && groupedTasks[ref]!!.any()
      }
      .map { it to groupedTasks[it.asReference().reference]!! }
  }

  override suspend fun loadRegisterFiltered(
    currentPage: Int,
    loadAll: Boolean,
    appFeatureName: String?,
    filters: RegisterFilter
  ): List<RegisterData> {
    val patientTasksPairs = searchRegister(filters, loadAll = true)
    val patientSubjectRefFilterCriteria =
      patientTasksPairs
        .map { it.first }
        .map<Patient, (ReferenceParamFilterCriterion.() -> Unit)> {
          return@map { value = it.referenceValue() }
        }
        .toTypedArray()
    val subjectListResourcesGroup: Map<String, ListResource> =
      if (patientSubjectRefFilterCriteria.isNotEmpty())
        fhirEngine
          .search<ListResource> {
            filter(ListResource.SUBJECT, *patientSubjectRefFilterCriteria, operation = Operation.OR)
            filter(ListResource.STATUS, { value = of(ListResource.ListStatus.CURRENT.toCode()) })
            sort(ListResource.TITLE, Order.DESCENDING)
          }
          .filter { it.status == ListResource.ListStatus.CURRENT }
          .groupingBy { it.subject.reference }
          .reduce { _, accumulator, element -> maxOf(accumulator, element, compareBy { it.title }) }
      else emptyMap()

    val tracingData: List<RegisterData.TracingRegisterData> =
      patientTasksPairs
        .map { ptp ->
          val (patient, tasks) = ptp
          val listResource = subjectListResourcesGroup[patient.referenceValue()]
          patient.toTracingRegisterData(tasks, listResource)
        }
        .filter { it.reasons.any() }
        .sortedWith(compareBy({ it.attempts }, { it.lastAttemptDate }, { it.firstAdded }))

    return if (!loadAll) {
      val from = currentPage * PaginationConstant.DEFAULT_PAGE_SIZE
      val to = from + PaginationConstant.DEFAULT_PAGE_SIZE
      tracingData.safeSubList(from..to)
    } else tracingData
  }

  override suspend fun loadRegisterData(
    currentPage: Int,
    loadAll: Boolean,
    appFeatureName: String?
  ): List<RegisterData> {

    val tracingPatients =
      fhirEngine.search<Patient> { has<Task>(Task.SUBJECT) { validTasksFilters() } }

    return tracingPatients
      .map { it.toTracingRegisterData() }
      .filter { it.reasons.any() }
      .sortedWith(compareBy({ it.attempts }, { it.lastAttemptDate }, { it.firstAdded }))
      .let {
        if (!loadAll) {
          val from = currentPage * PaginationConstant.DEFAULT_PAGE_SIZE
          val to = from + PaginationConstant.DEFAULT_PAGE_SIZE
          it.safeSubList(from..to)
        } else it
      }
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

      val attempt = tracingRepository.getTracingAttempt(patient)
      var dueData = getDueDate(it)
      if (dueData == null) {
        tasks.minOfOrNull { task -> task.authoredOn }?.let { date -> dueData = date }
      }

      ProfileData.TracingProfileData(
        identifier = hivPatientIdentifier(patient),
        logicalId = patient.logicalId,
        birthdate = patient.birthDate,
        name = patient.extractName(),
        gender = patient.gender,
        age = patient.birthDate.toAgeDisplay(),
        dueDate = dueData,
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
        practitioners = patient.practitioners(),
        currentAttempt =
          attempt.copy(
            reasons = tasks.mapNotNull { task -> task.reasonCode?.codingFirstRep?.display }
          ),
      )
    }
  }

  private suspend fun getDueDate(patient: Patient): Date? {
    val appointments =
      fhirEngine.search<Appointment> {
        filter(Appointment.STATUS, { value = of(Appointment.AppointmentStatus.BOOKED.toCode()) })
        filter(Appointment.ACTOR, { value = patient.referenceValue() })
        sort(Appointment.DATE, Order.ASCENDING)
        count = 1
        from = 0
      }
    val appointment = appointments.firstOrNull()
    return appointment?.start
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

  override suspend fun countRegisterFiltered(
    appFeatureName: String?,
    filters: RegisterFilter
  ): Long {
    return searchRegister(filters, loadAll = true).count().toLong()
  }

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
      it.status in listOf(Task.TaskStatus.INPROGRESS, Task.TaskStatus.READY) &&
        it.executionPeriod.hasStart() &&
        it.executionPeriod
          .start
          .before(Date())
          .or(it.executionPeriod.start.asDdMmmYyyy() == Date().asDdMmmYyyy())
    }
  }

  private suspend fun Patient.toTracingRegisterData(
    tasks: Iterable<Task>,
    listResource: ListResource?
  ): RegisterData.TracingRegisterData {
    val attempt =
      tracingRepository
        .getTracingAttempt(list = listResource)
        .copy(reasons = tasks.mapNotNull { task -> task.reasonCode?.codingFirstRep?.code })
    val oldestTaskDate = tasks.minOfOrNull { it.authoredOn }
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
      attempts = attempt.numberOfAttempts,
      lastAttemptDate = attempt.lastAttempt,
      firstAdded = oldestTaskDate,
      reasons = attempt.reasons
    )
  }

  private suspend fun Patient.toTracingRegisterData(): RegisterData.TracingRegisterData {
    val tasks = validTasks(this)
    val listResource = tracingRepository.getPatientListResource(this)
    return this.toTracingRegisterData(tasks, listResource)
  }
}
