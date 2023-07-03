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
import org.hl7.fhir.r4.model.Group
import org.hl7.fhir.r4.model.MeasureReport
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.app.ConfigService
import org.smartregister.fhircore.engine.configuration.report.measure.MeasureReportConfig
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.data.local.register.RegisterRepository
import org.smartregister.fhircore.engine.rulesengine.ConfigRulesExecutor
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.extension.asReference
import org.smartregister.fhircore.quest.ui.report.measure.MeasureReportViewModel

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
  val fhirOperator: FhirOperator
) :
  DefaultRepository(
    fhirEngine = fhirEngine,
    dispatcherProvider = dispatcherProvider,
    sharedPreferencesHelper = sharedPreferencesHelper,
    configurationRegistry = configurationRegistry,
    configService = configService,
    configRulesExecutor = configRulesExecutor
  ) {

  suspend fun evaluatePopulationMeasure(
    measureUrl: String,
    startDateFormatted: String,
    endDateFormatted: String,
    subjects: List<String>,
    existing: List<MeasureReport>
  ): List<MeasureReport> {
    val measureReport = mutableListOf<MeasureReport>()
    withContext(dispatcherProvider.io()) {
      if (subjects.isNotEmpty()) {
        subjects
          .map {
            runMeasureReport(
              measureUrl,
              MeasureReportViewModel.SUBJECT,
              startDateFormatted,
              endDateFormatted,
              it
            )
          }
          .forEach { measureReport.add(it) }
      } else
        runMeasureReport(
          measureUrl,
          MeasureReportViewModel.POPULATION,
          startDateFormatted,
          endDateFormatted,
          null
        )
          .also { measureReport.add(it) }

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
    }
    return measureReport
  }

  /**
   * Run and generate MeasureReport for given measure and subject.
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
    subject: String?
  ): MeasureReport {
    return fhirOperator.evaluateMeasure(
      measureUrl = measureUrl,
      start = startDateFormatted,
      end = endDateFormatted,
      reportType = reportType,
      subject = subject,
      practitioner = null
      /* TODO DO NOT pass this id to MeasureProcessor as this is treated as subject if subject is null.
      practitionerId?.asReference(ResourceType.Practitioner)?.reference*/ ,
    )
  }

  suspend fun fetchSubjects(config: MeasureReportConfig): List<String> {
    return if (config.subjectXFhirQuery?.isNotEmpty() == true) {
      fhirEngine.search(config.subjectXFhirQuery!!).map {
        // prevent missing subject where MeasureEvaluator looks for Group members and skips the
        // Group itself
        if (it is Group && !it.hasMember()) {
          it.addMember(Group.GroupMemberComponent(it.asReference()))
          update(it)
        }
        "${it.resourceType.name}/${it.logicalId}"
      }
    } else emptyList()
  }
}
