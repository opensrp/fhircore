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

import android.app.AlertDialog
import android.app.Application
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Button
import androidx.core.os.bundleOf
import androidx.fragment.app.commit
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import ca.uhn.fhir.context.FhirContext
import com.google.android.fhir.datacapture.QuestionnaireFragment
import com.google.android.fhir.datacapture.QuestionnaireFragment.Companion.BUNDLE_KEY_QUESTIONNAIRE
import com.google.android.fhir.datacapture.QuestionnaireFragment.Companion.BUNDLE_KEY_QUESTIONNAIRE_RESPONSE
import com.google.android.fhir.logicalId
import kotlinx.coroutines.withContext
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.ui.base.BaseMultiLanguageActivity
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.FormConfigUtil
import org.smartregister.fhircore.engine.util.extension.assertIsConfigurable
import org.smartregister.fhircore.engine.util.extension.createFactory
import org.smartregister.fhircore.engine.util.extension.showToast
import timber.log.Timber

/**
 * Launches Questionnaire with given id. If questionnaire has subjectType = Patient his activity can
 * handle data persistence for [Patient] resources among others.
 *
 * Implement a subclass of this [QuestionnaireActivity] to provide functionality on how to
 * [handleQuestionnaireResponse]
 */
open class QuestionnaireActivity : BaseMultiLanguageActivity(), View.OnClickListener {

  val dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider

  lateinit var questionnaireConfig: QuestionnaireConfig

  lateinit var questionnaireViewModel: QuestionnaireViewModel

  protected lateinit var questionnaire: Questionnaire

  protected var clientIdentifier: String? = null

  private val parser = FhirContext.forR4().newJsonParser()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_questionnaire)
    application.assertIsConfigurable()
    if (!intent.hasExtra(QUESTIONNAIRE_ARG_FORM) && !intent.hasExtra(QUESTIONNAIRE_ARG_ID)) {
      showToast(getString(R.string.error_loading_form))
      finish()
    }

    clientIdentifier = intent.getStringExtra(QUESTIONNAIRE_ARG_PATIENT_KEY)

    lifecycleScope.launchWhenCreated {
      questionnaireViewModel = createViewModel(application)

      var questionnaireId = intent.getStringExtra(QUESTIONNAIRE_ARG_ID)

      if (questionnaireId == null) {
        val form = intent.getStringExtra(QUESTIONNAIRE_ARG_FORM)
        questionnaireConfig = getQuestionnaireConfig(form!!)
        questionnaire = questionnaireViewModel.loadQuestionnaire(questionnaireConfig.identifier)!!
      } else {
        questionnaire = questionnaireViewModel.loadQuestionnaire(questionnaireId)!!
        questionnaireConfig =
          QuestionnaireConfig(
            form = questionnaire.name,
            title = questionnaire.title,
            identifier = questionnaire.logicalId
          )
      }

      supportActionBar?.apply {
        setDisplayHomeAsUpEnabled(true)
        title = questionnaireConfig.title
      }

      // Only add the fragment once, when the activity is first created.
      if (savedInstanceState == null) {
        val fragment =
          QuestionnaireFragment().apply {
            val parsedQuestionnaire = parser.encodeResourceToString(questionnaire)
            arguments =
              when {
                clientIdentifier == null ->
                  bundleOf(Pair(BUNDLE_KEY_QUESTIONNAIRE, parsedQuestionnaire))
                clientIdentifier != null -> {
                  val parsedQuestionnaireResponse =
                    parser.encodeResourceToString(
                      questionnaireViewModel.generateQuestionnaireResponse(questionnaire!!, intent)
                    )
                  bundleOf(
                    Pair(BUNDLE_KEY_QUESTIONNAIRE, parsedQuestionnaire),
                    Pair(BUNDLE_KEY_QUESTIONNAIRE_RESPONSE, parsedQuestionnaireResponse)
                  )
                }
                else -> bundleOf()
              }
          }
        supportFragmentManager.commit { add(R.id.container, fragment, QUESTIONNAIRE_FRAGMENT_TAG) }
      }
    }

    findViewById<Button>(R.id.btn_save_client_info).setOnClickListener(this)
  }

  suspend fun getQuestionnaireConfig(form: String): QuestionnaireConfig {
    val loadConfig =
      withContext(dispatcherProvider.io()) {
        FormConfigUtil.loadConfig<List<QuestionnaireConfig>>(
          FORM_CONFIGURATIONS,
          this@QuestionnaireActivity
        )
      }

    return loadConfig.associateBy { it.form }.getValue(form)
  }

  open fun createViewModel(application: Application) =
    ViewModelProvider(
      this@QuestionnaireActivity,
      QuestionnaireViewModel(application).createFactory()
    )[QuestionnaireViewModel::class.java]

  override fun onClick(view: View) {
    if (view.id == R.id.btn_save_client_info) {
      val questionnaireFragment =
        supportFragmentManager.findFragmentByTag(QUESTIONNAIRE_FRAGMENT_TAG) as
          QuestionnaireFragment
      handleQuestionnaireResponse(questionnaireFragment.getQuestionnaireResponse())
    } else {
      showToast(getString(R.string.error_saving_form))
    }
  }

  open fun handleQuestionnaireResponse(questionnaireResponse: QuestionnaireResponse) {
    if (questionnaire != null) {
      val alertDialog = showDialog()

      questionnaireViewModel.extractionProgress.observe(
        this,
        { result ->

          // TODO: Unregister this observer

          if (result) {
            alertDialog.dismiss()
            finish()
          } else {
            Timber.e("An error occurred during extraction")
          }
        }
      )

      questionnaireViewModel.extractAndSaveResources(
        context = this@QuestionnaireActivity,
        questionnaire = questionnaire!!,
        questionnaireResponse = questionnaireResponse,
        resourceId = intent.getStringExtra(QUESTIONNAIRE_ARG_PATIENT_KEY)
      )
    }
  }

  fun showDialog(): AlertDialog {
    val dialogBuilder =
      AlertDialog.Builder(this).apply {
        setView(R.layout.dialog_saving)
        setCancelable(false)
      }

    return dialogBuilder.create().apply { show() }
  }

  companion object {
    const val QUESTIONNAIRE_TITLE_KEY = "questionnaire-title-key"
    const val QUESTIONNAIRE_POPULATION_RESOURCES = "questionnaire-population-resources"
    const val QUESTIONNAIRE_FRAGMENT_TAG = "questionnaire-fragment-tag"
    const val QUESTIONNAIRE_ARG_PATIENT_KEY = "questionnaire_patient_item_id"
    const val QUESTIONNAIRE_ARG_FORM = "questionnaire_form"
    const val QUESTIONNAIRE_ARG_ID = "questionnaire_id"
    const val FORM_CONFIGURATIONS = "form_configurations.json"

    fun requiredIntentArgs(
      clientIdentifier: String?,
      form: String? = null,
      questionnaireId: String? = null
    ) =
      bundleOf(
        Pair(QUESTIONNAIRE_ARG_PATIENT_KEY, clientIdentifier),
        Pair(QUESTIONNAIRE_ARG_FORM, form),
        Pair(QUESTIONNAIRE_ARG_ID, questionnaireId)
      )
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    return when (item.itemId) {
      android.R.id.home -> {
        onBackPressed()
        true
      }
      else -> super.onOptionsItemSelected(item)
    }
  }

  override fun onBackPressed() {
    finish()
  }
}
