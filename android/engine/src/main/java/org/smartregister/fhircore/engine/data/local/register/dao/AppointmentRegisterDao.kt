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

import ca.uhn.fhir.rest.param.ParamPrefixEnum
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.datacapture.extensions.logicalId
import com.google.android.fhir.search.Operation
import com.google.android.fhir.search.Order
import com.google.android.fhir.search.Search
import com.google.android.fhir.search.search
import java.util.Calendar
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.hl7.fhir.r4.model.Appointment
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.DateTimeType
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.app.ApplicationConfiguration
import org.smartregister.fhircore.engine.data.domain.PregnancyStatus
import org.smartregister.fhircore.engine.data.local.AppointmentRegisterFilter
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.data.local.RegisterFilter
import org.smartregister.fhircore.engine.domain.model.HealthStatus
import org.smartregister.fhircore.engine.domain.model.ProfileData
import org.smartregister.fhircore.engine.domain.model.RegisterData
import org.smartregister.fhircore.engine.domain.repository.RegisterDao
import org.smartregister.fhircore.engine.domain.util.PaginationConstant
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.engine.util.SharedPreferenceKey
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.extension.asReference
import org.smartregister.fhircore.engine.util.extension.extractAge
import org.smartregister.fhircore.engine.util.extension.extractHealthStatusFromMeta
import org.smartregister.fhircore.engine.util.extension.extractName
import org.smartregister.fhircore.engine.util.extension.extractOfficialIdentifier
import org.smartregister.fhircore.engine.util.extension.fetch
import org.smartregister.fhircore.engine.util.extension.getPregnancyStatus
import org.smartregister.fhircore.engine.util.extension.safeSubList

@Singleton
class AppointmentRegisterDao
@Inject
constructor(
  val fhirEngine: FhirEngine,
  val defaultRepository: DefaultRepository,
  val configurationRegistry: ConfigurationRegistry,
  val dispatcherProvider: DefaultDispatcherProvider,
  val sharedPreferencesHelper: SharedPreferencesHelper,
) : RegisterDao {

  private val currentPractitioner by lazy {
    sharedPreferencesHelper.read(
      key = SharedPreferenceKey.PRACTITIONER_ID.name,
      defaultValue = null,
    )
  }

  private fun Appointment.patientRef() =
    this.participant
      .firstOrNull { it.actor.referenceElement.resourceType == ResourceType.Patient.name }
      ?.actor

  private fun Appointment.practitionerRef() =
    this.participant
      .firstOrNull { it.actor.referenceElement.resourceType == ResourceType.Practitioner.name }
      ?.actor

  private fun applicationConfiguration(): ApplicationConfiguration =
    configurationRegistry.getAppConfigs()

  override suspend fun countRegisterData(): Flow<Long> {
    var counter = 0L
    var offset = 0
    val pageCount = 100
    val dateOfAppointment = Calendar.getInstance().time

    return flow {
      do {
        val appointments =
          fhirEngine.search<Appointment> {
            genericFilter(dateOfAppointment)
            from = offset
            count = pageCount
          }
        offset += appointments.size
        val validAppointmentCount =
          appointments.map { it.resource }.count { isAppointmentValid(it) }
        counter += validAppointmentCount
        emit(counter)
      } while (appointments.isNotEmpty())
    }
  }

  override suspend fun countRegisterFiltered(filters: RegisterFilter): Long =
    searchAppointments(filters, loadAll = true).count().toLong()

  private suspend fun patientCategoryMatches(
    appointment: Appointment,
    categories: Iterable<HealthStatus>,
  ): Boolean {
    val patient =
      appointment.patientRef()?.let { defaultRepository.loadResource(it) as Patient }
        ?: return false
    return patient.meta.tag.any { coding ->
      coding.code in categories.map { it.name.lowercase().replace("_", "-") }
    }
  }

  private suspend fun searchAppointments(
    filters: RegisterFilter,
    loadAll: Boolean,
    page: Int = -1,
  ): List<Appointment> {
    filters as AppointmentRegisterFilter

    val searchResults =
      fhirEngine.fetch<Appointment>(
        offset = max(page, 0) * PaginationConstant.DEFAULT_PAGE_SIZE,
        loadAll = loadAll,
      ) {
        genericFilter(
          dateOfAppointment = filters.dateOfAppointment,
          reasonCode = filters.reasonCode,
          patientCategory = filters.patientCategory,
          myPatients = filters.myPatients,
        )

        sort(Appointment.DATE, Order.ASCENDING)
      }

    val keySet = mutableSetOf<String>()
    return searchResults
      .mapNotNull {
        if (keySet.contains(it.resource.logicalId)) return@mapNotNull null
        keySet.add(it.resource.logicalId)
        it.resource
      }
      .filter {
        isAppointmentValid(
          appointment = it,
          reasonCode = filters.reasonCode,
          patientCategory = filters.patientCategory,
          myPatients = filters.myPatients,
        )
      }
  }

  private suspend fun isAppointmentValid(
    appointment: Appointment,
    reasonCode: CodeableConcept? = null,
    patientCategory: Iterable<HealthStatus>? = null,
    myPatients: Boolean = false,
  ): Boolean {
    val patientAssignmentFilter =
      !myPatients ||
        (appointment.practitionerRef()?.reference ==
          currentPractitioner?.asReference(ResourceType.Practitioner)?.reference)

    val patientCategoryFilter =
      patientCategory == null || (patientCategoryMatches(appointment, patientCategory))

    val appointmentReasonFilter =
      reasonCode == null ||
        (appointment.reasonCode
          .flatMap { cc -> cc.coding }
          .any { c -> c.code == reasonCode.coding.firstOrNull()?.code })

    return (appointment.status == Appointment.AppointmentStatus.BOOKED ||
      appointment.status == Appointment.AppointmentStatus.WAITLIST ||
      appointment.status == Appointment.AppointmentStatus.NOSHOW) &&
      appointment.hasStart() &&
      patientAssignmentFilter &&
      patientCategoryFilter &&
      appointmentReasonFilter &&
      appointment.patientRef() != null &&
      appointment.practitionerRef() != null
  }

  private fun Search.genericFilter(
    dateOfAppointment: Date? = null,
    reasonCode: CodeableConcept? = null,
    patientCategory: Iterable<HealthStatus>? = null,
    myPatients: Boolean = false,
  ) {
    filter(
      Appointment.STATUS,
      { value = of(Appointment.AppointmentStatus.BOOKED.toCode()) },
      { value = of(Appointment.AppointmentStatus.WAITLIST.toCode()) },
      { value = of(Appointment.AppointmentStatus.NOSHOW.toCode()) },
      operation = Operation.OR,
    )

    if (dateOfAppointment != null) {
      filter(
        Appointment.DATE,
        {
          value = of(DateTimeType(dateOfAppointment))
          prefix = ParamPrefixEnum.EQUAL
        },
      )
    }

    if (myPatients && currentPractitioner != null) {
      filter(
        Appointment.ACTOR,
        { value = currentPractitioner!!.asReference(ResourceType.Practitioner).reference },
      )
    }

    if (reasonCode != null) {
      filter(Appointment.REASON_CODE, { value = of(reasonCode) })
    }

    // TODO: look into this further
    //    if (patientCategory != null) {
    //      val patientTypeFilterTag =
    // applicationConfiguration().patientTypeFilterTagViaMetaCodingSystem
    //
    //      val paramQueries: List<(TokenParamFilterCriterion.() -> Unit)> =
    //        patientCategory.map { healthStatus ->
    //          val coding: Coding =
    //            Coding().apply {
    //              system = patientTypeFilterTag
    //              code = healthStatus.name.lowercase().replace("_", "-")
    //            }
    //          return@map { value = of(coding) }
    //        }
    //
    //      has<Patient>(Appointment.PATIENT) {
    //        filter(TokenClientParam("_tag"), *paramQueries.toTypedArray(), operation =
    // Operation.OR)
    //      }
    //    }
  }

  private suspend fun transformAppointment(appointment: Appointment): RegisterData {
    val refPatient = appointment.patientRef()!!
    val patient = defaultRepository.loadResource(refPatient) as Patient
    val pregnancyStatus = defaultRepository.getPregnancyStatus(patient.logicalId)

    return RegisterData.AppointmentRegisterData(
      logicalId = patient.logicalId,
      appointmentLogicalId = appointment.logicalId,
      name = patient.extractName(),
      identifier = patient.extractOfficialIdentifier(),
      gender = patient.gender,
      age = patient.extractAge(),
      healthStatus =
        patient.extractHealthStatusFromMeta(
          applicationConfiguration().patientTypeFilterTagViaMetaCodingSystem,
        ),
      isPregnant = pregnancyStatus == PregnancyStatus.Pregnant,
      isBreastfeeding = pregnancyStatus == PregnancyStatus.BreastFeeding,
      reasons = appointment.reasonCode.flatMap { cc -> cc.coding.map { coding -> coding.code } },
    )
  }

  override suspend fun loadRegisterFiltered(
    currentPage: Int,
    loadAll: Boolean,
    filters: RegisterFilter,
  ): List<RegisterData> =
    searchAppointments(filters, loadAll = true)
      .map { transformAppointment(it) }
      .sortedWith(
        nullsFirst(
          compareBy {
            it as RegisterData.AppointmentRegisterData
            it.identifier
          },
        ),
      )
      .let {
        if (!loadAll) {
          val from = currentPage * PaginationConstant.DEFAULT_PAGE_SIZE
          val to = from + PaginationConstant.DEFAULT_PAGE_SIZE + PaginationConstant.EXTRA_ITEM_COUNT
          it.safeSubList(from..to)
        } else {
          it
        }
      }

  override suspend fun loadRegisterData(
    currentPage: Int,
    loadAll: Boolean,
    appFeatureName: String?,
  ): List<RegisterData> {
    val appointments =
      fhirEngine.fetch<Appointment> {
        filter(Appointment.STATUS, { value = of(Appointment.AppointmentStatus.BOOKED.toCode()) })
        filter(Appointment.DATE, { value = of(DateTimeType.today()) })
      }
    val keySet = mutableSetOf<String>()
    return appointments
      .mapNotNull {
        if (keySet.contains(it.resource.logicalId)) return@mapNotNull null
        keySet.add(it.resource.logicalId)
        it.resource
      }
      .filter {
        it.status == Appointment.AppointmentStatus.BOOKED &&
          it.hasStart() &&
          it.patientRef() != null &&
          it.practitionerRef() != null
      }
      .map { transformAppointment(it) }
      .sortedWith(
        nullsFirst(
          compareBy {
            it as RegisterData.AppointmentRegisterData
            it.identifier
          },
        ),
      )
      .let {
        if (!loadAll) {
          val from = currentPage * PaginationConstant.DEFAULT_PAGE_SIZE
          val to = from + PaginationConstant.DEFAULT_PAGE_SIZE + PaginationConstant.EXTRA_ITEM_COUNT
          it.safeSubList(from..to)
        } else {
          it
        }
      }
  }

  override suspend fun loadProfileData(appFeatureName: String?, resourceId: String): ProfileData? {
    TODO()
  }
}
