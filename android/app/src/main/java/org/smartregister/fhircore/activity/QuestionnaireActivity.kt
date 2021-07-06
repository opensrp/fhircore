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

package org.smartregister.fhircore.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.viewModels
import androidx.core.os.bundleOf
import androidx.fragment.app.commit
import androidx.lifecycle.viewModelScope
import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.parser.IParser
import com.google.android.fhir.datacapture.QuestionnaireFragment
import com.google.android.fhir.datacapture.mapping.ResourceMapper
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.smartregister.fhircore.FhirApplication
import org.smartregister.fhircore.R
import org.smartregister.fhircore.fragment.PatientDetailFragment
import org.smartregister.fhircore.viewmodel.QuestionnaireViewModel

class QuestionnaireActivity : MultiLanguageBaseActivity() {
  private val viewModel: QuestionnaireViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_questionnaire)

    supportActionBar!!.hide()

    // Only add the fragment once, when the activity is first created.
    if (savedInstanceState == null) {
      val fragment = QuestionnaireFragment()

      intent.getStringExtra(PatientDetailFragment.ARG_ITEM_ID)?.let {
        fragment.arguments =
          bundleOf(
            QuestionnaireFragment.BUNDLE_KEY_QUESTIONNAIRE to viewModel.questionnaire,
            QuestionnaireFragment.BUNDLE_KEY_QUESTIONNAIRE_RESPONSE to getQuestionnaireResponse()
          )
      }
        ?: kotlin.run {
          fragment.arguments =
            bundleOf(QuestionnaireFragment.BUNDLE_KEY_QUESTIONNAIRE to viewModel.questionnaire)
        }

      supportFragmentManager.commit { add(R.id.container, fragment, QUESTIONNAIRE_FRAGMENT_TAG) }
    }

    findViewById<Button>(R.id.btn_save_client_info).setOnClickListener {
      val questionnaireFragment =
        supportFragmentManager.findFragmentByTag(QUESTIONNAIRE_FRAGMENT_TAG) as
          QuestionnaireFragment
      savePatientResource(questionnaireFragment.getQuestionnaireResponse())
    }
  }

  fun savePatientResource(questionnaireResponse: QuestionnaireResponse) {

    val iParser: IParser = FhirContext.forR4().newJsonParser()
    val questionnaire =
      iParser.parseResource(
        org.hl7.fhir.r4.model.Questionnaire::class.java,
        viewModel.questionnaire
      ) as
        Questionnaire

    val patient = ResourceMapper.extract(questionnaire, questionnaireResponse) as Patient

    patient.id =
      intent.getStringExtra(PatientDetailFragment.ARG_ITEM_ID) ?: patient.name.first().family

    viewModel.savePatient(patient)

    this.startActivity(Intent(this, PatientListActivity::class.java))
  }

  private fun getQuestionnaireResponse(): String {
    var response = QuestionnaireResponse()
    val questionnaire =
      FhirContext.forR4().newJsonParser().parseResource(viewModel.questionnaire) as Questionnaire

    intent.getStringExtra(PatientDetailFragment.ARG_ITEM_ID)?.let {
      var patient: Patient? = null
      viewModel.viewModelScope.launch {
        patient = FhirApplication.fhirEngine(applicationContext).load(Patient::class.java, it)
      }

      patient?.let {
        response = ResourceMapper.populate(questionnaire, it)
        response.find("patient-0-gender")?.apply {
          if (answer?.singleOrNull()?.value.toString() == "male") {
            val genderCoding = Coding("", "male", "Male")
            answer[0].value = genderCoding
          } else {
            val genderCoding = Coding("", "female", "Female")
            answer[0].value = genderCoding
          }
        }
      }
    }

    return FhirContext.forR4().newJsonParser().encodeResourceToString(response)
  }

  private fun Questionnaire.find(linkId: String): Questionnaire.QuestionnaireItemComponent? {
    return item.find(linkId, null)
  }

  private fun List<Questionnaire.QuestionnaireItemComponent>.find(
    linkId: String,
    default: Questionnaire.QuestionnaireItemComponent?
  ): Questionnaire.QuestionnaireItemComponent? {
    var result = default
    run loop@{
      forEach {
        if (it.linkId == linkId) {
          result = it
          return@loop
        } else if (it.item.isNotEmpty()) {
          result = it.item.find(linkId, result)
        }
      }
    }

    return result
  }

  private fun List<QuestionnaireResponse.QuestionnaireResponseItemComponent>.find(
    linkId: String,
    default: QuestionnaireResponse.QuestionnaireResponseItemComponent?
  ): QuestionnaireResponse.QuestionnaireResponseItemComponent? {
    var result = default
    run loop@{
      forEach {
        if (it.linkId == linkId) {
          result = it
          return@loop
        } else if (it.item.isNotEmpty()) {
          result = it.item.find(linkId, result)
        }
      }
    }

    return result
  }

  private fun QuestionnaireResponse.find(
    linkId: String
  ): QuestionnaireResponse.QuestionnaireResponseItemComponent? {
    return item.find(linkId, null)
  }

  companion object {
    const val QUESTIONNAIRE_TITLE_KEY = "questionnaire-title-key"
    const val QUESTIONNAIRE_FILE_PATH_KEY = "questionnaire-file-path-key"
    const val QUESTIONNAIRE_FRAGMENT_TAG = "questionannire-fragment-tag"
  }
}
