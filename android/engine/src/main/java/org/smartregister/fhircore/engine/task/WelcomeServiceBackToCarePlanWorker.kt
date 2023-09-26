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
import org.hl7.fhir.r4.model.CarePlan
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.DateTimeType
import org.hl7.fhir.r4.model.IdType
import org.hl7.fhir.r4.model.Identifier
import org.hl7.fhir.r4.model.Meta
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Reference
import org.hl7.fhir.r4.model.Task
import org.smartregister.fhircore.engine.util.extension.asDdMmmYyyy
import org.smartregister.fhircore.engine.util.extension.asReference
import org.smartregister.fhircore.engine.util.extension.referenceValue
import timber.log.Timber

@HiltWorker
class WelcomeServiceBackToCarePlanWorker
@AssistedInject
constructor(
  @Assisted val appContext: Context,
  @Assisted workerParameters: WorkerParameters,
  val fhirEngine: FhirEngine
) : CoroutineWorker(appContext, workerParameters) {

  override suspend fun doWork(): Result {
    return try {
      updateCarePlanWithWelcomeService()
      Result.success()
    } catch (e: Exception) {
      Timber.e(e)
      Result.failure()
    }
  }

  private suspend fun updateCarePlanWithWelcomeService() {
    val tracingTagsInit =
      tracingCodingTags
        .map<Coding, TokenParamFilterCriterion.() -> Unit> {
          return@map { value = of(it) }
        }
        .toTypedArray()

    val interruptedTreatmentTasks =
      fhirEngine
        .search<Task> {
          filter(TokenClientParam("_tag"), *tracingTagsInit, operation = Operation.OR)
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
        .map { it.resource }
        .filter { it.reasonCode.coding.any { coding -> coding.code == INTERRUPTED_TREAT_CODE } }
        .filter {
          it.status in listOf(Task.TaskStatus.INPROGRESS, Task.TaskStatus.READY) &&
            it.executionPeriod.hasStart() &&
            it.executionPeriod
              .start
              .before(Date())
              .or(it.executionPeriod.start.asDdMmmYyyy() == Date().asDdMmmYyyy())
        }

    interruptedTreatmentTasks.groupBy { it.`for`.reference }.forEach { (t, u) ->
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
                  Coding("https://d-tree.org", WELCOME_SERVICE_QUESTIONNAIRE_ID, taskDescription)
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

    private val phoneTracingCoding = Coding("https://d-tree.org", "phone-tracing", "Phone Tracing")
    private val homeTracingCoding = Coding("https://d-tree.org", "phone-tracing", "Phone Tracing")
    private val tracingCodingTags =
      arrayOf<Coding>(
        phoneTracingCoding,
        homeTracingCoding,
        phoneTracingCoding.copy().apply { system = "http://snomed.info/sct" },
        homeTracingCoding.copy().apply { system = "http://snomed.info/sct" }
      )
  }
}

internal fun CarePlan.welcomeServiceTask(
  taskId: String,
  taskDescription: String,
  patient: Patient
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
      }
    )
    id = taskId
    meta =
      Meta()
        .addTag(
          Coding().apply {
            system = "https://d-tree.org"
            code = "clinic-visit-task-order-11"
          }
        )
    reasonReference =
      Reference().apply {
        reference = "Questionnaire/art-client-welcome-service-back-to-care"
        display = taskDescription
      }
  }
