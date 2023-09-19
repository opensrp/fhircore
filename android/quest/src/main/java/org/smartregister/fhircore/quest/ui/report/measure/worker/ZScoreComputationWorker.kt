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
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import org.hl7.fhir.r4.model.Basic
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Encounter
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.cql.LibraryEvaluator
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.quest.ui.questionnaire.QuestionnaireViewModel.Companion.Z_SCORE_COMPUTATION_DATA
import org.smartregister.fhircore.quest.ui.questionnaire.deserializeFromJson
import timber.log.Timber

@HiltWorker
class ZScoreComputationWorker
@AssistedInject
constructor(
  @Assisted val context: Context,
  @Assisted workerParams: WorkerParameters,
  val fhirEngine: FhirEngine,
  val defaultRepository: DefaultRepository,
  val dispatcherProvider: DispatcherProvider,
  private val libraryEvaluator: LibraryEvaluator,
  val configurationRegistry: ConfigurationRegistry,
) : CoroutineWorker(context, workerParams) {

  var bundle: Bundle = Bundle()

  override suspend fun doWork(): Result {
    try {
      Timber.i("Started $TAG")
      // Retrieve the CqlComputationData object from input data
      val cqlComputationData = deserializeFromJson(inputData.getString(Z_SCORE_COMPUTATION_DATA))
      val subjectId = cqlComputationData?.subjectId
      var resource: Resource? = null
      cqlComputationData?.resourceIdentifier?.forEach { resourceIdentifier ->
        when (resourceIdentifier.resourceType) {
          OBSERVATION -> {
            resource = defaultRepository.loadResource(resourceIdentifier.resourceId) as Observation?
          }
          ENCOUNTER -> {
            resource = defaultRepository.loadResource(resourceIdentifier.resourceId) as Encounter?
          }
          QUESTIONNAIRE_RESPONSE -> {
            resource =
              defaultRepository.loadResource(resourceIdentifier.resourceId)
                as QuestionnaireResponse?
          }
        }
        bundle.addEntry(Bundle.BundleEntryComponent().setResource(resource))
      }

      val subject = defaultRepository.loadResource(subjectId.toString()) as Patient?
      bundle.addEntry(Bundle.BundleEntryComponent().setResource(subject))

      // TODO Refactor/Remove as per the issue: https://github.com/opensrp/fhircore/issues/2747
      BASIC_RESOURCE_IDS.forEach { resourceId ->
        val basicResource = defaultRepository.loadResource(resourceId) as Basic?
        bundle.addEntry(Bundle.BundleEntryComponent().setResource(basicResource))
      }

      // Perform CQL computation
      if (subject?.resourceType == ResourceType.Patient) {
        libraryEvaluator.runCqlLibrary(LIBRARY_ID, subject, bundle)
      }

      Timber.i("Successfully completed $TAG")
    } catch (e: Exception) {
      Timber.e(e, "Error in $TAG")
      return Result.failure()
    }

    return Result.success()
  }

  companion object {
    private const val TAG = "ZScoreComputationWorker"
    private const val LIBRARY_ID = "223758"
    private val BASIC_RESOURCE_IDS = listOf("223754", "223755", "223756", "223757")
    private const val OBSERVATION = "Observation"
    private const val ENCOUNTER = "Encounter"
    private const val QUESTIONNAIRE_RESPONSE = "QuestionnaireResponse"
  }
}
