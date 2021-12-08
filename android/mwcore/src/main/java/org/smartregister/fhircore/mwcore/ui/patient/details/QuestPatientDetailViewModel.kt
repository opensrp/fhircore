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

package org.smartregister.fhircore.mwcore.ui.patient.details

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.smartregister.fhircore.engine.configuration.view.SearchFilter
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireConfig
import org.smartregister.fhircore.engine.util.AssetUtil
import org.smartregister.fhircore.mwcore.data.patient.PatientRepository
import org.smartregister.fhircore.mwcore.data.patient.model.PatientItem
import org.smartregister.fhircore.mwcore.ui.patient.register.PatientItemMapper

@HiltViewModel
class QuestPatientDetailViewModel
@Inject
constructor(val patientRepository: PatientRepository, val patientItemMapper: PatientItemMapper) :
  ViewModel() {

  val patientItem = MutableLiveData<PatientItem>()
  val questionnaireConfigs = MutableLiveData<List<QuestionnaireConfig>>()
  val testResults = MutableLiveData<List<QuestionnaireResponse>>()
  val onBackPressClicked = MutableLiveData(false)
  val onMenuItemClicked = MutableLiveData(false)
  val onFormItemClicked = MutableLiveData<QuestionnaireConfig>(null)
  val onFormTestResultClicked = MutableLiveData<QuestionnaireResponse>(null)

  fun getDemographics(patientId: String) {
    viewModelScope.launch {
      patientItem.postValue(
        patientItemMapper.mapToDomainModel(patientRepository.fetchDemographics(patientId))
      )
    }
  }

  fun getAllForms(context: Context) {
    viewModelScope.launch {
      // TODO Load binary resources
      val config =
        AssetUtil.decodeAsset<ProfileConfig>(fileName = PROFILE_CONFIG, context = context)
      questionnaireConfigs.postValue(
        patientRepository.fetchTestForms(config.profileQuestionnaireFilter)
      )
    }
  }

  fun getAllResults(patientId: String) {
    viewModelScope.launch { testResults.postValue(patientRepository.fetchTestResults(patientId)) }
  }

  fun onMenuItemClickListener(menuClicked: Boolean) {
    onMenuItemClicked.value = menuClicked
  }

  fun onFormItemClickListener(questionnaireConfig: QuestionnaireConfig) {
    onFormItemClicked.value = questionnaireConfig
  }

  fun onTestResultItemClickListener(questionnaireResponse: QuestionnaireResponse) {
    onFormTestResultClicked.value = questionnaireResponse
  }

  fun onBackPressed(backPressed: Boolean) {
    onBackPressClicked.value = backPressed
  }

  companion object {
    const val PROFILE_CONFIG = "configurations/form/profile_config.json"
  }

  @Serializable data class ProfileConfig(val profileQuestionnaireFilter: SearchFilter)
}
