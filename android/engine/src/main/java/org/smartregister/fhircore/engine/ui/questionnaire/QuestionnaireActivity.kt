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

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Button
import androidx.activity.viewModels
import androidx.core.os.bundleOf
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import ca.uhn.fhir.context.FhirContext
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.datacapture.QuestionnaireFragment
import com.google.android.fhir.datacapture.QuestionnaireFragment.Companion.EXTRA_QUESTIONNAIRE_JSON_STRING
import com.google.android.fhir.datacapture.QuestionnaireFragment.Companion.EXTRA_QUESTIONNAIRE_RESPONSE_JSON_STRING
import com.google.android.fhir.datacapture.validation.QuestionnaireResponseValidator
import com.google.android.fhir.db.ResourceNotFoundException
import com.google.android.fhir.logicalId
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.StringType
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.ui.base.AlertDialogue
import org.smartregister.fhircore.engine.ui.base.AlertDialogue.showConfirmAlert
import org.smartregister.fhircore.engine.ui.base.AlertDialogue.showProgressAlert
import org.smartregister.fhircore.engine.ui.base.BaseMultiLanguageActivity
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.extension.FieldType
import org.smartregister.fhircore.engine.util.extension.find
import org.smartregister.fhircore.engine.util.extension.showToast
import timber.log.Timber

/**
 * Launches Questionnaire with given id. If questionnaire has subjectType = Patient his activity can
 * handle data persistence for [Patient] resources among others.
 *
 * Implement a subclass of this [QuestionnaireActivity] to provide functionality on how to
 * [handleQuestionnaireResponse]
 */
@AndroidEntryPoint
open class QuestionnaireActivity : BaseMultiLanguageActivity(), View.OnClickListener {

  @Inject lateinit var configurationRegistry: ConfigurationRegistry

  @Inject lateinit var dispatcherProvider: DispatcherProvider

  @Inject lateinit var fhirEngine: FhirEngine

  open val questionnaireViewModel: QuestionnaireViewModel by viewModels()

  lateinit var questionnaireConfig: QuestionnaireConfig

  protected lateinit var questionnaire: Questionnaire
  protected var clientIdentifier: String? = null
  protected var immunizationId: String? = null
  var readOnly: Boolean = false
  var editMode = false
  lateinit var fragment: FhirCoreQuestionnaireFragment
  private val parser = FhirContext.forR4Cached().newJsonParser()

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    outState.clear()
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_questionnaire)
    if (!intent.hasExtra(QUESTIONNAIRE_ARG_FORM)) {
      showToast(getString(R.string.error_loading_form))
      finish()
    }

    val loadProgress = showProgressAlert(this, R.string.loading)

    clientIdentifier = intent.getStringExtra(QUESTIONNAIRE_ARG_PATIENT_KEY)
    immunizationId = intent.getStringExtra(ADVERSE_EVENT_IMMUNIZATION_ITEM_KEY)

    lifecycleScope.launchWhenCreated {
      readOnly = intent.getBooleanExtra(QUESTIONNAIRE_READ_ONLY, false)
      editMode = intent.getBooleanExtra(QUESTIONNAIRE_EDIT_MODE, false)

      val formName = intent.getStringExtra(QUESTIONNAIRE_ARG_FORM)!!
      // form is either name of form in asset/form-config or questionnaire-id
      // load from assets and get questionnaire or if not found build it from questionnaire
      questionnaireConfig =
        kotlin
          .runCatching {
            questionnaireViewModel.getQuestionnaireConfig(formName, this@QuestionnaireActivity)
          }
          .getOrElse {
            // load questionnaire from db and build config
            questionnaire = questionnaireViewModel.loadQuestionnaire(formName, readOnly)!!

            QuestionnaireConfig(
              appId = configurationRegistry.appId,
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
        if (readOnly || questionnaire.experimental) {
          text = context.getString(R.string.done)
        } else if (editMode) {
          text = getString(R.string.edit)
        }
      }

      // Only add the fragment once, when the activity is first created.
      if (savedInstanceState == null) {
        renderFragment()
      }
      loadProgress.dismiss()
    }
  }

  private suspend fun renderFragment() {
    fragment =
      FhirCoreQuestionnaireFragment().apply {
        populateInitialValues(questionnaire)

        val questionnaireString = parser.encodeResourceToString(questionnaire)

        // Generate Fragment bundle arguments. This is the Questionnaire & QuestionnaireResponse
        arguments =
          when {
            clientIdentifier == null -> {
              bundleOf(Pair(EXTRA_QUESTIONNAIRE_JSON_STRING, questionnaireString)).apply {
                val questionnaireResponse = intent.getStringExtra(QUESTIONNAIRE_RESPONSE)
                if (readOnly && questionnaireResponse != null) {
                  putString(EXTRA_QUESTIONNAIRE_RESPONSE_JSON_STRING, questionnaireResponse)
                }
              }
            }
            clientIdentifier != null -> {
              try {
                fhirEngine.load(Patient::class.java, clientIdentifier!!)
              } catch (e: ResourceNotFoundException) {
                setBarcode(questionnaire, clientIdentifier!!, true)
              }

              val serializedQuestionnaireResponse =
                parser.encodeResourceToString(
                  questionnaireViewModel.generateQuestionnaireResponse(questionnaire, intent)
                )

              bundleOf(
                Pair(EXTRA_QUESTIONNAIRE_JSON_STRING, parser.encodeResourceToString(questionnaire)),
                Pair(EXTRA_QUESTIONNAIRE_RESPONSE_JSON_STRING, serializedQuestionnaireResponse)
              )
            }
            else -> bundleOf(Pair(EXTRA_QUESTIONNAIRE_JSON_STRING, questionnaireString))
          }
      }
    supportFragmentManager.commit { add(R.id.container, fragment, QUESTIONNAIRE_FRAGMENT_TAG) }
  }

  private fun setBarcode(questionnaire: Questionnaire, code: String, readonly: Boolean) {
    questionnaire.find(QUESTIONNAIRE_ARG_BARCODE_KEY)?.apply {
      initial =
        mutableListOf(Questionnaire.QuestionnaireItemInitialComponent().setValue(StringType(code)))
      readOnly = readonly
    }
  }

  override fun onClick(view: View) {
    if (view.id == R.id.btn_save_client_info) {
      if (readOnly) {
        finish()
      } else {
        showFormSubmissionConfirmAlert()
      }
    } else {
      showToast(getString(R.string.error_saving_form))
    }
  }

  fun showFormSubmissionConfirmAlert() {
    if (questionnaire.experimental)
      showConfirmAlert(
        context = this,
        message = R.string.questionnaire_alert_test_only_message,
        title = R.string.questionnaire_alert_test_only_title,
        confirmButtonListener = { handleQuestionnaireSubmit() },
        confirmButtonText = R.string.questionnaire_alert_test_only_button_title
      )
    else
      showConfirmAlert(
        context = this,
        message = R.string.questionnaire_alert_submit_message,
        title = R.string.questionnaire_alert_submit_title,
        confirmButtonListener = { handleQuestionnaireSubmit() },
        confirmButtonText = R.string.questionnaire_alert_submit_button_title
      )
  }

  fun getQuestionnaireResponse(): QuestionnaireResponse {
    val questionnaireFragment =
      supportFragmentManager.findFragmentByTag(QUESTIONNAIRE_FRAGMENT_TAG) as QuestionnaireFragment
    return questionnaireFragment.getQuestionnaireResponse()
  }

  private lateinit var saveProcessingAlertDialog: AlertDialog

  fun dismissSaveProcessing() {
    if (::saveProcessingAlertDialog.isInitialized && saveProcessingAlertDialog.isShowing)
      saveProcessingAlertDialog.dismiss()
  }

  open fun handleQuestionnaireSubmit() {
    saveProcessingAlertDialog = showProgressAlert(this, R.string.form_progress_message)

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
          postSaveSuccessful(questionnaireResponse)
        } else {
          Timber.e("An error occurred during extraction")
        }
      }
    )
  }

  open fun populateInitialValues(questionnaire: Questionnaire) = Unit

  open fun postSaveSuccessful(questionnaireResponse: QuestionnaireResponse) {
    val message = questionnaireViewModel.extractionProgressMessage.value
    if (message?.isNotBlank() == true)
      AlertDialogue.showInfoAlert(
        this,
        message,
        getString(R.string.done),
        {
          it.dismiss()
          finishActivity(questionnaireResponse)
        }
      )
    else finishActivity(questionnaireResponse)
  }

  fun finishActivity(questionnaireResponse: QuestionnaireResponse) {
    val parcelResponse = questionnaireResponse.copy()
    questionnaire.find(FieldType.TYPE, Questionnaire.QuestionnaireItemType.ATTACHMENT.name)
      .forEach { parcelResponse.find(it.linkId)?.answer?.clear() }
    setResult(
      Activity.RESULT_OK,
      Intent().apply {
        putExtra(QUESTIONNAIRE_RESPONSE, parser.encodeResourceToString(parcelResponse))
        putExtra(QUESTIONNAIRE_ARG_FORM, questionnaire.logicalId)
      }
    )
    finish()
  }

  fun deepFlat(
    qItems: List<Questionnaire.QuestionnaireItemComponent>,
    questionnaireResponse: QuestionnaireResponse,
    targetQ: MutableList<Questionnaire.QuestionnaireItemComponent>,
    targetQR: MutableList<QuestionnaireResponse.QuestionnaireResponseItemComponent>,
  ) {
    qItems.forEach { qit ->
      // process each inner item list
      deepFlat(qit.item, questionnaireResponse, targetQ, targetQR)

      // remove nested structure to prevent validation recursion; it is already processed above
      qit.item.clear()

      // add questionnaire and response pair for each linkid on same index
      questionnaireResponse.find(qit.linkId)?.let { qrit ->
        targetQ.add(qit)
        targetQR.add(qrit)
      }
    }
  }

  // TODO change this when SDK bug for validation is fixed
  // https://github.com/google/android-fhir/issues/912
  fun validQuestionnaireResponse(questionnaireResponse: QuestionnaireResponse): Boolean {
    // clone questionnaire and response for processing and changing structure
    val q = parser.parseResource(parser.encodeResourceToString(questionnaire)) as Questionnaire
    val qr =
      parser.parseResource(parser.encodeResourceToString(questionnaireResponse)) as
        QuestionnaireResponse

    // flatten and pair all responses temporarily to fix index mapping issue for questionnaire and
    // questionnaire response
    val qItems = mutableListOf<Questionnaire.QuestionnaireItemComponent>()
    val qrItems = mutableListOf<QuestionnaireResponse.QuestionnaireResponseItemComponent>()

    deepFlat(q.item, qr, qItems, qrItems)

    return QuestionnaireResponseValidator.validateQuestionnaireResponseAnswers(
        qItems,
        qrItems,
        this
      )
      .values
      .flatten()
      .all { it.isValid }
  }

  open fun handleQuestionnaireResponse(questionnaireResponse: QuestionnaireResponse) {
    questionnaireViewModel.extractAndSaveResources(
      context = this,
      questionnaire = questionnaire,
      questionnaireResponse = questionnaireResponse,
      resourceId = intent.getStringExtra(QUESTIONNAIRE_ARG_PATIENT_KEY),
      editMode = editMode
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
    const val QUESTIONNAIRE_EDIT_MODE = "edit-mode"
    const val QUESTIONNAIRE_RESPONSE = "questionnaire-response"
    const val QUESTIONNAIRE_ARG_BARCODE_KEY = "patient-barcode"
    const val WHO_IDENTIFIER_SYSTEM = "WHO-HCID"
    const val QUESTIONNAIRE_AGE = "PR-age"

    fun Intent.questionnaireEditMode() = this.getBooleanExtra(QUESTIONNAIRE_EDIT_MODE, false)
    fun Intent.questionnaireResponse() = this.getStringExtra(QUESTIONNAIRE_RESPONSE)
    fun Intent.populationResources() =
      this.getStringArrayListExtra(QuestionnaireActivity.QUESTIONNAIRE_POPULATION_RESOURCES)

    fun intentArgs(
      clientIdentifier: String? = null,
      formName: String,
      readOnly: Boolean = false,
      editMode: Boolean = false,
      questionnaireResponse: QuestionnaireResponse? = null,
      immunizationId: String? = null,
      populationResources: ArrayList<Resource> = ArrayList()
    ) =
      bundleOf(
        Pair(QUESTIONNAIRE_ARG_PATIENT_KEY, clientIdentifier),
        Pair(QUESTIONNAIRE_ARG_FORM, formName),
        Pair(QUESTIONNAIRE_READ_ONLY, readOnly),
        Pair(ADVERSE_EVENT_IMMUNIZATION_ITEM_KEY, immunizationId),
        Pair(QUESTIONNAIRE_EDIT_MODE, editMode)
      )
        .apply {
          val jsonParser = FhirContext.forR4Cached().newJsonParser()
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
