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
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import ca.uhn.fhir.rest.gclient.TokenClientParam
import ca.uhn.fhir.rest.param.ParamPrefixEnum
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.get
import com.google.android.fhir.search.Operation
import com.google.android.fhir.search.filter.TokenParamFilterCriterion
import com.google.android.fhir.search.search
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.Date
import java.util.UUID
import kotlinx.coroutines.withContext
import org.hl7.fhir.r4.model.CarePlan
import org.hl7.fhir.r4.model.CodeType
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.DateTimeType
import org.hl7.fhir.r4.model.IdType
import org.hl7.fhir.r4.model.Identifier
import org.hl7.fhir.r4.model.Meta
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Reference
import org.hl7.fhir.r4.model.Task
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.ReasonConstants
import org.smartregister.fhircore.engine.util.extension.asDdMmmYyyy
import org.smartregister.fhircore.engine.util.extension.asReference
import org.smartregister.fhircore.engine.util.extension.referenceValue
import timber.log.Timber

// TODO: Programs needs to revisit workflow, combine with
// ProposedWelcomeServiceAppointmentsWorker.kt
@HiltWorker
class WelcomeServiceBackToCarePlanWorker
@AssistedInject
constructor(
  @Assisted val appContext: Context,
  @Assisted workerParameters: WorkerParameters,
  val fhirEngine: FhirEngine,
  val dispatcherProvider: DispatcherProvider,
) : CoroutineWorker(appContext, workerParameters) {

  override suspend fun doWork(): Result {
    return try {
      withContext(dispatcherProvider.singleThread()) {
        updateCarePlanWithWelcomeService()
        Result.success()
      }
    } catch (e: Exception) {
      Timber.e(e)
      Result.failure()
    }
  }

  private suspend fun getInterruptedTreatmentTasks(page: Int) =
    fhirEngine
      .search<Task> {
        filter(
          TokenClientParam("_tag"),
          *tracingTagsTokenParamFilterCriterion,
          operation = Operation.OR,
        )
        filter(
          Task.STATUS,
          { value = of(Task.TaskStatus.READY.toCode()) },
          { value = of(Task.TaskStatus.INPROGRESS.toCode()) },
          operation = Operation.OR,
        )
        filter(
          Task.PERIOD,
          {
            value = of(DateTimeType.now())
            prefix = ParamPrefixEnum.GREATERTHAN
          },
        )
        from = page * PAGE_COUNT
        count = PAGE_COUNT
      }
      .map { it.resource }
      .filter { it.reasonCode.coding.any { coding -> coding.code == INTERRUPTED_TREAT_CODE } }
      .filter {
        it.status in listOf(Task.TaskStatus.INPROGRESS, Task.TaskStatus.READY) &&
          it.executionPeriod.hasStart() &&
          it.executionPeriod.start
            .before(Date())
            .or(it.executionPeriod.start.asDdMmmYyyy() == Date().asDdMmmYyyy())
      }

  private suspend fun processInterruptedTreatmentTasks(tasks: List<Task>) {
    tasks
      .groupBy { it.`for`.reference }
      .forEach { (t, u) ->
        val patientId = IdType(t).idPart
        val patient = fhirEngine.get<Patient>(patientId)
        val carePlan = patient.activeCarePlan() ?: return@forEach
        val carePlanHasWelcomeService =
          carePlan.activity.any { act ->
            act.detail.code.coding.any { it.code == WELCOME_SERVICE_QUESTIONNAIRE_ID }
          }
        if (carePlanHasWelcomeService) return@forEach

        carePlan.apply {
          val taskId = UUID.randomUUID().toString()
          val taskDescription = "Welcome Service"

          val task = welcomeServiceTask(taskId, taskDescription, patient)
          addActivity().apply {
            addOutcomeReference(task.asReference().apply { display = taskDescription })

            detail =
              CarePlan.CarePlanActivityDetailComponent().apply {
                status = CarePlan.CarePlanActivityStatus.INPROGRESS
                kind = CarePlan.CarePlanActivityKind.TASK
                description = taskDescription
                code =
                  CodeableConcept(
                    Coding("https://d-tree.org", WELCOME_SERVICE_QUESTIONNAIRE_ID, taskDescription),
                  )
                scheduled = period.copy().apply { start = DateTimeType.now().value }
                addPerformer(author)
              }
          }
          fhirEngine.create(task)
          fhirEngine.update(this)
        }
      }
  }

  private suspend fun updateCarePlanWithWelcomeService() {
    var start = 0
    do {
      val tasks = getInterruptedTreatmentTasks(start++)
      processInterruptedTreatmentTasks(tasks)
    } while (tasks.isNotEmpty())
  }

  private suspend fun Patient.activeCarePlan() =
    fhirEngine
      .search<CarePlan> {
        filter(CarePlan.SUBJECT, { value = this@activeCarePlan.referenceValue() })
        filter(CarePlan.STATUS, { value = of(CarePlan.CarePlanStatus.ACTIVE.toCode()) })
      }
      .map { it.resource }
      .filter { it.period?.start != null }
      .filter { it.status == CarePlan.CarePlanStatus.ACTIVE }
      .sortedByDescending { it.period.start }
      .firstOrNull()

  companion object {
    const val NAME: String = "WelcomeServiceBackToCarePlanWorker"
    const val INTERRUPTED_TREAT_CODE = "interrupt-treat"
    const val WELCOME_SERVICE_QUESTIONNAIRE_ID = "art-client-welcome-service-back-to-care"

    private val tracingCodingTags =
      arrayOf<Coding>(
        ReasonConstants.homeTracingCoding,
        ReasonConstants.phoneTracingCoding,
      )

    private val tracingTagsTokenParamFilterCriterion =
      tracingCodingTags
        .map<Coding, TokenParamFilterCriterion.() -> Unit> {
          return@map { value = of(CodeType(it.code)) }
        }
        .toTypedArray()

    const val PAGE_COUNT = 500
  }
}

internal fun CarePlan.welcomeServiceTask(
  taskId: String,
  taskDescription: String,
  patient: Patient,
) =
  Task().apply {
    status = Task.TaskStatus.READY
    intent = Task.TaskIntent.PLAN
    priority = Task.TaskPriority.ROUTINE
    description = taskDescription
    val dateNow = DateTimeType.now().value
    authoredOn = dateNow
    lastModified = dateNow
    `for` = patient.asReference()
    executionPeriod = period.copy().apply { start = dateNow }
    requester = author
    owner = author
    addIdentifier(
      Identifier().apply {
        value = taskId
        use = Identifier.IdentifierUse.OFFICIAL
      },
    )
    id = taskId
    meta =
      Meta()
        .addTag(
          Coding().apply {
            system = "https://d-tree.org"
            code = "clinic-visit-task-order-11"
          },
        )
    reasonReference =
      Reference().apply {
        reference = "Questionnaire/art-client-welcome-service-back-to-care"
        display = taskDescription
      }
  }
