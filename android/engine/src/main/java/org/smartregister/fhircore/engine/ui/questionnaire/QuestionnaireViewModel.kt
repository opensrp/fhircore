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

package org.smartregister.fhircore.engine.ui.questionnaire

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.fhir.datacapture.mapping.ResourceMapper
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.StructureMap
import org.smartregister.fhircore.engine.configuration.app.ConfigurableApplication
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.util.helper.TransformSupportServices

class QuestionnaireViewModel(
  application: Application,
  private val questionnaireConfig: QuestionnaireConfig
) : AndroidViewModel(application) {

  private val defaultRepository: DefaultRepository =
    DefaultRepository(
      (application as ConfigurableApplication).fhirEngine,
    )

  var structureMapProvider: (suspend (String) -> StructureMap?)? = null

  suspend fun loadQuestionnaire(): Questionnaire? =
    defaultRepository.loadResource(questionnaireConfig.identifier)

  fun fetchStructureMap(structureMapUrl: String?): StructureMap? {
    var structureMap: StructureMap? = null
    viewModelScope.launch {
      structureMapUrl?.substringAfterLast("/")?.run {
        structureMap = defaultRepository.loadResource(this)
      }
    }
    return structureMap
  }

  fun saveExtractedResources(
    resourceId: String?,
    context: Context,
    questionnaire: Questionnaire,
    questionnaireResponse: QuestionnaireResponse
  ) {
    viewModelScope.launch {
      val contextR4 =
        (getApplication<Application>() as ConfigurableApplication).workerContextProvider
      val transformSupportServices = TransformSupportServices(mutableListOf(), contextR4)
      val bundle =
        ResourceMapper.extract(
          questionnaire = questionnaire,
          questionnaireResponse = questionnaireResponse,
          structureMapProvider = retrieveStructureMapProvider(),
          context = context,
          transformSupportServices = transformSupportServices
        )

      if (!bundle.isEmpty) {
        bundle.entry.forEach { bundleEntry ->
          if (resourceId != null && bundleEntry.hasResource()) {
            bundleEntry.resource.id = resourceId
          }
          defaultRepository.save(bundleEntry.resource)
        }
      }
    }
  }

  fun retrieveStructureMapProvider(): (suspend (String) -> StructureMap?) {
    if (structureMapProvider == null) {
      structureMapProvider = { structureMapUrl: String -> fetchStructureMap(structureMapUrl) }
    }
    return structureMapProvider!!
  }

  suspend fun loadPatient(patientId: String): Patient? {
    return defaultRepository.loadResource(patientId)
  }

  fun saveResource(resource: Resource) {
    viewModelScope.launch { defaultRepository.save(resource = resource) }
  }
}
