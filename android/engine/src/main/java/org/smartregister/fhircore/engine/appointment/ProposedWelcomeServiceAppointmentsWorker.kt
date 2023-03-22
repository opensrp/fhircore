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

package org.smartregister.fhircore.engine.appointment

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import ca.uhn.fhir.rest.param.ParamPrefixEnum
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.get
import com.google.android.fhir.search.search
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import org.hl7.fhir.r4.model.Appointment
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.DateTimeType
import org.hl7.fhir.r4.model.IdType
import org.hl7.fhir.r4.model.ResourceType
import timber.log.Timber

@HiltWorker
class ProposedWelcomeServiceAppointmentsWorker
@AssistedInject
constructor(
  @Assisted val appContext: Context,
  @Assisted workerParameters: WorkerParameters,
  val fhirEngine: FhirEngine
) : CoroutineWorker(appContext, workerParameters) {
  val welcomeServiceCodeableConcept =
    CodeableConcept(Coding("https://d-tree.org", "Welcome", "Welcome Service")).apply {
      text = "Welcome Service"
    }

  override suspend fun doWork(): Result {
    Timber.i("Checking 'Welcome Service' appointments")

    val proposedAppointments =
      fhirEngine
        .search<Appointment> {
          filter(
            Appointment.STATUS,
            { value = of(Appointment.AppointmentStatus.PROPOSED.toCode()) }
          )
          filter(Appointment.REASON_CODE, { value = of(welcomeServiceCodeableConcept) })
          filter(
            Appointment.DATE,
            {
              value = of(DateTimeType.today())
              prefix = ParamPrefixEnum.LESSTHAN_OR_EQUALS
            }
          )
        }
        .filter {
          it.hasStart() &&
            it.hasSupportingInformation() &&
            it.supportingInformation.any { reference ->
              reference.referenceElement.resourceType == ResourceType.Appointment.name
            }
        }
        .map {
          val supportFinishVisitAppointmentRef =
            it.supportingInformation.first { reference ->
              reference.referenceElement.resourceType == ResourceType.Appointment.name
            }
          val supportFinishVisitAppointmentRefId =
            IdType(supportFinishVisitAppointmentRef.reference).idPart
          val supportFinishVisitAppointment =
            fhirEngine.get<Appointment>(supportFinishVisitAppointmentRefId)
          Pair(it, supportFinishVisitAppointment)
        }

    val proposedAppointmentsToCancel =
      proposedAppointments
        .filter {
          val finishVisitAppointment = it.second
          finishVisitAppointment.status == Appointment.AppointmentStatus.FULFILLED
        }
        .map { it.first }

    val proposedAppointmentsToBook =
      proposedAppointments
        .filter {
          val finishVisitAppointment = it.second
          finishVisitAppointment.status in
            arrayOf(Appointment.AppointmentStatus.NOSHOW, Appointment.AppointmentStatus.BOOKED)
        }
        .map { it.first }

    bookWelcomeService(proposedAppointmentsToBook.toTypedArray())
    cancelWelcomeService(proposedAppointmentsToCancel.toTypedArray())

    return Result.success()
  }

  private suspend fun bookWelcomeService(appointments: Array<Appointment>) {
    appointments.forEach { it.status = Appointment.AppointmentStatus.BOOKED }
    fhirEngine.update(*appointments)
  }

  private suspend fun cancelWelcomeService(appointments: Array<Appointment>) {
    appointments.forEach { it.status = Appointment.AppointmentStatus.CANCELLED }
    fhirEngine.update(*appointments)
  }

  companion object {
    const val NAME = "ProposedWelcomeServiceAppointmentsWorker"
  }
}
