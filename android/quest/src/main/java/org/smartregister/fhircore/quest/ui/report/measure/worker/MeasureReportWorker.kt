/*
 * Copyright 2021-2024 Ona Systems, Inc
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
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.knowledge.KnowledgeManager
import com.google.android.fhir.workflow.FhirOperator
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Calendar
import java.util.Date
import java.util.NoSuchElementException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.withContext
import org.hl7.fhir.instance.model.api.IBaseResource
import org.hl7.fhir.r4.model.Measure
import org.hl7.fhir.r4.model.MeasureReport
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.engine.util.extension.SDFHH_MM
import org.smartregister.fhircore.engine.util.extension.SDF_YYYY_MM_DD
import org.smartregister.fhircore.engine.util.extension.batchedSearch
import org.smartregister.fhircore.engine.util.extension.firstDayOfMonth
import org.smartregister.fhircore.engine.util.extension.formatDate
import org.smartregister.fhircore.engine.util.extension.lastDayOfMonth
import org.smartregister.fhircore.engine.util.extension.loadCqlLibraryBundle
import org.smartregister.fhircore.engine.util.extension.parseDate
import org.smartregister.fhircore.engine.util.extension.plusMonths
import org.smartregister.fhircore.engine.util.extension.retrievePreviouslyGeneratedMeasureReports
import org.smartregister.fhircore.engine.util.extension.today
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
  val fhirEngine: FhirEngine,
  private val knowledgeManager: KnowledgeManager,
  val workManager: WorkManager,
) : CoroutineWorker(appContext, workerParams) {

  override suspend fun doWork(): Result {
    val time = inputData.getString(TIME)
    val campaignDate = inputData.getString(APP_CONFIG)
    try {
      val monthList = campaignDate?.let { getMonthRangeList(it) }
      Timber.w("started MeasureReportWorker")

      fhirEngine
        .batchedSearch<Measure> {}
        .map { it.resource }
        .forEach {
          monthList?.forEachIndexed { index, date ->
            val startDateFormatted = date.firstDayOfMonth().formatDate(SDF_YYYY_MM_DD)
            val endDateFormatted = date.lastDayOfMonth().formatDate(SDF_YYYY_MM_DD)
            if (
              fhirEngine
                .retrievePreviouslyGeneratedMeasureReports(
                  startDateFormatted = startDateFormatted,
                  endDateFormatted = endDateFormatted,
                  measureUrl = it.url,
                  subjects = emptyList(),
                )
                .isEmpty()
            ) {
              evaluatePopulationMeasure(it.url, startDateFormatted, endDateFormatted)
            } else {
              if (index == 0 && lastDayOfMonth(endDateFormatted.parseDate(SDF_YYYY_MM_DD))) {
                evaluatePopulationMeasure(it.url, startDateFormatted, endDateFormatted)
              }
            }
          }
        }
      Timber.w("successfully completed MeasureReportMonthPeriodWorker")
    } catch (e: Exception) {
      Timber.w(e.localizedMessage)
      Result.failure()
    } finally {
      time?.let { if (campaignDate != null) scheduleMeasureReportWorker(workManager = workManager) }
    }
    return Result.success()
  }

  private fun lastDayOfMonth(date: Date?): Boolean = date?.before(today().lastDayOfMonth()) == true

  private suspend fun evaluatePopulationMeasure(
    measureUrl: String,
    startDateFormatted: String,
    endDateFormatted: String,
  ) {
    Timber.w("evaluatePopulationMeasure in MeasureReportWorker")

    withContext(dispatcherProvider.io()) {
      fhirEngine.loadCqlLibraryBundle(fhirOperator, measureUrl)
    }
    val measureReport: MeasureReport? =
      withContext(dispatcherProvider.io()) {
        try {
          val measureUrlResources: Iterable<IBaseResource> =
            knowledgeManager.loadResources(
              resourceType = ResourceType.Measure.name,
              url = measureUrl,
            )

          fhirOperator.evaluateMeasure(
            measure = measureUrlResources.first() as Measure,
            start = startDateFormatted,
            end = endDateFormatted,
            reportType = MeasureReportViewModel.POPULATION,
            subjectId = null,
            practitioner = null,
            /* TODO DO NOT pass this id to MeasureProcessor as this is treated as subject if subject is null.
            practitionerId?.asReference(ResourceType.Practitioner)?.reference*/
          )
        } catch (exception: IllegalArgumentException) {
          Timber.e(exception)
          null
        } catch (exception: NoSuchElementException) {
          Timber.e(exception)
          null
        }
      }
    if (measureReport != null) {
      Timber.w(
        "saving measureReport in MeasureReportWorker with period.end ${measureReport.period.end}",
      )

      val result =
        fhirEngine.retrievePreviouslyGeneratedMeasureReports(
          startDateFormatted = startDateFormatted,
          endDateFormatted = endDateFormatted,
          measureUrl = measureUrl,
          subjects = emptyList(),
        )
      if (result.isNotEmpty()) defaultRepository.delete(result.last())
      defaultRepository.addOrUpdate(resource = measureReport)
    }
  }

  /** @return list of months within current date and starting date of campaign */
  fun getMonthRangeList(campaignRegisterDate: String): List<Date> {
    val yearMonths = mutableListOf<Date>()
    val endDate = Calendar.getInstance().time.formatDate(SDF_YYYY_MM_DD).parseDate(SDF_YYYY_MM_DD)
    var lastDate = endDate?.firstDayOfMonth()
    while (lastDate?.after(campaignRegisterDate.parseDate(SDF_YYYY_MM_DD)) == true) {
      yearMonths.add(lastDate)
      lastDate = lastDate.plusMonths(-1)
    }
    return yearMonths.toList()
  }

  companion object {
    private const val WORK_ID = "fhirMeasureReportWorker"
    private const val APP_CONFIG = "appConfig"
    private const val TIME = "time"

    fun scheduleMeasureReportWorker(workManager: WorkManager) {
      // TODO removed the from application config, should be retrieved from
      // MeasureReportConfiguration instead
      val scheduleTime = "12:55"
      val campaignRegisterDate = "2020-10-27"

      try {
        val alarmTime =
          LocalTime.parse(scheduleTime, DateTimeFormatter.ofPattern(SDFHH_MM)) // 24h format
        var now = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES)
        val nowTime = now.toLocalTime()
        // if same time, schedule for next day as well
        // if today's time had passed, schedule for next day
        if (nowTime == alarmTime || nowTime.isAfter(alarmTime)) {
          now = now.plusDays(1)
        }
        now =
          now
            .withHour(alarmTime.hour)
            .withMinute(alarmTime.minute) // .withSecond(alarmTime.second).withNano(alarmTime.nano)
        val duration = Duration.between(LocalDateTime.now(), now)
        val data = workDataOf(APP_CONFIG to campaignRegisterDate, TIME to scheduleTime)

        val workRequest =
          OneTimeWorkRequestBuilder<MeasureReportWorker>()
            .setInitialDelay(duration.seconds, TimeUnit.SECONDS)
            .setInputData(data)
            .build()

        workManager.enqueueUniqueWork(WORK_ID + now, ExistingWorkPolicy.KEEP, workRequest)
      } catch (e: Exception) {
        Timber.w(e.localizedMessage)
      }
    }
  }
}
