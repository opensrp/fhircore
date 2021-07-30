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
import kotlinx.coroutines.runBlocking
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.Resource
import org.smartregister.fhircore.FhirApplication
import org.smartregister.fhircore.activity.QuestionnaireActivity

class QuestionnaireViewModel(application: Application, private val state: SavedStateHandle) :
  AndroidViewModel(application) {

  val fhirEngine = FhirApplication.fhirEngine(getApplication())

  val questionnaire: Questionnaire
    get() {
      val id: String = state[QuestionnaireActivity.QUESTIONNAIRE_PATH_KEY]!!
      return loadQuestionnaire(id)
    }

  fun loadQuestionnaire(id: String): Questionnaire{
    return runBlocking {
      fhirEngine.load(Questionnaire::class.java, id)
    }
  }

  fun saveResource(resource: Resource) {
    viewModelScope.launch { fhirEngine.save(resource) }
  }

  fun saveObservations(resource: List<Observation>) {
    resource.stream().forEach { viewModelScope.launch { saveResource(it) } }
  }

  fun savePatient(resource: Patient) {
    viewModelScope.launch { fhirEngine.save(resource) }
  }
}
