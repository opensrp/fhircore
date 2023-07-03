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
import org.hl7.fhir.r4.model.MeasureReport
import org.smartregister.fhircore.engine.configuration.ConfigType
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.report.measure.MeasureReportConfiguration
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.extension.SDF_YYYY_MMM
import org.smartregister.fhircore.engine.util.extension.SDF_YYYY_MM_DD
import org.smartregister.fhircore.engine.util.extension.firstDayOfMonth
import org.smartregister.fhircore.engine.util.extension.formatDate
import org.smartregister.fhircore.engine.util.extension.lastDayOfMonth
import org.smartregister.fhircore.engine.util.extension.parseDate
import org.smartregister.fhircore.engine.util.extension.retrievePreviouslyGeneratedMeasureReports
import org.smartregister.fhircore.engine.util.extension.today
import org.smartregister.fhircore.quest.data.report.measure.MeasureReportRepository
import timber.log.Timber
import java.util.Date

@HiltWorker
class MeasureReportConfigWorker
@AssistedInject
constructor(
    @Assisted val context: Context,
    @Assisted workerParams: WorkerParameters,
    val fhirEngine: FhirEngine,
    val defaultRepository: DefaultRepository,
    val dispatcherProvider: DispatcherProvider,
    val measureReportRepository: MeasureReportRepository,
    val configurationRegistry: ConfigurationRegistry,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {

        try {

            inputData.getString(MEASURE_REPORT_CONFIG_ID)
                ?.let<String, MeasureReportConfiguration> {
                    configurationRegistry.retrieveConfiguration(ConfigType.MeasureReport, it)
                }
                ?.reports?.forEach { config ->
                    val startDateFormatted = today().firstDayOfMonth().formatDate(SDF_YYYY_MM_DD)
                    val endDateFormatted = today().lastDayOfMonth().formatDate(SDF_YYYY_MM_DD)

                    val subjects = measureReportRepository.fetchSubjects(config)
                    val existing = retrievePreviouslyGeneratedMeasureReports(
                        fhirEngine = fhirEngine,
                        startDateFormatted = startDateFormatted,
                        endDateFormatted = endDateFormatted,
                        measureUrl = config.url,
                        subjects = subjects
                    )

                    if (endDateFormatted.parseDate(SDF_YYYY_MM_DD)!!
                            .formatDate(SDF_YYYY_MMM)
                            .contentEquals(Date().formatDate(SDF_YYYY_MMM)) || existing.isEmpty()
                    ) {
                        measureReportRepository.evaluatePopulationMeasure(config.url, startDateFormatted, endDateFormatted, subjects, existing)
                    }
                }
            Timber.w("Result.success  / . . . MeasureReportWorker . . ./")
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