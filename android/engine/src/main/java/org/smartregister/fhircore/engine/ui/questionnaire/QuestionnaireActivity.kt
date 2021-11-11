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
import com.google.android.fhir.FhirEngineProvider.fhirEngine
import com.google.android.fhir.datacapture.QuestionnaireFragment
import com.google.android.fhir.datacapture.QuestionnaireFragment.Companion.BUNDLE_KEY_QUESTIONNAIRE
import com.google.android.fhir.datacapture.QuestionnaireFragment.Companion.BUNDLE_KEY_QUESTIONNAIRE_RESPONSE
import com.google.android.fhir.datacapture.validation.QuestionnaireResponseValidator
import com.google.android.fhir.db.ResourceNotFoundException
import com.google.android.fhir.logicalId
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.StringType
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.configuration.app.ConfigurableApplication
import org.smartregister.fhircore.engine.ui.base.AlertDialogue
import org.smartregister.fhircore.engine.ui.base.AlertDialogue.showConfirmAlert
import org.smartregister.fhircore.engine.ui.base.AlertDialogue.showProgressAlert
import org.smartregister.fhircore.engine.ui.base.BaseMultiLanguageActivity
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.engine.util.DispatcherProvider
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

  protected var immunizationId: String? = null

  var readOnly: Boolean = false

  private val parser = FhirContext.forR4().newJsonParser()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_questionnaire)
    application.assertIsConfigurable()
    if (!intent.hasExtra(QUESTIONNAIRE_ARG_FORM)) {
      showToast(getString(R.string.error_loading_form))
      finish()
    }

    val loadProgress = showProgressAlert(this, R.string.loading)

    clientIdentifier = intent.getStringExtra(QUESTIONNAIRE_ARG_PATIENT_KEY)
    immunizationId = intent.getStringExtra(ADVERSE_EVENT_IMMUNIZATION_ITEM_KEY)

    lifecycleScope.launchWhenCreated {
      readOnly = intent.getBooleanExtra(QUESTIONNAIRE_READ_ONLY, false)
      questionnaireViewModel = createViewModel(application, readOnly)

      val formName = intent.getStringExtra(QUESTIONNAIRE_ARG_FORM)!!
      // form is either name of form in asset/form-config or questionnaire-id
      // load from assets and get questionnaire or if not found build it from questionnaire
      questionnaireConfig =
        kotlin.runCatching { questionnaireViewModel.getQuestionnaireConfig(formName) }.getOrElse {
          // load questionnaire from db and build config
          questionnaire = questionnaireViewModel.loadQuestionnaire(formName)!!

          QuestionnaireConfig(
            appId = (application as ConfigurableApplication).configurationRegistry.appId,
            form = questionnaire.name ?: "",
            title = questionnaire.title ?: "",
            identifier = questionnaire.logicalId
          )
        }

      // if questionnaire is still not initialized load using config loaded from assets
      if (!::questionnaire.isInitialized)
        questionnaire = questionnaireViewModel.loadQuestionnaire(questionnaireConfig.identifier)!!

      supportActionBar?.apply {
        setDisplayHomeAsUpEnabled(true)
        title = questionnaireConfig.title
      }

      findViewById<Button>(R.id.btn_save_client_info).apply {
        setOnClickListener(this@QuestionnaireActivity)
        if (readOnly) {
          text = context.getString(R.string.done)
        }
      }

      // Only add the fragment once, when the activity is first created.
      if (savedInstanceState == null) {
        val fragment =
          FhirCoreQuestionnaireFragment().apply {
            val questionnaireString = parser.encodeResourceToString(questionnaire)
            arguments =
              when {
                clientIdentifier == null -> {
                  bundleOf(Pair(BUNDLE_KEY_QUESTIONNAIRE, questionnaireString)).apply {
                    val questionnaireResponse = intent.getStringExtra(QUESTIONNAIRE_RESPONSE)
                    if (readOnly && questionnaireResponse != null) {
                      putString(BUNDLE_KEY_QUESTIONNAIRE_RESPONSE, questionnaireResponse)
                    }
                  }
                }
                clientIdentifier != null -> {
                  try {
                    fhirEngine.load(Patient::class.java, clientIdentifier!!)
                  } catch (e: ResourceNotFoundException) {
                    setBarcode(questionnaire, clientIdentifier!!, true)
                  }

                  val parsedQuestionnaireResponse =
                    parser.encodeResourceToString(
                      questionnaireViewModel.generateQuestionnaireResponse(questionnaire!!, intent)
                    )
                  bundleOf(
                    Pair(BUNDLE_KEY_QUESTIONNAIRE, parser.encodeResourceToString(questionnaire)),
                    Pair(BUNDLE_KEY_QUESTIONNAIRE_RESPONSE, parsedQuestionnaireResponse)
                  )
                }
                else -> bundleOf(Pair(BUNDLE_KEY_QUESTIONNAIRE, questionnaireString))
              }
          }
        supportFragmentManager.commit { add(R.id.container, fragment, QUESTIONNAIRE_FRAGMENT_TAG) }
      }
      loadProgress.dismiss()
    }
  }

  private fun setBarcode(questionnaire: Questionnaire, code: String, readonly: Boolean) {
    questionnaire.find(QUESTIONNAIRE_ARG_BARCODE_KEY)?.apply {
      initial =
        mutableListOf(Questionnaire.QuestionnaireItemInitialComponent().setValue(StringType(code)))
      readOnly = readonly
    }
  }

  private fun Questionnaire.find(linkId: String): Questionnaire.QuestionnaireItemComponent? {
    return item.find(linkId, null)
  }
  private fun List<Questionnaire.QuestionnaireItemComponent>.find(
    linkId: String,
    default: Questionnaire.QuestionnaireItemComponent?
  ): Questionnaire.QuestionnaireItemComponent? {
    var result = default
    forEach {
      if (it.linkId == linkId) {
        return it
      } else if (it.item.isNotEmpty()) {
        result = it.item.find(linkId, result)
      }
    }

    return result
  }

  open fun createViewModel(application: Application, readOnly: Boolean = false) =
    ViewModelProvider(
      this@QuestionnaireActivity,
      QuestionnaireViewModel(application, readOnly).createFactory()
    )[QuestionnaireViewModel::class.java]

  override fun onClick(view: View) {
    if (view.id == R.id.btn_save_client_info) {
      if (readOnly) {
        finish()
      } else {
        showConfirmAlert(
          context = this,
          message = R.string.questionnaire_alert_submit_message,
          title = R.string.questionnaire_alert_submit_title,
          confirmButtonListener = { handleQuestionnaireSubmit() },
          confirmButtonText = R.string.questionnaire_alert_submit_button_title
        )
      }
    } else {
      showToast(getString(R.string.error_saving_form))
    }
  }

  fun getQuestionnaireResponse(): QuestionnaireResponse {
    val questionnaireFragment =
      supportFragmentManager.findFragmentByTag(QUESTIONNAIRE_FRAGMENT_TAG) as QuestionnaireFragment
    return questionnaireFragment.getQuestionnaireResponse()
  }

  private lateinit var saveProcessingAlertDialog: AlertDialog

  fun dismissSaveProcessing() {
    saveProcessingAlertDialog.dismiss()
  }

  open fun handleQuestionnaireSubmit() {
    saveProcessingAlertDialog = showProgressAlert(this, R.string.saving_registration)

    val questionnaireResponse = getQuestionnaireResponse()
    if (!validQuestionnaireResponse(questionnaireResponse)) {
      saveProcessingAlertDialog.dismiss()

      AlertDialogue.showErrorAlert(
        this,
        R.string.questionnaire_alert_invalid_message,
        R.string.questionnaire_alert_invalid_title
      )
      return
    }

    handleQuestionnaireResponse(questionnaireResponse)

    questionnaireViewModel.extractionProgress.observe(
      this,
      { result ->
        saveProcessingAlertDialog.dismiss()
        if (result) {
          finish()
        } else {
          Timber.e("An error occurred during extraction")
        }
      }
    )
  }

  fun validQuestionnaireResponse(questionnaireResponse: QuestionnaireResponse): Boolean {
    return QuestionnaireResponseValidator.validate(
        questionnaire.item,
        questionnaireResponse.item,
        this
      )
      .values
      .flatten()
      .all { it.isValid }
  }

  open fun handleQuestionnaireResponse(questionnaireResponse: QuestionnaireResponse) {
    questionnaireViewModel.extractAndSaveResources(
      context = this@QuestionnaireActivity,
      questionnaire = questionnaire!!,
      questionnaireResponse = questionnaireResponse,
      resourceId = intent.getStringExtra(QUESTIONNAIRE_ARG_PATIENT_KEY)
    )
  }

  companion object {
    const val QUESTIONNAIRE_TITLE_KEY = "questionnaire-title-key"
    const val QUESTIONNAIRE_POPULATION_RESOURCES = "questionnaire-population-resources"
    const val QUESTIONNAIRE_FRAGMENT_TAG = "questionnaire-fragment-tag"
    const val QUESTIONNAIRE_ARG_PATIENT_KEY = "questionnaire_patient_item_id"
    const val ADVERSE_EVENT_IMMUNIZATION_ITEM_KEY = "adverse_event_immunization_item_id"
    const val FORM_CONFIGURATIONS = "configurations/form/form_configurations.json"
    const val QUESTIONNAIRE_ARG_FORM = "questionnaire-form-name"
    const val QUESTIONNAIRE_READ_ONLY = "read-only"
    const val QUESTIONNAIRE_RESPONSE = "questionnaire-response"
    const val QUESTIONNAIRE_ARG_BARCODE_KEY = "patient-barcode"
    const val WHO_IDENTIFIER_SYSTEM = "WHO-HCID"

    fun intentArgs(
      clientIdentifier: String? = null,
      formName: String,
      readOnly: Boolean = false,
      questionnaireResponse: QuestionnaireResponse? = null,
      immunizationId: String? = null,
      populationResources: ArrayList<Resource> = ArrayList()
    ) =
      bundleOf(
        Pair(QUESTIONNAIRE_ARG_PATIENT_KEY, clientIdentifier),
        Pair(QUESTIONNAIRE_ARG_FORM, formName),
        Pair(QUESTIONNAIRE_READ_ONLY, readOnly),
        Pair(ADVERSE_EVENT_IMMUNIZATION_ITEM_KEY, immunizationId)
      )
        .apply {
          val jsonParser = FhirContext.forR4().newJsonParser()
          if (questionnaireResponse != null) {
            putString(
              QUESTIONNAIRE_RESPONSE,
              jsonParser.encodeResourceToString(questionnaireResponse)
            )
          }

          val resourcesList = ArrayList<String>()
          populationResources.forEach { resource ->
            resourcesList.add(jsonParser.encodeResourceToString(resource))
          }

          if (resourcesList.isNotEmpty()) {
            putStringArrayList(QUESTIONNAIRE_POPULATION_RESOURCES, resourcesList)
          }
        }
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
    if (readOnly) {
      finish()
    } else {
      showConfirmAlert(
        this,
        R.string.questionnaire_alert_back_pressed_message,
        R.string.questionnaire_alert_back_pressed_title,
        { finish() },
        R.string.questionnaire_alert_back_pressed_button_title
      )
    }
  }
}
