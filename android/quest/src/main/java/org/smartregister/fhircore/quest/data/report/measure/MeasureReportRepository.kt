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

package org.smartregister.fhircore.quest.data.report.measure

import com.google.android.fhir.FhirEngine
import com.google.android.fhir.logicalId
import com.google.android.fhir.search.search
import com.google.android.fhir.workflow.FhirOperator
import javax.inject.Inject
import kotlinx.coroutines.withContext
import org.hl7.fhir.exceptions.FHIRException
import org.hl7.fhir.r4.model.Group
import org.hl7.fhir.r4.model.MeasureReport
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.app.ConfigService
import org.smartregister.fhircore.engine.configuration.report.measure.ReportConfiguration
import org.smartregister.fhircore.engine.cql.R4MeasureProcessorExt
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.data.local.register.RegisterRepository
import org.smartregister.fhircore.engine.rulesengine.ConfigRulesExecutor
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.extension.asReference
import org.smartregister.fhircore.engine.util.fhirpath.FhirPathDataExtractor
import org.smartregister.fhircore.quest.ui.report.measure.MeasureReportViewModel
import timber.log.Timber

class MeasureReportRepository
@Inject
constructor(
  override val fhirEngine: FhirEngine,
  override val dispatcherProvider: DispatcherProvider,
  override val sharedPreferencesHelper: SharedPreferencesHelper,
  override val configurationRegistry: ConfigurationRegistry,
  override val configService: ConfigService,
  override val configRulesExecutor: ConfigRulesExecutor,
  val registerRepository: RegisterRepository,
  private val fhirOperator: FhirOperator,
  override val fhirPathDataExtractor: FhirPathDataExtractor,
) :
  DefaultRepository(
    fhirEngine = fhirEngine,
    dispatcherProvider = dispatcherProvider,
    sharedPreferencesHelper = sharedPreferencesHelper,
    configurationRegistry = configurationRegistry,
    configService = configService,
    configRulesExecutor = configRulesExecutor,
    fhirPathDataExtractor = fhirPathDataExtractor,
  ) {

  val measureProcessorExt by lazy { R4MeasureProcessorExt.buildMeasureProcessorExt(fhirOperator) }

  /**
   * If running a measure for any subject throws a null pointer exception the measures for
   * unevaluated subjects are discarded and the method returns a list of any reports added so far.
   *
   * @param measureUrl url of measure report to generate
   * @param startDateFormatted start date of measure period with format yyyy-MM-dd
   * @param endDateFormatted end date of measure period with format yyyy-MM-dd
   * @param subjects list of subjects to generate report for, can be empty
   * @param existing list of existing measure reports, can be empty
   * @return list of generated measure reports
   */
  suspend fun evaluatePopulationMeasure(
    measureUrl: String,
    startDateFormatted: String,
    endDateFormatted: String,
    subjects: List<String>,
    existing: List<MeasureReport>,
    practitionerId: String?,
    params: Map<String, String>,
  ): List<MeasureReport> {
    val measureReport = mutableListOf<MeasureReport>()
    try {
      withContext(dispatcherProvider.io()) {
        if (subjects.isNotEmpty()) {
          subjects
            .map {
              runMeasureReport(
                measureUrl = measureUrl,
                reportType = MeasureReportViewModel.SUBJECT,
                startDateFormatted = startDateFormatted,
                endDateFormatted = endDateFormatted,
                subject = it,
                practitionerId = practitionerId,
                params = params,
              )
            }
            .forEach { subject -> measureReport.add(subject) }
        } else {
          runMeasureReport(
              measureUrl = measureUrl,
              reportType = MeasureReportViewModel.POPULATION,
              startDateFormatted = startDateFormatted,
              endDateFormatted = endDateFormatted,
              subject = null,
              practitionerId = practitionerId,
              params = params,
            )
            .also { measureReport.add(it) }
        }
      }

      measureReport.forEach { report ->
        // if report exists override instead of creating a new one
        existing
          .find {
            it.measure == report.measure &&
              (!it.hasSubject() || it.subject.reference == report.subject.reference)
          }
          ?.let { existing -> report.id = existing.id }
        addOrUpdate(resource = report)
      }
    } catch (exception: NullPointerException) {
      Timber.e(exception, "Exception thrown with measureUrl: $measureUrl.")
    }
    return measureReport
  }

  /**
   * Run and generate MeasureReport for given measure and subject. Note that we do not pass this
   * practitionerId to MeasureProcessor because this is treated as subject if subject is null.
   *
   * @param measureUrl url of measure to generate report for
   * @param reportType type of report (population | subject)
   * @param startDateFormatted start date of measure period with format yyyy-MM-dd
   * @param endDateFormatted end date of measure period with format yyyy-MM-dd
   * @param subject the individual subject reference (ResourceType/id) to run report for
   */
  private fun runMeasureReport(
    measureUrl: String,
    reportType: String,
    startDateFormatted: String,
    endDateFormatted: String,
    subject: String?,
    practitionerId: String?,
    params: Map<String, String>,
  ): MeasureReport {
    return measureProcessorExt.evaluateMeasure(
      measureUrl,
      startDateFormatted,
      endDateFormatted,
      reportType,
      subject,
      practitionerId,
      params,
    )
  }

  /**
   * Fetch subjects based on subjectXFhirQuery in passed [ReportConfiguration]. If empty or
   * FHIRException thrown return an empty list.
   *
   * @param config [ReportConfiguration] with subjectXFhirQuery to fetch subjects based on
   * @return list of subjects or empty list
   */
  suspend fun fetchSubjects(config: ReportConfiguration): List<String> {
    if (config.subjectXFhirQuery?.isNotEmpty() == true) {
      try {
        return fhirEngine.search(config.subjectXFhirQuery!!).map {
          // prevent missing subject where MeasureEvaluator looks for Group members and skips the
          // Group itself
          if (it is Group && !it.hasMember()) {
            it.addMember(Group.GroupMemberComponent(it.asReference()))
            update(it)
          }
          "${it.resourceType.name}/${it.logicalId}"
        }
      } catch (e: FHIRException) {
        Timber.e(e, "When fetching subjects for measure report")
      }
    }
    return emptyList()
  }
}
