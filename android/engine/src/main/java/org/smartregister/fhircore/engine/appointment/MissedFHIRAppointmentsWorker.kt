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
import com.google.android.fhir.search.search
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import org.hl7.fhir.r4.model.Appointment
import org.hl7.fhir.r4.model.DateTimeType
import org.joda.time.DateTime
import timber.log.Timber

@HiltWorker
class MissedFHIRAppointmentsWorker
@AssistedInject
constructor(
  @Assisted val appContext: Context,
  @Assisted workerParameters: WorkerParameters,
  val fhirEngine: FhirEngine
) : CoroutineWorker(appContext, workerParameters) {
  override suspend fun doWork(): Result {
    Timber.i("Checking missed Appointments")
    val missedAppointments =
      fhirEngine
        .search<Appointment> {
          filter(Appointment.STATUS, { value = of(Appointment.AppointmentStatus.BOOKED.toCode()) })
          filter(
            Appointment.DATE,
            {
              value = of(DateTimeType.today())
              prefix = ParamPrefixEnum.LESSTHAN
            }
          )
        }
        .filter {
          it.hasStart() &&
            it.start.before(DateTime().withTimeAtStartOfDay().toDate()) &&
            it.status == Appointment.AppointmentStatus.BOOKED
        }
        .toTypedArray()

    updateNoShow(missedAppointments)

    Timber.i("Updated ${missedAppointments.size} missed appointments")
    return Result.success()
  }

  private suspend fun updateNoShow(appointments: Array<Appointment>) {
    appointments.forEach { it.status = Appointment.AppointmentStatus.NOSHOW }
    fhirEngine.update(*appointments)
  }

  companion object {
    const val NAME = "MissedFHIRAppointmentsWorker"
  }
}
