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
import com.google.android.fhir.logicalId
import com.google.android.fhir.search.search
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton
import org.hl7.fhir.r4.model.Appointment
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.DateTimeType
import org.hl7.fhir.r4.model.Identifier
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Practitioner
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.appfeature.model.HealthModule
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.app.AppConfigClassification
import org.smartregister.fhircore.engine.configuration.app.ApplicationConfiguration
import org.smartregister.fhircore.engine.data.local.AppointmentRegisterFilter
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.data.local.RegisterFilter
import org.smartregister.fhircore.engine.domain.model.HealthStatus
import org.smartregister.fhircore.engine.domain.model.ProfileData
import org.smartregister.fhircore.engine.domain.model.RegisterData
import org.smartregister.fhircore.engine.domain.repository.RegisterDao
import org.smartregister.fhircore.engine.domain.util.PaginationConstant
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.engine.util.LOGGED_IN_PRACTITIONER
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.extension.asReference
import org.smartregister.fhircore.engine.util.extension.extractAge
import org.smartregister.fhircore.engine.util.extension.extractHealthStatusFromMeta
import org.smartregister.fhircore.engine.util.extension.extractName
import org.smartregister.fhircore.engine.util.extension.safeSubList
import org.smartregister.fhircore.engine.util.extension.toHealthStatus

@Singleton
class AppointmentRegisterDao
@Inject
constructor(
  val fhirEngine: FhirEngine,
  val defaultRepository: DefaultRepository,
  val configurationRegistry: ConfigurationRegistry,
  val dispatcherProvider: DefaultDispatcherProvider,
  val sharedPreferencesHelper: SharedPreferencesHelper
) : RegisterDao {

  private val currentPractitioner by lazy {
    sharedPreferencesHelper.read<Practitioner>(key = LOGGED_IN_PRACTITIONER, decodeWithGson = true)
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
    configurationRegistry.retrieveConfiguration(AppConfigClassification.APPLICATION)

  override suspend fun countRegisterData(appFeatureName: String?): Long {
    return fhirEngine
      .search<Appointment> {
        filter(Appointment.STATUS, { value = of(Appointment.AppointmentStatus.BOOKED.toCode()) })
        filter(Appointment.DATE, { value = of(DateTimeType.today()) })
      }
      .count {
        it.status == Appointment.AppointmentStatus.BOOKED &&
          it.hasStart() &&
          it.patientRef() != null &&
          it.practitionerRef() != null
      }
      .toLong()
  }

  override suspend fun countRegisterFiltered(appFeatureName: String?, filters: RegisterFilter) =
    searchAppointments(filters, loadAll = true).count().toLong()

  private suspend fun patientCategoryMatches(
    appointment: Appointment,
    categories: Iterable<HealthStatus>
  ): Boolean {
    val patient =
      appointment.patientRef()?.let { defaultRepository.loadResource(it) as Patient }
        ?: return false
    return patient.meta.tag.any { coding -> coding.toHealthStatus() in categories }
  }

  private suspend fun searchAppointments(
    filters: RegisterFilter,
    loadAll: Boolean,
    page: Int = -1
  ): List<Appointment> {
    filters as AppointmentRegisterFilter
    val searchResults =
      fhirEngine.search<Appointment> {
        if (!loadAll) count = PaginationConstant.DEFAULT_PAGE_SIZE

        if (page >= 0) from = page * PaginationConstant.DEFAULT_PAGE_SIZE

        filter(Appointment.STATUS, { value = of(Appointment.AppointmentStatus.BOOKED.toCode()) })

        filter(
          Appointment.DATE,
          {
            value = of(DateTimeType(filters.dateOfAppointment))
            prefix = ParamPrefixEnum.GREATERTHAN_OR_EQUALS
          }
        )
        filter(
          Appointment.DATE,
          {
            value = of(DateTimeType(filters.dateOfAppointment).apply { add(Calendar.DATE, 1) })
            prefix = ParamPrefixEnum.LESSTHAN
          }
        )

        //        if (filters.myPatients && currentPractititoner != null)
        //          filter(Appointment.PRACTITIONER, { value =
        // currentPractititoner!!.referenceValue() })
        //
        //        filters.patientCategory?.let {
        //          val paramQueries: List<(TokenParamFilterCriterion.() -> Unit)> =
        //            it.flatMap { healthStatus ->
        //              val coding: Coding =
        //                Coding().apply {
        //                  system = "https://d-tree.org"
        //                  code = healthStatus.name.lowercase().replace("_", "-")
        //                }
        //              val alternativeCoding: Coding =
        //                Coding().apply {
        //                  system = "https://d-tree.org"
        //                  code = healthStatus.name.lowercase()
        //                }
        //
        //              return@flatMap listOf<Coding>(coding, alternativeCoding).map<
        //                Coding, TokenParamFilterCriterion.() -> Unit> { c -> { value = of(c) } }
        //            }
        //
        //          has<Patient>(Appointment.PATIENT){
        //            filter(TokenClientParam("_tag"), *paramQueries.toTypedArray(), operation =
        // Operation.OR)
        //          }
        //        }

        filters.reasonCode?.let {
          val codeableConcept =
            CodeableConcept().apply {
              addCoding(
                Coding().apply {
                  system = "https://d-tree.org"
                  code = it
                }
              )
            }
          filter(Appointment.REASON_CODE, { value = of(codeableConcept) })
        }
      }

    return searchResults.filter {
      val patientAssignmentFilter =
        !filters.myPatients ||
          (it.practitionerRef()?.reference == currentPractitioner?.asReference()?.reference)
      val patientCategoryFilter =
        filters.patientCategory == null || (patientCategoryMatches(it, filters.patientCategory))

      val appointmentReasonFilter =
        filters.reasonCode == null ||
          (it.reasonCode.flatMap { cc -> cc.coding }.any { c -> c.code == filters.reasonCode })
      it.status == Appointment.AppointmentStatus.BOOKED &&
        it.hasStart() &&
        patientAssignmentFilter &&
        patientCategoryFilter &&
        appointmentReasonFilter
    }
  }

  private suspend fun transformAppointment(appointment: Appointment): RegisterData {
    val refPatient = appointment.patientRef()!!
    val patient = defaultRepository.loadResource(refPatient) as Patient

    return RegisterData.AppointmentRegisterData(
      logicalId = appointment.logicalId,
      name = patient.extractName(),
      identifier =
        patient.identifier
          .firstOrNull { identifier -> identifier.use == Identifier.IdentifierUse.OFFICIAL }
          ?.value,
      gender = patient.gender,
      age = patient.extractAge(),
      healthStatus =
        patient.extractHealthStatusFromMeta(
          applicationConfiguration().patientTypeFilterTagViaMetaCodingSystem
        ),
      isPregnant = defaultRepository.isPatientPregnant(patient),
      isBreastfeeding = defaultRepository.isPatientBreastfeeding(patient),
      reasons = appointment.reasonCode.flatMap { cc -> cc.coding.map { coding -> coding.code } }
    )
  }

  override suspend fun loadRegisterFiltered(
    currentPage: Int,
    loadAll: Boolean,
    appFeatureName: String?,
    filters: RegisterFilter
  ): List<RegisterData> =
    searchAppointments(filters, loadAll = true)
      .map { transformAppointment(it) }
      .sortedWith(
        nullsFirst(
          compareBy {
            it as RegisterData.AppointmentRegisterData
            it.identifier
          }
        )
      )
      .let {
        if (!loadAll) {
          val from = currentPage * PaginationConstant.DEFAULT_PAGE_SIZE
          val to = from + PaginationConstant.DEFAULT_PAGE_SIZE
          it.safeSubList(from..to)
        } else it
      }

  override suspend fun loadRegisterData(
    currentPage: Int,
    loadAll: Boolean,
    appFeatureName: String?
  ): List<RegisterData> {
    val appointments =
      fhirEngine.search<Appointment> {
        filter(Appointment.STATUS, { value = of(Appointment.AppointmentStatus.BOOKED.toCode()) })
        filter(Appointment.DATE, { value = of(DateTimeType.today()) })
      }

    return appointments
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
          }
        )
      )
      .let {
        if (!loadAll) {
          val from = currentPage * PaginationConstant.DEFAULT_PAGE_SIZE
          val to = from + PaginationConstant.DEFAULT_PAGE_SIZE
          it.safeSubList(from..to)
        } else it
      }
  }

  override suspend fun loadProfileData(appFeatureName: String?, resourceId: String): ProfileData? {
    TODO()
  }

  fun getRegisterDataFilters() =
    configurationRegistry.retrieveDataFilterConfiguration(HealthModule.APPOINTMENT.name)
}
