/*
 * Copyright 2021-2023 Ona Systems, Inc
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
import ca.uhn.fhir.rest.param.ParamPrefixEnum
import com.google.android.fhir.search.Operation
import com.google.android.fhir.search.search
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.Date
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.DateTimeType
import org.hl7.fhir.r4.model.Group
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Quantity
import org.hl7.fhir.r4.model.Resource
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.util.extension.asReference
import org.smartregister.fhircore.engine.util.extension.firstDayOfMonth
import org.smartregister.fhircore.engine.util.extension.lastDayOfMonth
import org.smartregister.fhircore.engine.util.extension.plusMonths
import org.smartregister.fhircore.engine.util.extension.referenceValue
import java.util.Calendar

@HiltWorker
class FhirMonthlyStockBalanceGeneratorWorker
@AssistedInject
constructor(
  @Assisted val context: Context,
  @Assisted workerParams: WorkerParameters,
  val defaultRepository: DefaultRepository,
) : CoroutineWorker(context, workerParams) {

  override suspend fun doWork(): Result {
    val calendar = Calendar.getInstance()
    if(calendar.get(Calendar.DATE) == calendar.getActualMinimum(Calendar.DATE)) {
      updateBalanceForStock()
    }

    return Result.success()
  }

  private suspend fun updateBalanceForStock() {
    defaultRepository.fhirEngine
      .search<Group> {
        filter(Group.CODE, { value = of(Coding().apply { system = "http://snomed.info/sct"; code = "386452003" }) })
      }
      .map { it.resource }
      .forEach {
        generateOrUpdateStockBalance(it)
      }
  }

  private suspend fun generateOrUpdateStockBalance(subject: Resource) {
    val lastMonthObservation = defaultRepository.fhirEngine
      .search<Observation> {
        filter(Observation.CODE, { value = of(Coding().apply { system = "https://mpower-social.com/"; code = "monthly-stock-balance" }) })
        filter(Observation.STATUS, { value = of(Observation.ObservationStatus.PRELIMINARY.toCode()) })
        filter(Observation.SUBJECT, { value = subject.referenceValue() })
      }
      .map { it.resource }
      .firstOrNull()

    val currentStockObservation = defaultRepository.fhirEngine
      .search<Observation> {
        filter(Observation.CODE, { value = of(Coding().apply { system = "https://mpower-social.com/"; code = "latest-stock-balance" }) })
        filter(Observation.STATUS, { value = of(Observation.ObservationStatus.PRELIMINARY.toCode()) })
        filter(Observation.SUBJECT, { value = subject.referenceValue() })
      }
      .map { it.resource }
      .firstOrNull()

    val addedStockBalance = defaultRepository.fhirEngine
      .search<Observation> {
        filter(Observation.CODE, { value = of(Coding().apply { system = "https://mpower-social.com/"; code = "added-stock-balance" }) })
        filter(Observation.SUBJECT, { value = subject.referenceValue() })
        filter(
          Observation.DATE,
          {
            value = of(DateTimeType(Date().plusMonths(-1).firstDayOfMonth()))
            prefix = ParamPrefixEnum.GREATERTHAN_OR_EQUALS
          },
          {
            value = of(DateTimeType(Date().plusMonths(-1).lastDayOfMonth()))
            prefix = ParamPrefixEnum.LESSTHAN_OR_EQUALS
          },
          operation = Operation.AND
        )
      }
      .map { it.resource }
      .sumOf { it.valueQuantity.value }

    val consumedStockBalance = defaultRepository.fhirEngine
      .search<Observation> {
        filter(Observation.CODE, { value = of(Coding().apply { system = "https://mpower-social.com/"; code = "consume-stock-balance" }) })
        filter(Observation.SUBJECT, { value = subject.referenceValue() })
        filter(
          Observation.DATE,
          {
            value = of(DateTimeType(Date().plusMonths(-1).firstDayOfMonth()))
            prefix = ParamPrefixEnum.GREATERTHAN_OR_EQUALS
          },
          {
            value = of(DateTimeType(Date().plusMonths(-1).lastDayOfMonth()))
            prefix = ParamPrefixEnum.LESSTHAN_OR_EQUALS
          },
          operation = Operation.AND
        )
      }
      .map { it.resource }
      .sumOf { it.valueQuantity.value }

    val currentStockBalance = currentStockObservation?.componentFirstRep?.valueQuantity ?: Quantity(0)

    lastMonthObservation?.let {
      it.component.add(
        Observation.ObservationComponentComponent().apply {
          code = CodeableConcept(Coding().apply { system = "https://mpower-social.com/"; code = "added-stock-balance" })
          value = Quantity(addedStockBalance.longValueExact())
        }
      )

      it.component.add(
        Observation.ObservationComponentComponent().apply {
          code = CodeableConcept(Coding().apply { system = "https://mpower-social.com/"; code = "consume-stock-balance" })
          value = Quantity(consumedStockBalance.longValueExact())
        }
      )

      it.component.add(
        Observation.ObservationComponentComponent().apply {
          code = CodeableConcept(Coding().apply { system = "https://mpower-social.com/"; code = "final-stock-balance" })
          value = currentStockBalance
        }
      )

      it.status = Observation.ObservationStatus.FINAL

      defaultRepository.addOrUpdate(addMandatoryTags = true, resource = it)
    }


    Observation().apply {
      status = Observation.ObservationStatus.PRELIMINARY
      category = listOf(
        CodeableConcept(Coding().apply { system = "http://snomed.info/sct"; code = "386452003" }),
        CodeableConcept(Coding().apply { system = "http://hl7.org/fhir/inventoryreport-counttype"; code = "snapshot" })
      )
      code = CodeableConcept(Coding().apply { system = "https://mpower-social.com/"; code = "monthly-stock-balance" })
      this.subject = subject.asReference()
      effective = DateTimeType(Date())
      value = currentStockBalance
      component.add(
        Observation.ObservationComponentComponent().apply {
          code = CodeableConcept(Coding().apply { system = "https://mpower-social.com/"; code = "initial-stock-balance" })
          value = currentStockBalance
        }
      )

      defaultRepository.addOrUpdate(addMandatoryTags = true, resource = this)
    }
  }

  companion object {
    const val WORK_ID = "FhirMonthlyStockBalanceGeneratorWorker"
  }
}
