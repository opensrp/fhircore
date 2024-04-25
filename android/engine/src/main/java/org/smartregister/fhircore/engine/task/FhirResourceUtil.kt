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

package org.smartregister.fhircore.engine.task

import android.content.Context
import ca.uhn.fhir.rest.param.ParamPrefixEnum
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.get
import com.google.android.fhir.search.search
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton
import org.hl7.fhir.r4.model.Appointment
import org.hl7.fhir.r4.model.CarePlan
import org.hl7.fhir.r4.model.DateTimeType
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.Task
import org.joda.time.DateTime
import org.smartregister.fhircore.engine.util.SystemConstants
import org.smartregister.fhircore.engine.util.extension.hasPastEnd
import org.smartregister.fhircore.engine.util.extension.referenceValue
import org.smartregister.fhircore.engine.util.extension.toCoding
import timber.log.Timber

@Singleton
class FhirResourceUtil
@Inject
constructor(
  @ApplicationContext val appContext: Context,
  private val fhirEngine: FhirEngine,
) {
  suspend fun expireOverdueTasks() {
    Timber.i("Starting task scheduler")
    val carePlanMap = mutableMapOf<String, CarePlan>()
    val tasksToUpdate = mutableListOf<Resource>()

    fhirEngine
      .search<Task> {
        filter(
          Task.STATUS,
          { value = of(Task.TaskStatus.REQUESTED.toCoding()) },
          { value = of(Task.TaskStatus.READY.toCoding()) },
          { value = of(Task.TaskStatus.ACCEPTED.toCoding()) },
          { value = of(Task.TaskStatus.INPROGRESS.toCoding()) },
          { value = of(Task.TaskStatus.RECEIVED.toCoding()) },
        )

        filter(
          Task.PERIOD,
          {
            prefix = ParamPrefixEnum.ENDS_BEFORE
            value = of(DateTimeType(Date()))
          },
        )
      }
      .map { it.resource }
      .filter {
        it.hasPastEnd() &&
          it.status in
            arrayOf(
              Task.TaskStatus.REQUESTED,
              Task.TaskStatus.READY,
              Task.TaskStatus.ACCEPTED,
              Task.TaskStatus.INPROGRESS,
              Task.TaskStatus.RECEIVED,
            )
      }
      .onEach { task ->
        task.status = Task.TaskStatus.FAILED

        val carePlanId =
          task.meta.tag
            .firstOrNull { it.system == SystemConstants.CARE_PLAN_REFERENCE_SYSTEM }
            ?.code
            ?.substringAfterLast(delimiter = '/', missingDelimiterValue = "")

        val carePlan =
          carePlanId?.let { id ->
            if (carePlanMap.containsKey(id)) {
              carePlanMap[id]
            } else {
              runCatching { fhirEngine.get<CarePlan>(id) }.getOrNull()
            }
          }

        if (carePlan != null) {
          kotlin
            .runCatching {
              val index =
                carePlan.activity.indexOfFirst { activity ->
                  activity.outcomeReference.firstOrNull()?.reference == task.referenceValue()
                }
              if (index != -1) {
                val item = carePlan.activity?.get(index)
                item?.detail?.status = CarePlan.CarePlanActivityStatus.STOPPED
                carePlan.activity[index] = item
                Timber.d("Updating carePlan: ${carePlan.referenceValue()}")
                carePlanMap[carePlanId] = carePlan
              }
            }
            .onFailure {
              Timber.e(
                "${carePlan.referenceValue()} CarePlan was not found. In consistent data ${it.message}",
              )
            }
        }

        Timber.d("Updating task: ${task.referenceValue()}")
        tasksToUpdate.add(task)
      }

    Timber.d("Going to expire tasks = ${tasksToUpdate.size}  and carePlans = ${carePlanMap.size}")
    fhirEngine.update(*(tasksToUpdate + carePlanMap.values).toTypedArray())

    Timber.i("Done task scheduling")
  }

  suspend fun handleMissedAppointment() {
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
            },
          )
        }
        .map { it.resource }
        .filter {
          it.hasStart() &&
            it.start.before(DateTime().withTimeAtStartOfDay().toDate()) &&
            it.status == Appointment.AppointmentStatus.BOOKED
        }
        .toTypedArray()

    updateNoShow(missedAppointments)

    Timber.i("Updated ${missedAppointments.size} missed appointments")
  }

  private suspend fun updateNoShow(appointments: Array<Appointment>) {
    appointments.forEach { it.status = Appointment.AppointmentStatus.NOSHOW }
    fhirEngine.update(*appointments)
  }
}
