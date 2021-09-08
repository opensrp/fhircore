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
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.google.android.fhir.datacapture.mapping.ResourceMapper
import com.google.android.fhir.datacapture.utilities.SimpleWorkerContextProvider
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.hl7.fhir.r4.model.Base
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.StructureMap
import org.smartregister.fhircore.engine.configuration.app.ConfigurableApplication
import org.smartregister.fhircore.helper.TransformSupportServices

class QuestionnaireViewModel(application: Application, private val state: SavedStateHandle) :
  AndroidViewModel(application) {

  var structureMapProvider: (suspend (String) -> StructureMap?)? = null
  val fhirEngine = (application as ConfigurableApplication).fhirEngine

  val questionnaire: Questionnaire
    get() {
      val id: String = state[QuestionnaireActivity.QUESTIONNAIRE_PATH_KEY]!!
      return loadQuestionnaire(id)
    }

  fun loadQuestionnaire(id: String): Questionnaire {
    return runBlocking { fhirEngine.load(Questionnaire::class.java, id) }
  }

  fun saveResource(resource: Resource) {
    viewModelScope.launch { fhirEngine.save(resource) }
  }

  fun saveBundleResources(bundle: Bundle, resourceId: String? = null) {
    if (!bundle.isEmpty) {
      bundle.entry.forEach { bundleEntry ->
        if (resourceId != null && bundleEntry.hasResource()) {
          bundleEntry.resource.id = resourceId
        }

        saveResource(bundleEntry.resource)
      }
    }
  }

  fun fetchStructureMap(structureMapUrl: String?): StructureMap? {
    var structureMap: StructureMap? = null
    runBlocking {
      launch {
        val structureMapId = structureMapUrl?.substringAfterLast("/")
        if (structureMapId != null) {
          structureMap = fhirEngine.load(StructureMap::class.java, structureMapId)
        }
      }
    }

    return structureMap
  }

  fun saveExtractedResources(
    context: Context,
    intent: Intent,
    questionnaire: Questionnaire,
    questionnaireResponse: QuestionnaireResponse
  ) {
    var bundle: Bundle

    viewModelScope.launch {
      val contextR4 = SimpleWorkerContextProvider.loadSimpleWorkerContext(getApplication())

      val outputs: MutableList<Base> = ArrayList()
      val transformSupportServices = TransformSupportServices(outputs, contextR4)

      bundle =
        ResourceMapper.extract(
          questionnaire,
          questionnaireResponse,
          getStructureMapProvider(context),
          context,
          transformSupportServices
        )

      // todo Assign Encounter , Observation etc their separate Ids with reference to Patient Id
      val resourceId = intent.getStringExtra(QuestionnaireActivity.QUESTIONNAIRE_ARG_PATIENT_KEY)

      saveBundleResources(bundle, resourceId)
    }
  }

  fun getStructureMapProvider(context: Context): (suspend (String) -> StructureMap?) {
    if (structureMapProvider == null) {
      structureMapProvider = { structureMapUrl: String -> fetchStructureMap(structureMapUrl) }
    }

    return structureMapProvider!!
  }
}
