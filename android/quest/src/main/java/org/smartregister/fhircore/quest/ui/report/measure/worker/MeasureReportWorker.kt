package org.smartregister.fhircore.quest.ui.report.measure.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import org.smartregister.fhircore.engine.configuration.ConfigType
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.report.measure.MeasureReportConfiguration
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import timber.log.Timber

@HiltWorker
class MeasureReportWorker
@AssistedInject
constructor(
    @Assisted val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    val defaultRepository: DefaultRepository,
    val configurationRegistry: ConfigurationRegistry
) : CoroutineWorker(appContext, workerParams) {
  val measureReportConfig: MeasureReportConfiguration by lazy {
    configurationRegistry.retrieveConfiguration(ConfigType.MeasureReport)
  }
  override suspend fun doWork(): Result {
    Timber.i("Starting measure reporting worker")
    val measureUrl = measureReportConfig.reports[0].url


    Timber.i("Done with measure reporting worker")

    return Result.success()
  }
}
