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

package org.smartregister.fhircore.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Resource
import org.smartregister.fhircore.FhirApplication
import org.smartregister.fhircore.activity.QuestionnaireActivity

class QuestionnaireViewModel(application: Application, private val state: SavedStateHandle) :
  AndroidViewModel(application) {

  var questionnaireJson: String? = null
  val questionnaire: String
    get() {
      if (questionnaireJson == null) {
        questionnaireJson =
          getApplication<Application>()
            .assets
            .open(state[QuestionnaireActivity.QUESTIONNAIRE_FILE_PATH_KEY]!!)
            .bufferedReader()
            .use { it.readText() }
      }
      return questionnaireJson!!
    }

  fun saveResource(resource: Resource) {
    viewModelScope.launch { FhirApplication.fhirEngine(getApplication()).save(resource) }
  }

  fun saveObservations(resource: List<Observation>) {
    resource.stream().forEach { viewModelScope.launch { saveResource(it) } }
  }

  fun savePatient(resource: Patient) {
    viewModelScope.launch { FhirApplication.fhirEngine(getApplication()).save(resource) }
  }
}
