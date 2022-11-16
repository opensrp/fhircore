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

package org.smartregister.fhircore.quest.ui.report.measure.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import ca.uhn.fhir.rest.param.ParamPrefixEnum
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.search.search
import com.google.android.fhir.workflow.FhirOperator
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.Calendar
import java.util.Date
import kotlinx.coroutines.withContext
import org.hl7.fhir.r4.model.DateTimeType
import org.hl7.fhir.r4.model.MeasureReport
import org.smartregister.fhircore.engine.configuration.ConfigType
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.report.measure.MeasureReportConfiguration
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.engine.util.extension.SDF_YYYY_MM_DD
import org.smartregister.fhircore.engine.util.extension.firstDayOfMonth
import org.smartregister.fhircore.engine.util.extension.formatDate
import org.smartregister.fhircore.engine.util.extension.lastDayOfMonth
import org.smartregister.fhircore.engine.util.extension.loadCqlLibraryBundle
import org.smartregister.fhircore.engine.util.extension.parseDate
import org.smartregister.fhircore.engine.util.extension.plusMonths
import org.smartregister.fhircore.quest.ui.report.measure.MeasureReportViewModel
import timber.log.Timber

@HiltWorker
class MeasureReportWorker
@AssistedInject
constructor(
  @Assisted val appContext: Context,
  @Assisted workerParams: WorkerParameters,
  val defaultRepository: DefaultRepository,
  val configurationRegistry: ConfigurationRegistry,
  val dispatcherProvider: DefaultDispatcherProvider,
  val fhirOperator: FhirOperator,
  val fhirEngine: FhirEngine
) : CoroutineWorker(appContext, workerParams) {

  override suspend fun doWork(): Result {
    val configuration = retrieveMeasureReportConfiguration()
    val monthList = getMonthRangeList(configuration)

    configuration.reports.forEach {
      withContext(dispatcherProvider.io()) { fhirEngine.loadCqlLibraryBundle(fhirOperator, it.url) }

      monthList.forEach { date ->
        val startDateFormatted = date.firstDayOfMonth().formatDate(SDF_YYYY_MM_DD)
        val endDateFormatted = date.lastDayOfMonth().formatDate(SDF_YYYY_MM_DD)
        if (!checkReportAlreadyGenerated(startDateFormatted, endDateFormatted)) {
          evaluatePopulationMeasure(it.url, startDateFormatted, endDateFormatted)
        }
      }
    }

    return Result.success()
  }

  private fun retrieveMeasureReportConfiguration(): MeasureReportConfiguration =
    configurationRegistry.retrieveConfiguration(
      configType = ConfigType.MeasureReport,
      configId = "defaultMeasureReport"
    )

  private suspend fun evaluatePopulationMeasure(
    measureUrl: String,
    startDateFormatted: String,
    endDateFormatted: String
  ) {

    val measureReport: MeasureReport? =
      withContext(dispatcherProvider.io()) {
        try {
          fhirOperator.evaluateMeasure(
            measureUrl = measureUrl,
            start = startDateFormatted,
            end = endDateFormatted,
            reportType = MeasureReportViewModel.POPULATION,
            subject = null,
            practitioner = null
            /* TODO DO NOT pass this id to MeasureProcessor as this is treated as subject if subject is null.
            practitionerId?.asReference(ResourceType.Practitioner)?.reference*/ ,
            lastReceivedOn = null // Non-null value not supported yet
          )
        } catch (exception: IllegalArgumentException) {
          Timber.e(exception)
          null
        }
      }
    if (measureReport != null) defaultRepository.addOrUpdate(measureReport)
  }

  /** @return list of months within current date and starting date of campaign */
  fun getMonthRangeList(configuration: MeasureReportConfiguration): List<Date> {
    val yearMonths = mutableListOf<Date>()
    val endDate = Calendar.getInstance().time.formatDate(SDF_YYYY_MM_DD).parseDate(SDF_YYYY_MM_DD)
    var lastDate = endDate?.firstDayOfMonth()
    while (lastDate!!.after(configuration.registerDate?.parseDate(SDF_YYYY_MM_DD))) {
      yearMonths.add(lastDate)
      lastDate = lastDate.plusMonths(-1)
    }
    return yearMonths.toList()
  }
  suspend fun checkReportAlreadyGenerated(
    startDateFormatted: String,
    endDateFormatted: String
  ): Boolean {
    return fhirEngine
      .search<MeasureReport> {
        filter(
          MeasureReport.PERIOD,
          {
            value = of(DateTimeType(startDateFormatted))
            prefix = ParamPrefixEnum.GREATERTHAN_OR_EQUALS
          },
          {
            value = of(DateTimeType(endDateFormatted))
            prefix = ParamPrefixEnum.LESSTHAN_OR_EQUALS
          },
          operation = com.google.android.fhir.search.Operation.AND
        )
      }
      .isEmpty()
  }
  companion object {
    const val WORK_ID = "fhirMeasureReportWorker"
  }
}
