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

import android.content.Context
import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.logicalId
import com.google.android.fhir.search.search
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.hl7.fhir.r4.model.Condition
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.Resource
import org.smartregister.fhircore.engine.configuration.view.SearchFilter
import org.smartregister.fhircore.engine.cql.LibraryEvaluator
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireConfig
import org.smartregister.fhircore.engine.util.AssetUtil
import org.smartregister.fhircore.engine.util.extension.isPatient
import org.smartregister.fhircore.quest.configuration.parser.DetailConfigParser
import org.smartregister.fhircore.quest.configuration.view.PatientDetailsViewConfiguration
import org.smartregister.fhircore.quest.data.patient.PatientRepository
import org.smartregister.fhircore.quest.data.patient.model.PatientItem
import org.smartregister.fhircore.quest.data.patient.model.ResultItem
import org.smartregister.fhircore.quest.ui.patient.register.PatientItemMapper

@HiltViewModel
class QuestPatientDetailViewModel
@Inject
constructor(
  val patientRepository: PatientRepository,
  val defaultRepository: DefaultRepository,
  val patientItemMapper: PatientItemMapper,
  val libraryEvaluator: LibraryEvaluator,
  val fhirEngine: FhirEngine
) : ViewModel() {

  private val _patientDetailsViewConfiguration = MutableLiveData<PatientDetailsViewConfiguration>()
  val patientDetailsViewConfiguration: LiveData<PatientDetailsViewConfiguration>
    get() = _patientDetailsViewConfiguration

  val patientItem = MutableLiveData<PatientItem>()
  val questionnaireConfigs = MutableLiveData<List<QuestionnaireConfig>>()
  val testResults = MutableLiveData<List<ResultItem>>()
  val onBackPressClicked = MutableLiveData(false)
  val onMenuItemClicked = MutableLiveData(-1)
  val onFormItemClicked = MutableLiveData<QuestionnaireConfig>(null)
  val onFormTestResultClicked = MutableLiveData<ResultItem?>(null)

  fun getDemographicsWithAdditionalData(
    patientId: String,
    patientDetailsViewConfiguration: PatientDetailsViewConfiguration
  ) {
    viewModelScope.launch {
      val demographic = patientRepository.fetchDemographicsWithAdditionalData(patientId)
      demographic.additionalData?.forEach {
        it.valuePrefix = patientDetailsViewConfiguration.valuePrefix
      }
      patientItem.postValue(demographic)
    }
  }

  fun getAllForms(
    profileConfig: ProfileConfig,
    patientDetailsViewConfiguration: PatientDetailsViewConfiguration
  ) {
    viewModelScope.launch {
      questionnaireConfigs.postValue(
        patientRepository.fetchTestForms(
          profileConfig.profileQuestionnaireFilter,
          patientDetailsViewConfiguration.appId
        )
      )
    }
  }

  fun getAllResults(
    patientId: String,
    profileConfig: ProfileConfig,
    patientDetailsViewConfiguration: PatientDetailsViewConfiguration,
    parser: DetailConfigParser?
  ) {
    viewModelScope.launch {
      val forms =
        patientRepository.fetchTestForms(
          profileConfig.profileQuestionnaireFilter,
          patientDetailsViewConfiguration.appId
        )

      testResults.postValue(
        patientRepository.fetchTestResults(
          patientId,
          forms,
          patientDetailsViewConfiguration,
          parser
        )
      )
    }
  }

  suspend fun getAllDataFor(patientId: String): List<Resource> {
    val fhirEngine = patientRepository.fhirEngine
    val patient = fhirEngine.load(Patient::class.java, patientId)
    val observations =
      fhirEngine.search<Observation> {
        filter(Observation.SUBJECT) { this.value = "Patient/$patientId" }
      }
    val conditions =
      fhirEngine.search<Condition> {
        filter(Condition.SUBJECT) { this.value = "Patient/$patientId" }
      }

    return mutableListOf<Resource>().apply {
      add(patient)
      addAll(observations)
      addAll(conditions)
    }
  }

  fun runCqlFor(patientId: String, context: Context): MutableLiveData<String?> {
    val result = MutableLiveData("")
    viewModelScope.launch {
      val dataBundle = getAllDataFor(patientId)
      val config =
        AssetUtil.decodeAsset<ProfileConfig>(fileName = PROFILE_CONFIG, context = context)

      val patient = dataBundle.first { it.isPatient(patientId) }
      val otherResources = dataBundle.filterNot { it.isPatient(patientId) }

      val data =
        kotlin
          .runCatching {
            libraryEvaluator
              .runCqlLibrary(
                libraryId = config.cqlProfileLibraryFilter.code,
                patient = patient as Patient,
                resources = otherResources,
                repository = defaultRepository
              )
              .joinToString("\n")
          }
          .onFailure { result.postValue(it.stackTraceToString()) }
          .getOrNull()
      result.postValue(data)
    }
    return result
  }

  fun onMenuItemClickListener(@StringRes id: Int) {
    onMenuItemClicked.value = id
  }

  fun onFormItemClickListener(questionnaireConfig: QuestionnaireConfig) {
    onFormItemClicked.value = questionnaireConfig
  }

  fun onTestResultItemClickListener(resultItem: ResultItem?) {
    onFormTestResultClicked.value = resultItem
  }

  fun onBackPressed(backPressed: Boolean) {
    onBackPressClicked.value = backPressed
  }

  fun fetchResultItemLabel(testResult: Pair<QuestionnaireResponse, Questionnaire>): String {
    return testResult.second.name ?: testResult.second.title ?: testResult.second.logicalId
  }

  fun updateViewConfigurations(patientDetailsViewConfiguration: PatientDetailsViewConfiguration) {
    _patientDetailsViewConfiguration.value = patientDetailsViewConfiguration
  }

  fun loadParser(
    packageName: String,
    patientDetailsViewConfiguration: PatientDetailsViewConfiguration
  ): DetailConfigParser {
    return Class.forName(
        "$packageName.configuration.parser.${patientDetailsViewConfiguration.parser}"
      )
      .getConstructor(FhirEngine::class.java)
      .newInstance(fhirEngine) as
      DetailConfigParser
  }

  companion object {
    const val PROFILE_CONFIG = "configurations/form/profile_config.json"
  }

  @Serializable
  data class ProfileConfig(
    val profileQuestionnaireFilter: SearchFilter,
    val cqlHelperLibraryFilter: SearchFilter,
    val cqlProfileLibraryFilter: SearchFilter
  )
}
