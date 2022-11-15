package org.smartregister.fhircore.quest.ui.report.measure.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.search.Search
import com.google.android.fhir.search.search
import com.google.android.fhir.workflow.FhirOperator
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.YearMonth.of
import java.util.Calendar
import java.util.Date
import kotlinx.coroutines.withContext
import org.hl7.fhir.r4.model.DateType
import org.hl7.fhir.r4.model.MeasureReport
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.configuration.ConfigType
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.report.measure.MeasureReportConfiguration
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.domain.model.DataQuery
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.engine.util.extension.SDF_YYYY_MM_DD
import org.smartregister.fhircore.engine.util.extension.decodeJson
import org.smartregister.fhircore.engine.util.extension.filterBy
import org.smartregister.fhircore.engine.util.extension.formatDate
import org.smartregister.fhircore.engine.util.extension.loadCqlLibraryBundle
import org.smartregister.fhircore.engine.util.extension.parseDate
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
    Timber.w("Starting measure reporting worker")
    val endDate = Calendar.getInstance().time.formatDate(SDF_YYYY_MM_DD)
    val startDate =
        retrieveMeasureReportConfiguration().registerDate?.parseDate(SDF_YYYY_MM_DD).toString()

    //    retrieveMeasureReportConfiguration().reports.forEach {
    //      withContext(dispatcherProvider.io()) { fhirEngine.loadCqlLibraryBundle(fhirOperator,
    // it.url) }
    //
    //      evaluatePopulationMeasure(it.url, "2022-11-01", "2022-11-30")
    //    }
    withContext(dispatcherProvider.io()) {
      fhirEngine.loadCqlLibraryBundle(
          fhirOperator, retrieveMeasureReportConfiguration().reports[0].url)
    }

    evaluatePopulationMeasure(
        retrieveMeasureReportConfiguration().reports[0].url, "2022-12-01", "2022-12-30")

    Timber.w("Done with measure reporting worker")
    return Result.success()
  }

  private fun retrieveMeasureReportConfiguration(): MeasureReportConfiguration =
      configurationRegistry.retrieveConfiguration(
          configType = ConfigType.MeasureReport, configId = "defaultMeasureReport")

  private suspend fun evaluatePopulationMeasure(
      measureUrl: String,
      startDateFormatted: String,
      endDateFormatted: String
  ) {
    Timber.w("url $measureUrl")

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
            Timber.w(exception)
            null
          }
        }
    Timber.w("MeasureResource" + measureReport.toString())
    if (measureReport != null) defaultRepository.addOrUpdate(measureReport)
    Timber.w(
        "Fetching" +
            fhirEngine.search<MeasureReport> {
              filter(MeasureReport.PERIOD, { value = of(DateType("2022-12-01")) })
            })
    val dataQuery =
        """{
          "id": "reportQueryByDate",
          "filterType": "DATE",
          "key": "date",
          "valueType": "DATE",
          "valueDate": "2022-12-01",
          "paramPrefix": "GREATERTHAN_OR_EQUALS"
        }"""
            .decodeJson<DataQuery>()
    val search = Search(ResourceType.MeasureReport).apply { filterBy(dataQuery) }
    Timber.w(
        "MeasureResource " +
            fhirEngine.search<MeasureReport>(Search(type = ResourceType.MeasureReport)))
  }
}
