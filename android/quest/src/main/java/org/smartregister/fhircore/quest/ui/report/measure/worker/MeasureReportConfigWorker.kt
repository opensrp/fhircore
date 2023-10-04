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

package org.smartregister.fhircore.quest.ui.report.measure.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.workflow.FhirOperator
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.withContext
import org.smartregister.fhircore.engine.configuration.ConfigType
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.report.measure.MeasureReportConfiguration
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.extension.SDF_YYYY_MM_DD
import org.smartregister.fhircore.engine.util.extension.firstDayOfMonth
import org.smartregister.fhircore.engine.util.extension.formatDate
import org.smartregister.fhircore.engine.util.extension.lastDayOfMonth
import org.smartregister.fhircore.engine.util.extension.loadCqlLibraryBundle
import org.smartregister.fhircore.engine.util.extension.retrievePreviouslyGeneratedMeasureReports
import org.smartregister.fhircore.engine.util.extension.today
import org.smartregister.fhircore.quest.data.report.measure.MeasureReportRepository
import timber.log.Timber

@HiltWorker
class MeasureReportConfigWorker
@AssistedInject
constructor(
  @Assisted val context: Context,
  @Assisted workerParams: WorkerParameters,
  val fhirEngine: FhirEngine,
  val fhirOperator: FhirOperator,
  val defaultRepository: DefaultRepository,
  val dispatcherProvider: DispatcherProvider,
  val measureReportRepository: MeasureReportRepository,
  val configurationRegistry: ConfigurationRegistry,
) : CoroutineWorker(context, workerParams) {

  override suspend fun doWork(): Result {

    try {
      Timber.i("started  / . . . MeasureReportWorker . . ./")

      inputData
        .getString(MEASURE_REPORT_CONFIG_ID)
        ?.let<String, MeasureReportConfiguration> {
          configurationRegistry.retrieveConfiguration(ConfigType.MeasureReport, it)
        }
        ?.reports
        ?.forEach { config ->
          val startDateFormatted = today().firstDayOfMonth().formatDate(SDF_YYYY_MM_DD)
          val endDateFormatted = today().lastDayOfMonth().formatDate(SDF_YYYY_MM_DD)

          val subjects = measureReportRepository.fetchSubjects(config)
          val existing =
            retrievePreviouslyGeneratedMeasureReports(
              fhirEngine = fhirEngine,
              startDateFormatted = startDateFormatted,
              endDateFormatted = endDateFormatted,
              measureUrl = config.url,
              subjects = listOf()
            )

          if (existing.isEmpty()) {
            withContext(dispatcherProvider.io()) {
              fhirEngine.loadCqlLibraryBundle(fhirOperator, config.url)
            }

            measureReportRepository.evaluatePopulationMeasure(
              measureUrl = config.url,
              startDateFormatted = startDateFormatted,
              endDateFormatted = endDateFormatted,
              subjects = subjects,
              existing = existing,
              practitionerId = null,
              params = mapOf(), // TODO do we also want practitioner based reports prebuilt
            )
          }
        }
      Timber.i("Result.success  / . . . MeasureReportWorker . . ./")
    } catch (e: Exception) {
      Timber.w(e.localizedMessage)
      Result.failure()
    }

    return Result.success()
  }

  companion object {
    const val WORK_ID = "fhirMeasureReportWorker"
    const val MEASURE_REPORT_CONFIG_ID = "measureReportConfigId"
  }
}
