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
import com.google.android.fhir.search.count
import com.google.android.fhir.search.search
import javax.inject.Inject
import javax.inject.Singleton
import org.hl7.fhir.r4.model.Appointment
import org.hl7.fhir.r4.model.Identifier
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.appfeature.model.HealthModule
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.app.AppConfigClassification
import org.smartregister.fhircore.engine.configuration.app.ApplicationConfiguration
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.domain.model.ProfileData
import org.smartregister.fhircore.engine.domain.model.RegisterData
import org.smartregister.fhircore.engine.domain.repository.RegisterDao
import org.smartregister.fhircore.engine.domain.util.PaginationConstant
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.engine.util.extension.extractAge
import org.smartregister.fhircore.engine.util.extension.extractHealthStatusFromMeta
import org.smartregister.fhircore.engine.util.extension.extractName

@Singleton
class AppointmentRegisterDao
@Inject
constructor(
  val fhirEngine: FhirEngine,
  val defaultRepository: DefaultRepository,
  val configurationRegistry: ConfigurationRegistry,
  val dispatcherProvider: DefaultDispatcherProvider
) : RegisterDao {

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
    return fhirEngine.count<Appointment> {
      filter(Appointment.STATUS, { value = of(Appointment.AppointmentStatus.BOOKED.toCode()) })
    }
  }

  override suspend fun loadRegisterData(
    currentPage: Int,
    loadAll: Boolean,
    appFeatureName: String?
  ): List<RegisterData> {
    val appointments =
      fhirEngine.search<Appointment> {
        filter(Appointment.STATUS, { value = of(Appointment.AppointmentStatus.BOOKED.toCode()) })
        if (!loadAll) count = PaginationConstant.DEFAULT_PAGE_SIZE
        from = currentPage * PaginationConstant.DEFAULT_PAGE_SIZE
      }

    return appointments
      .filter { it.hasStart() && it.patientRef() != null && it.practitionerRef() != null }
      .map {
        val refPatient = it.patientRef()!!
        val patient = defaultRepository.loadResource(refPatient) as Patient

        RegisterData.AppointmentRegisterData(
          logicalId = patient.logicalId,
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
          reasons = it.reasonCode.flatMap { cc -> cc.coding.map { coding -> coding.code } }
        )
      }
  }

  override suspend fun loadProfileData(appFeatureName: String?, resourceId: String): ProfileData? {
    TODO()
  }

  fun getRegisterDataFilters() =
    configurationRegistry.retrieveDataFilterConfiguration(HealthModule.APPOINTMENT.name)
}
