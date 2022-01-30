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
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.os.bundleOf
import androidx.fragment.app.commit
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import ca.uhn.fhir.context.FhirContext
import com.famoco.desfireservicelib.CardReaderState
import com.famoco.desfireservicelib.DESFireServiceAccess
import com.famoco.desfireservicelib.ServiceConnectionState
import com.google.android.fhir.FhirEngineProvider
import com.google.android.fhir.datacapture.QuestionnaireFragment
import com.google.android.fhir.datacapture.QuestionnaireFragment.Companion.EXTRA_QUESTIONNAIRE_JSON_STRING
import com.google.android.fhir.datacapture.QuestionnaireFragment.Companion.EXTRA_QUESTIONNAIRE_RESPONSE_JSON_STRING
import com.google.android.fhir.datacapture.validation.QuestionnaireResponseValidator
import com.google.android.fhir.db.ResourceNotFoundException
import com.google.android.fhir.logicalId
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.StringType
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.nfc.MainViewModel
import org.smartregister.fhircore.engine.ui.base.AlertDialogue
import org.smartregister.fhircore.engine.ui.base.AlertDialogue.showConfirmAlert
import org.smartregister.fhircore.engine.ui.base.AlertDialogue.showProgressAlert
import org.smartregister.fhircore.engine.ui.base.BaseMultiLanguageActivity
import org.smartregister.fhircore.engine.util.DispatcherProvider
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

  open val questionnaireViewModel: QuestionnaireViewModel by viewModels()

  lateinit var questionnaireConfig: QuestionnaireConfig

  protected lateinit var questionnaire: Questionnaire
  protected var clientIdentifier: String? = null
  protected var immunizationId: String? = null
  var readOnly: Boolean = false
  var editMode = false
  lateinit var fragment: FhirCoreQuestionnaireFragment
  private val parser = FhirContext.forR4Cached().newJsonParser()
  private var formName: String = ""

  private val mainViewModel: MainViewModel by viewModels()
  private lateinit var eventJob: Job
  private var desFireServiceObserversAdded = false
  private val desFireServiceConnectionStateObserver: Observer<in ServiceConnectionState> =
      Observer {
    val state = it.name
  }
  private val cardReaderStateObserver: Observer<in CardReaderState> = Observer {
    val cardReaderState = it.name
  }
  private var readResultObserver: Observer<in Array<String>>? = null

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

      formName = intent.getStringExtra(QUESTIONNAIRE_ARG_FORM)!!
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
        if (readOnly) {
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

    // Add DES service listeners
    addDesServiceListeners()
    // to be safe init SAM when activity is launched
    mainViewModel.generateProtoFile()
    // InitializeSAM
    mainViewModel.initSAM()
  }

  private suspend fun renderFragment() {
    fragment =
      FhirCoreQuestionnaireFragment().apply {
        val questionnaireString = parser.encodeResourceToString(questionnaire)

        // Generate Fragment bundle arguments.
        // is the Questionnaire & QuestionnaireResponse
        arguments =
          when {
            clientIdentifier == null -> {
              bundleOf(Pair(EXTRA_QUESTIONNAIRE_JSON_STRING, questionnaireString)).apply {
                val questionnaireResponse = intent.getStringExtra(QUESTIONNAIRE_RESPONSE)
                if (readOnly && questionnaireResponse != null) {
                  putString(EXTRA_QUESTIONNAIRE_JSON_STRING, questionnaireResponse)
                }
              }
            }
            clientIdentifier != null -> {
              try {
                FhirEngineProvider.getInstance(this@QuestionnaireActivity)
                  .load(Patient::class.java, clientIdentifier!!)
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
    if (::saveProcessingAlertDialog.isInitialized && saveProcessingAlertDialog.isShowing)
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
        if (result.first) {
          postSaveSuccessful(result.second)
        } else {
          Timber.e("An error occurred during extraction")
        }
      }
    )
  }

  open fun postSaveSuccessful(questionnaireResponse: QuestionnaireResponse) {
    saveToNfc(questionnaireResponse)
    // finish()
  }

  fun validQuestionnaireResponse(questionnaireResponse: QuestionnaireResponse): Boolean {
    return QuestionnaireResponseValidator.validateQuestionnaireResponseAnswers(
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
    const val ASSISTANCE_VISIT_FORM = "assistance-visit"
    const val ANTHRO_FOLLOWING_VISIT_FORM = "anthro-following-visit"
    const val CODA_CHILD_REG_FORM = "wfp-coda-poc-child-registration"

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

  override fun onResume() {
    super.onResume()
    // Event that prompt only once to be able to know what has just happen with the card reader
    // This consumption will be used if the end-user want to use the Read/Write Activities
    // from the DESFire Service, so that the event can be consume inside the end-user app.
    eventJob =
      lifecycleScope.launchWhenStarted {
        DESFireServiceAccess.eventFlow.collect { event ->
          Toast.makeText(baseContext, event.name, Toast.LENGTH_SHORT).show()
        }
      }
    eventJob.start()
  }

  private fun addDesServiceListeners() {
    // Check state connection with the service
    desFireServiceObserversAdded = true
    DESFireServiceAccess.DESFireServiceConnectionState.observe(
      this,
      desFireServiceConnectionStateObserver
    )

    // Check if card interaction status
    DESFireServiceAccess.cardReaderState.observe(this, cardReaderStateObserver)

    if (readResultObserver == null) {
      readResultObserver =
        Observer { result ->
          if (!result.isNullOrEmpty()) {
            val stringBuilder = StringBuilder().append("")
            result.forEach { stringBuilder.append(it) }
            val readResult = stringBuilder.toString()
            this@QuestionnaireActivity.finish() // quit questinnaire activity
          }
        }
    }

    // After reading the card, the end-user will need the content that has been read on the card
    DESFireServiceAccess.readResult.observe(this, readResultObserver!!)
  }
  private fun saveToNfc(questionnaireResponse: QuestionnaireResponse) {
    var json: String = "{}"
    when (formName) {
      ASSISTANCE_VISIT_FORM ->
        json = questionnaireViewModel.getAssistanceVisitNfcJson(questionnaireResponse)
      CODA_CHILD_REG_FORM ->
        json = questionnaireViewModel.getChildRegNfcJson(questionnaireResponse, this)
    }
    writeToCard(json)
  }

  private fun writeToCard(json: String) {
    mainViewModel.generateProtoFile()
    // InitializeSAM
    mainViewModel.initSAM()

    // Perform Write action with the UI given by the Service
    mainViewModel.writeSerialized(json)
  }
}
