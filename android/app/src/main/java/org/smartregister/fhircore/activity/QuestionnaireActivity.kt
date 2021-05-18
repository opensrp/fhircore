/*
 * Copyright 2021 Ona Systems Inc
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
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.fragment.app.commit
import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.parser.IParser
import com.google.android.fhir.datacapture.QuestionnaireFragment
import com.google.android.fhir.datacapture.mapping.ResourceMapper
import java.util.UUID
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.Task
import org.smartregister.fhircore.FhirApplication
import org.smartregister.fhircore.R
import org.smartregister.fhircore.viewmodel.QuestionnaireViewModel
import org.smartregister.fhircore.viewmodel.ResourceSaveListener

class QuestionnaireActivity : AppCompatActivity() {
  private val viewModel: QuestionnaireViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_questionnaire)

    supportActionBar!!.apply {
      title = intent.getStringExtra(QUESTIONNAIRE_TITLE_KEY)
      setDisplayHomeAsUpEnabled(true)
    }

    // Only add the fragment once, when the activity is first created.
    if (savedInstanceState == null) {
      val fragment = QuestionnaireFragment()
      fragment.arguments =
        bundleOf(QuestionnaireFragment.BUNDLE_KEY_QUESTIONNAIRE to viewModel.questionnaire)

      supportFragmentManager.commit { add(R.id.container, fragment, QUESTIONNAIRE_FRAGMENT_TAG) }
    }
  }

  override fun onCreateOptionsMenu(menu: Menu?): Boolean {
    menuInflater.inflate(R.menu.top_bar_menu, menu)
    return true
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    return when (item.getItemId()) {
      R.id.action_submit -> {
        val questionnaireFragment =
          supportFragmentManager.findFragmentByTag(QUESTIONNAIRE_FRAGMENT_TAG) as
            QuestionnaireFragment
        savePatientResource(questionnaireFragment.getQuestionnaireResponse())
        true
      }
      else -> super.onOptionsItemSelected(item)
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

    patient.id = UUID.randomUUID().toString().toLowerCase()
    // FhirApplication.fhirEngine(applicationContext).save(patient)
    viewModel.saveResource(
      patient,
      listOf(
        object : ResourceSaveListener {
          override suspend fun afterSave(resource: Resource) {
            // generate a task
            FhirApplication.fhirEngine(application).save(
              Task().apply {
                status = Task.TaskStatus.READY
              }
            )
          }
        }
      )
    )

    this.startActivity(Intent(this, PatientListActivity::class.java))
  }

  companion object {
    const val QUESTIONNAIRE_TITLE_KEY = "questionnaire-title-key"
    const val QUESTIONNAIRE_FILE_PATH_KEY = "questionnaire-file-path-key"
    const val QUESTIONNAIRE_FRAGMENT_TAG = "questionnaire-fragment-tag"
  }
}
