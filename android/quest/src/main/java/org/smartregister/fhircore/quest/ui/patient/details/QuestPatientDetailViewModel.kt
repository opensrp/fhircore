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

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.smartregister.fhircore.engine.configuration.view.SearchFilter
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireConfig
import org.smartregister.fhircore.engine.util.extension.createFactory
import org.smartregister.fhircore.engine.util.extension.decodeJson
import org.smartregister.fhircore.engine.util.extension.loadBinaryResourceConfiguration
import org.smartregister.fhircore.quest.QuestApplication
import org.smartregister.fhircore.quest.QuestApplication.Companion.getProfileConfigId
import org.smartregister.fhircore.quest.data.patient.PatientRepository

class QuestPatientDetailViewModel(
  val application: QuestApplication,
  private val repository: PatientRepository,
  private val patientId: String
) : AndroidViewModel(application), QuestPatientDetailDataProvider {

  private var mOnBackPressListener: () -> Unit = {}
  private var mOnMenuItemClickListener: (menuItem: String) -> Unit = {}
  private var mOnFormItemClickListener: (item: QuestionnaireConfig) -> Unit = {}
  private var mOnTestResultItemClickListener: (item: QuestionnaireResponse) -> Unit = {}

  override fun getDemographics(): LiveData<Patient> {
    return repository.fetchDemographics(patientId)
  }

  override fun onBackPressListener(): () -> Unit {
    return mOnBackPressListener
  }

  override fun onMenuItemClickListener(): (menuItem: String) -> Unit {
    return mOnMenuItemClickListener
  }

  override fun getAllForms(): LiveData<List<QuestionnaireConfig>> {
    return runBlocking {
      val config = loadProfileConfig()

      repository.fetchTestForms(config.profileQuestionnaireFilter)
    }
  }

  override fun getAllResults(): LiveData<List<QuestionnaireResponse>> {
    return repository.fetchTestResults(patientId)
  }

  override fun onFormItemClickListener(): (item: QuestionnaireConfig) -> Unit {
    return mOnFormItemClickListener
  }

  override fun onTestResultItemClickListener(): (item: QuestionnaireResponse) -> Unit {
    return mOnTestResultItemClickListener
  }

  fun setOnBackPressListener(onBackPressListener: () -> Unit) {
    this.mOnBackPressListener = onBackPressListener
  }

  fun setOnMenuItemClickListener(onMenuItemClickListener: (menuItem: String) -> Unit) {
    this.mOnMenuItemClickListener = onMenuItemClickListener
  }

  fun setOnFormItemClickListener(onFormItemClickListener: (item: QuestionnaireConfig) -> Unit) {
    this.mOnFormItemClickListener = onFormItemClickListener
  }

  fun setOnTestResultItemClickListener(
    onTestResultItemClickListener: (item: QuestionnaireResponse) -> Unit
  ) {
    this.mOnTestResultItemClickListener = onTestResultItemClickListener
  }

  private suspend fun loadProfileConfig(): ProfileConfig {
    return application.loadBinaryResourceConfiguration<ProfileConfig>(getProfileConfigId())
      ?: application.assets.open(PROFILE_CONFIG).bufferedReader().use { it.readText() }.decodeJson()
  }

  companion object {
    const val PROFILE_CONFIG = "profile_config.json"

    fun get(
      owner: ViewModelStoreOwner,
      application: QuestApplication,
      repository: PatientRepository,
      patientId: String
    ): QuestPatientDetailViewModel {
      return ViewModelProvider(
        owner,
        QuestPatientDetailViewModel(application, repository, patientId).createFactory()
      )[QuestPatientDetailViewModel::class.java]
    }
  }

  @Serializable data class ProfileConfig(val profileQuestionnaireFilter: SearchFilter)
}
