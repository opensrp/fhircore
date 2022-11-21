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
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.search.search
import com.google.android.fhir.workflow.FhirOperator
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.util.Calendar
import java.util.Date
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.withContext
import org.hl7.fhir.r4.model.Measure
import org.hl7.fhir.r4.model.MeasureReport
import org.smartregister.fhircore.engine.configuration.ConfigType
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.app.ApplicationConfiguration
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.engine.util.extension.SDF_YYYY_MM_DD
import org.smartregister.fhircore.engine.util.extension.alreadyGeneratedMeasureReports
import org.smartregister.fhircore.engine.util.extension.firstDayOfMonth
import org.smartregister.fhircore.engine.util.extension.formatDate
import org.smartregister.fhircore.engine.util.extension.lastDayOfMonth
import org.smartregister.fhircore.engine.util.extension.loadCqlLibraryBundle
import org.smartregister.fhircore.engine.util.extension.parseDate
import org.smartregister.fhircore.engine.util.extension.plusMonths
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
  val fhirEngine: FhirEngine
) : CoroutineWorker(appContext, workerParams) {

  val applicationConfiguration: ApplicationConfiguration by lazy {
    configurationRegistry.retrieveConfiguration(ConfigType.Application, configId = "app/debug")
  }

  override suspend fun doWork(): Result {
    val monthList = getMonthRangeList()
    Timber.w("started  / . . . worker . . ./")
    fhirEngine.search<Measure> {}.forEach {
      monthList.forEachIndexed { index, date ->
        val startDateFormatted = date.firstDayOfMonth().formatDate(SDF_YYYY_MM_DD)
        val endDateFormatted = date.lastDayOfMonth().formatDate(SDF_YYYY_MM_DD)
        if (alreadyGeneratedMeasureReports(fhirEngine, startDateFormatted, endDateFormatted, it.url)
            .isEmpty()
        ) {
          evaluatePopulationMeasure(it.url, startDateFormatted, endDateFormatted)
        } else {
          if (index == 0 && lastDayOfMonth(endDateFormatted.parseDate(SDF_YYYY_MM_DD)))
            evaluatePopulationMeasure(it.url, startDateFormatted, endDateFormatted)
        }
      }
    }

    return Result.success()
  }

  private fun lastDayOfMonth(date: Date?): Boolean = date?.before(today().lastDayOfMonth()) == true

  private suspend fun evaluatePopulationMeasure(
    measureUrl: String,
    startDateFormatted: String,
    endDateFormatted: String
  ) {
    withContext(dispatcherProvider.io()) {
      fhirEngine.loadCqlLibraryBundle(fhirOperator, measureUrl)
    }
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
    if (measureReport != null) {
      val result =
        alreadyGeneratedMeasureReports(fhirEngine, startDateFormatted, endDateFormatted, measureUrl)
      if (result.isNotEmpty()) defaultRepository.delete(result.last())
      defaultRepository.addOrUpdate(resource = measureReport)
    }
  }

  /** @return list of months within current date and starting date of campaign */
  fun getMonthRangeList(): List<Date> {
    Timber.w("appConfig$applicationConfiguration")
    val yearMonths = mutableListOf<Date>()
    val endDate = Calendar.getInstance().time.formatDate(SDF_YYYY_MM_DD).parseDate(SDF_YYYY_MM_DD)
    var lastDate = endDate?.firstDayOfMonth()
    while (lastDate?.after(applicationConfiguration.registerDate.parseDate(SDF_YYYY_MM_DD)) ==
      true) {
      yearMonths.add(lastDate)
      lastDate = lastDate.plusMonths(-1)
    }
    return yearMonths.toList()
  }
  companion object {
    private const val WORK_ID = "fhirMeasureReportWorker"

    fun scheduleMeasureReportWorker(workManager: WorkManager, hours: Int, minutes: Int) {
      // trigger at 8:30am
      val alarmTime = LocalTime.of(hours, minutes)
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

      Timber.d("runAt=${duration.seconds}s")
      val workRequest =
        OneTimeWorkRequestBuilder<MeasureReportWorker>()
          .setInitialDelay(duration.seconds, TimeUnit.SECONDS)
          .build()

      workManager.enqueueUniqueWork(WORK_ID, ExistingWorkPolicy.REPLACE, workRequest)
    }
  }
}
