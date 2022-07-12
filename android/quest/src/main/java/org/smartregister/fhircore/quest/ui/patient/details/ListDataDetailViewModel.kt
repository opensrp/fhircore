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

package org.smartregister.fhircore.quest.ui.patient.details

import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.getLocalizedText
import com.google.android.fhir.logicalId
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.configuration.view.SearchFilter
import org.smartregister.fhircore.engine.cql.LibraryEvaluator
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireConfig
import org.smartregister.fhircore.quest.configuration.view.DataDetailsListViewConfiguration
import org.smartregister.fhircore.quest.data.patient.PatientRepository
import org.smartregister.fhircore.quest.data.patient.model.PatientItem
import org.smartregister.fhircore.quest.data.patient.model.QuestResultItem
import org.smartregister.fhircore.quest.util.mappers.PatientItemMapper

@HiltViewModel
class ListDataDetailViewModel
@Inject
constructor(
  val patientRepository: PatientRepository,
  val defaultRepository: DefaultRepository,
  val patientItemMapper: PatientItemMapper,
  val libraryEvaluator: LibraryEvaluator,
  val fhirEngine: FhirEngine
) : ViewModel() {

  private val _patientDetailsViewConfiguration = MutableLiveData<DataDetailsListViewConfiguration>()
  val patientDetailsViewConfiguration: LiveData<DataDetailsListViewConfiguration>
    get() = _patientDetailsViewConfiguration

  val patientItem = MutableLiveData<PatientItem>()
  val questionnaireConfigs = MutableLiveData<List<QuestionnaireConfig>>()
  val testResults = MutableLiveData<List<QuestResultItem>>()
  val onBackPressClicked = MutableLiveData(false)
  val onMenuItemClicked = MutableLiveData(-1)
  val onFormItemClicked = MutableLiveData<QuestionnaireConfig>(null)
  val onFormTestResultClicked = MutableLiveData<QuestResultItem?>(null)

  fun getDemographicsWithAdditionalData(
    patientId: String,
    patientDetailsViewConfiguration: DataDetailsListViewConfiguration
  ) {
    viewModelScope.launch {
      val demographic = patientRepository.fetchDemographicsWithAdditionalData(patientId)
      demographic.additionalData?.forEach {
        it.valuePrefix = patientDetailsViewConfiguration.valuePrefix
      }
      patientItem.postValue(demographic)
    }
  }

  fun getAllForms(searchFilter: SearchFilter) {
    viewModelScope.launch {
      questionnaireConfigs.postValue(patientRepository.fetchTestForms(searchFilter))
    }
  }

  fun getAllResults(
    subjectId: String,
    subjectType: ResourceType,
    searchFilter: SearchFilter,
    patientDetailsViewConfiguration: DataDetailsListViewConfiguration
  ) {
    viewModelScope.launch {
      val forms = patientRepository.fetchTestForms(searchFilter)

      testResults.postValue(
        patientRepository.fetchTestResults(
          subjectId,
          subjectType,
          forms,
          patientDetailsViewConfiguration
        )
      )
    }
  }

  fun onMenuItemClickListener(@StringRes id: Int) {
    onMenuItemClicked.value = id
  }

  fun onFormItemClickListener(questionnaireConfig: QuestionnaireConfig) {
    onFormItemClicked.value = questionnaireConfig
  }

  fun onTestResultItemClickListener(resultItem: QuestResultItem?) {
    onFormTestResultClicked.value = resultItem
  }

  fun onBackPressed(backPressed: Boolean) {
    onBackPressClicked.value = backPressed
  }

  fun fetchResultItemLabel(testResult: Pair<QuestionnaireResponse, Questionnaire>): String {
    return testResult.second.titleElement.getLocalizedText()
      ?: testResult.second.nameElement.getLocalizedText() ?: testResult.second.logicalId
  }

  fun updateViewConfigurations(patientDetailsViewConfiguration: DataDetailsListViewConfiguration) {
    _patientDetailsViewConfiguration.value = patientDetailsViewConfiguration
  }

  fun fetchPatientResources(patientId: String): LiveData<ArrayList<String>> {
    val resourceListLive = MutableLiveData<ArrayList<String>>()
    val resourceList = arrayListOf<String>()
    viewModelScope.launch {
      val activePregnancyConditionString =
        patientRepository.fetchPregnancyCondition(patientId = patientId)
      if (activePregnancyConditionString.isNotEmpty())
        resourceList.add(activePregnancyConditionString)
      resourceListLive.postValue(resourceList)
    }
    return resourceListLive
  }
}
