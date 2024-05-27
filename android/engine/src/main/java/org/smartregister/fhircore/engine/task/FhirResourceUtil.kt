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
import ca.uhn.fhir.rest.gclient.TokenClientParam
import ca.uhn.fhir.rest.param.ParamPrefixEnum
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.get
import com.google.android.fhir.search.Operation
import com.google.android.fhir.search.search
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import java.time.ZoneId
import java.util.Calendar
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton
import org.hl7.fhir.r4.model.Appointment
import org.hl7.fhir.r4.model.CarePlan
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.DateTimeType
import org.hl7.fhir.r4.model.IdType
import org.hl7.fhir.r4.model.Meta
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Period
import org.hl7.fhir.r4.model.Reference
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.ResourceType
import org.hl7.fhir.r4.model.Task
import org.joda.time.DateTime
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.domain.model.HealthStatus
import org.smartregister.fhircore.engine.util.ReasonConstants
import org.smartregister.fhircore.engine.util.SharedPreferenceKey
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.SystemConstants
import org.smartregister.fhircore.engine.util.extension.activeCarePlans
import org.smartregister.fhircore.engine.util.extension.asReference
import org.smartregister.fhircore.engine.util.extension.extractHealthStatusFromMeta
import org.smartregister.fhircore.engine.util.extension.extractId
import org.smartregister.fhircore.engine.util.extension.getCarePlanId
import org.smartregister.fhircore.engine.util.extension.hasPastEnd
import org.smartregister.fhircore.engine.util.extension.plusDays
import org.smartregister.fhircore.engine.util.extension.referenceValue
import org.smartregister.fhircore.engine.util.extension.toCoding
import timber.log.Timber

@Singleton
class FhirResourceUtil
@Inject
constructor(
  @ApplicationContext val appContext: Context,
  private val fhirEngine: FhirEngine,
  val sharedPreferencesHelper: SharedPreferencesHelper,
  val defaultRepository: DefaultRepository,
) {

  private val currentPractitioner by lazy {
    sharedPreferencesHelper.read(
      key = SharedPreferenceKey.PRACTITIONER_ID.name,
      defaultValue = null,
    )
  }

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

        val carePlanId = task.getCarePlanId()

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
    val tracingTasksToAdd = mutableListOf<Task>()
    val missedAppointments =
      fhirEngine
        .search<Appointment> {
          filter(
            Appointment.STATUS,
            { value = of(Appointment.AppointmentStatus.BOOKED.toCode()) },
            { value = of(Appointment.AppointmentStatus.WAITLIST.toCode()) },
            operation = Operation.OR,
          )
          filter(
            Appointment.DATE,
            {
              value = of(DateTimeType.today())
              prefix = ParamPrefixEnum.LESSTHAN
            },
          )
        }
        .mapNotNull {
          val appointment = it.resource

          if (
            !(appointment.hasStart() &&
              appointment.start.before(DateTime().withTimeAtStartOfDay().toDate()) &&
              (appointment.status == Appointment.AppointmentStatus.BOOKED ||
                appointment.status == Appointment.AppointmentStatus.WAITLIST))
          ) {
            return@mapNotNull null
          }

          val today = LocalDate.now()
          val missedAppointmentInRange =
            LocalDate.from(appointment.start.plusDays(7).toInstant().atZone(ZoneId.systemDefault()))
              .let { missedAppointmentDate ->
                today.isAfter(missedAppointmentDate) || today.isEqual(missedAppointmentDate)
              }
          val missedMilestoneInRange =
            LocalDate.from(appointment.start.plusDays(1).toInstant().atZone(ZoneId.systemDefault()))
              .let { missedMilestoneDate ->
                today.isAfter(missedMilestoneDate) || today.isEqual(missedMilestoneDate)
              }

          if ((missedAppointmentInRange) || (missedMilestoneInRange)) {
            if (missedMilestoneInRange) {
              appointment.status = Appointment.AppointmentStatus.WAITLIST
              tracingTasksToAdd.addAll(addMissedAppointment(appointment, true))
            }
            if(missedAppointmentInRange) {
              appointment.status = Appointment.AppointmentStatus.NOSHOW
              tracingTasksToAdd.addAll(addMissedAppointment(appointment, false))
            }
          } else {
            appointment.status = Appointment.AppointmentStatus.WAITLIST
          }

          appointment
        }

    if (tracingTasksToAdd.isNotEmpty()) {
      defaultRepository.create(addResourceTags = true, *tracingTasksToAdd.toTypedArray())
    }

    if (missedAppointments.isNotEmpty()) {
      fhirEngine.update(*missedAppointments.toTypedArray())
    }

    Timber.i(
      "Updated ${missedAppointments.size} missed appointments, created tracing tasks: ${tracingTasksToAdd.size}",
    )
  }

  suspend fun handleWelcomeServiceAppointmentWorker() {
    Timber.i("Checking 'Welcome Service' appointments")
    val tracingTasks = mutableListOf<Task>()

    val proposedAppointments =
      fhirEngine
        .search<Appointment> {
          filter(
            Appointment.STATUS,
            { value = of(Appointment.AppointmentStatus.PROPOSED.toCode()) },
          )
          filter(Appointment.REASON_CODE, { value = of(ReasonConstants.WelcomeServiceCode) })
          filter(
            Appointment.DATE,
            {
              value = of(DateTimeType.today())
              prefix = ParamPrefixEnum.LESSTHAN_OR_EQUALS
            },
          )
        }
        .map { it.resource }
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
          val isValid =
            finishVisitAppointment.status in
              arrayOf(Appointment.AppointmentStatus.NOSHOW, Appointment.AppointmentStatus.BOOKED)

          if (isValid) {
            addToTracingList(
                finishVisitAppointment,
                ReasonConstants.interruptedTreatmentTracingCode,
              )
              ?.let { task -> tracingTasks.add(task) }
          }

          isValid
        }
        .map { it.first }

    defaultRepository.create(addResourceTags = true, *tracingTasks.toTypedArray())
    bookWelcomeService(proposedAppointmentsToBook.toTypedArray())
    cancelWelcomeService(proposedAppointmentsToCancel.toTypedArray())

    Timber.i(
      "proposedAppointmentsToBook: ${proposedAppointmentsToBook.size}, proposedAppointmentsToCancel: ${proposedAppointmentsToCancel.size}, tracing tasks: ${tracingTasks.size}",
    )
  }

  private suspend fun bookWelcomeService(appointments: Array<Appointment>) {
    appointments.forEach { it.status = Appointment.AppointmentStatus.BOOKED }
    fhirEngine.update(*appointments)
  }

  private suspend fun cancelWelcomeService(appointments: Array<Appointment>) {
    appointments.forEach { it.status = Appointment.AppointmentStatus.CANCELLED }
    fhirEngine.update(*appointments)
  }

  private suspend fun addMissedAppointment(
    appointment: Appointment,
    isMilestoneAppointment: Boolean,
  ): List<Task> {
    val tracingTasks = mutableListOf<Task>()
    val patient = getPatient(appointment) ?: return listOf()
    val isEID =
      patient.extractHealthStatusFromMeta(
        SystemConstants.PATIENT_TYPE_FILTER_TAG_VIA_META_CODINGS_SYSTEM,
      ) == HealthStatus.EXPOSED_INFANT

    if (isEID && isMilestoneAppointment) {
      val carePlan = patient.activeCarePlans(fhirEngine).firstOrNull()
      val hasMileStoneTest =
        carePlan?.activity?.firstOrNull {
          it.hasDetail() &&
            it.detail.code.codingFirstRep.code == "Questionnaire/exposed-infant-milestone-hiv-test"
        } != null
      val milestoneTracingTaskDoesNotExist =
        fhirEngine
          .search<Task> {
            filter(
              TokenClientParam("_tag"),
              { value = of(ReasonConstants.homeTracingCoding) },
              { value = of(ReasonConstants.phoneTracingCoding) },
            )
            filter(Task.CODE, { value = of(ReasonConstants.missedMilestoneAppointmentTracingCode) })
            filter(
              Task.STATUS,
              { value = of(Task.TaskStatus.READY.toCode()) },
              { value = of(Task.TaskStatus.INPROGRESS.toCode()) },
              operation = Operation.OR,
            )
          }
          .isEmpty()
      if (hasMileStoneTest && milestoneTracingTaskDoesNotExist) {
        addToTracingList(appointment, ReasonConstants.missedMilestoneAppointmentTracingCode)?.let {
          tracingTasks.add(it)
        }
      }
    }
    if(!isMilestoneAppointment) {
      addToTracingList(
        appointment,
        if (isEID) {
          ReasonConstants.missedRoutineAppointmentTracingCode
        } else ReasonConstants.missedAppointmentTracingCode,
      )
        ?.let { tracingTasks.add(it) }
    }

    return tracingTasks
  }

  private suspend fun addToTracingList(appointment: Appointment, coding: Coding): Task? {
    val patient = getPatient(appointment) ?: return null
    return createTracingTask(
      patient,
      currentPractitioner!!.asReference(ResourceType.Practitioner),
      coding,
    )
  }

  private fun createTracingTask(patient: Patient, practitioner: Reference, coding: Coding): Task {
    val hasPhone = patient.hasTelecom()
    val now = Calendar.getInstance().time
    return Task().apply {
      meta =
        Meta().apply {
          tag =
            mutableListOf(
              if (hasPhone) {
                ReasonConstants.phoneTracingCoding
              } else ReasonConstants.homeTracingCoding,
            )
        }
      status = Task.TaskStatus.READY
      intent = Task.TaskIntent.PLAN
      priority = Task.TaskPriority.ROUTINE
      authoredOn = now
      lastModified = now
      code =
        CodeableConcept(
          Coding("http://snomed.info/sct", "225368008", "Contact tracing (procedure)"),
        )
      `for` = patient.asReference()
      owner = practitioner
      executionPeriod = Period().apply { start = now }
      reasonCode =
        CodeableConcept(
            coding,
          )
          .apply { text = coding.display }
    }
  }

  private suspend fun getPatient(appointment: Appointment): Patient? {
    try {
      val patientRef =
        appointment.participant
          .firstOrNull { it.hasActor() && it.actor.reference.contains(ResourceType.Patient.name) }
          ?.actor
          ?.extractId() ?: return null
      return fhirEngine.get<Patient>(patientRef)
    } catch (e: Exception) {
      return null
    }
  }
}
