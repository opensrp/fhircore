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
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.whenResumed
import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.FhirVersionEnum
import com.google.android.fhir.datacapture.QuestionnaireFragment
import com.google.android.fhir.datacapture.extensions.isPaginated
import com.google.android.fhir.datacapture.validation.Invalid
import com.google.android.fhir.datacapture.validation.QuestionnaireResponseValidator
import com.google.android.fhir.logicalId
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.hl7.fhir.r4.model.Encounter
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.StringType
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.sync.SyncBroadcaster
import org.smartregister.fhircore.engine.trace.PerformanceReporter
import org.smartregister.fhircore.engine.ui.base.AlertDialogue
import org.smartregister.fhircore.engine.ui.base.AlertDialogue.showConfirmAlert
import org.smartregister.fhircore.engine.ui.base.AlertDialogue.showProgressAlert
import org.smartregister.fhircore.engine.ui.base.BaseMultiLanguageActivity
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.engine.util.extension.FieldType
import org.smartregister.fhircore.engine.util.extension.decodeResourceFromString
import org.smartregister.fhircore.engine.util.extension.distinctifyLinkId
import org.smartregister.fhircore.engine.util.extension.encodeResourceToString
import org.smartregister.fhircore.engine.util.extension.find
import org.smartregister.fhircore.engine.util.extension.generateMissingItems
import org.smartregister.fhircore.engine.util.extension.showToast
import timber.log.Timber

/**
 * Launches Questionnaire/ Implement a subclass of this [QuestionnaireActivity] to provide
 * functionality on how to [handleQuestionnaireResponse]
 */
@AndroidEntryPoint
open class QuestionnaireActivity : BaseMultiLanguageActivity(), View.OnClickListener {

  @Inject lateinit var configurationRegistry: ConfigurationRegistry

  @Inject lateinit var dispatcherProvider: DefaultDispatcherProvider

  @Inject lateinit var syncBroadcaster: SyncBroadcaster

  @Inject lateinit var tracer: PerformanceReporter

  open val questionnaireViewModel: QuestionnaireViewModel by viewModels()

  lateinit var questionnaireConfig: QuestionnaireConfig

  var questionnaireType = QuestionnaireType.DEFAULT

  protected lateinit var questionnaire: Questionnaire

  protected var clientIdentifier: String? = null

  lateinit var fragment: QuestionnaireFragment

  private val parser = FhirContext.forCached(FhirVersionEnum.R4).newJsonParser()

  private lateinit var saveProcessingAlertDialog: AlertDialog

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    outState.clear()
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_questionnaire)
    val formName = intent.getStringExtra(QUESTIONNAIRE_ARG_FORM)!!
    clientIdentifier = intent.getStringExtra(QUESTIONNAIRE_ARG_PATIENT_KEY)
    intent.getStringExtra(QUESTIONNAIRE_ARG_TYPE)?.let {
      questionnaireType = QuestionnaireType.valueOf(it)
    }

    val loadProgress = showProgressAlert(this, R.string.loading)

    // Initialises the lateinit variable questionnaireViewModel to prevent
    // some init operations running on a separate thread and causing a crash
    questionnaireViewModel.sharedPreferencesHelper

    lifecycleScope.launch {
      withContext(dispatcherProvider.io()) {
        tracer.traceSuspend("Questionnaire.loadQuestionnaireAndConfig") {
          loadQuestionnaireAndConfig(formName)
        }
      }

      withContext(dispatcherProvider.main()) {
        // Only add the fragment once, when the activity is first created.
        if (savedInstanceState == null || !this@QuestionnaireActivity::fragment.isInitialized) {
          renderFragment()
        }

        updateViews()
        fragment.whenResumed { loadProgress.dismiss() }
      }
    }

    this.onBackPressedDispatcher.addCallback(
      this,
      object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
          handleBackPress()
        }
      },
    )

    questionnaireViewModel.extractionProgress.observe(this) { result ->
      if (result is ExtractionProgress.Success) {
        onExtractionSuccess(result.questionnaireResponse, result.extras)
      } else {
        result as ExtractionProgress.Failed
        onExtractionFailed(result.questionnaireResponse, result.exception)
      }
    }
  }

  fun updateViews() {
    findViewById<Button>(R.id.btn_edit_qr).apply {
      visibility = if (questionnaireType.isReadOnly()) View.VISIBLE else View.GONE
      setOnClickListener(this@QuestionnaireActivity)
    }

    supportActionBar?.apply {
      setDisplayHomeAsUpEnabled(true)
      title = questionnaireConfig.title
    }
  }

  private suspend fun renderFragment() {
    tracer.startTrace(QUESTIONNAIRE_TRACE)
    val questionnaireString = parser.encodeResourceToString(questionnaire)
    var questionnaireResponse: QuestionnaireResponse?
    if (clientIdentifier != null) {
      setBarcode(questionnaire, clientIdentifier!!, true)
      questionnaireResponse =
        questionnaireViewModel.generateQuestionnaireResponse(questionnaire, intent)
    } else {
      questionnaireResponse =
        intent
          .getStringExtra(QUESTIONNAIRE_RESPONSE)
          ?.decodeResourceFromString<QuestionnaireResponse>()
          ?.apply { generateMissingItems(this@QuestionnaireActivity.questionnaire) }
      if (questionnaireType.isReadOnly()) {
        requireNotNull(questionnaireResponse)
      } else {
        questionnaireResponse =
          questionnaireViewModel.generateQuestionnaireResponse(questionnaire, intent)
      }
    }

    val questionnaireFragmentBuilder =
      QuestionnaireFragment.builder()
        .setQuestionnaire(questionnaireString)
        .showReviewPageBeforeSubmit(questionnaire.isPaginated)
        .setShowSubmitButton(true)
        .setCustomQuestionnaireItemViewHolderFactoryMatchersProvider(
          QuestionnaireItemViewHolderFactoryMatchersProviderFactoryImpl.DEFAULT_PROVIDER,
        )
        .setIsReadOnly(questionnaireType.isReadOnly())
    questionnaireResponse.let {
      it.distinctifyLinkId()
      //        Timber.e(it.encodeResourceToString())
      questionnaireFragmentBuilder.setQuestionnaireResponse(it.encodeResourceToString())
    }
    intent.getBundleExtra(QUESTIONNAIRE_LAUNCH_CONTEXTS)?.let { launchContextBundle ->
      val launchContextMap =
        buildMap<String, String> {
          launchContextBundle.keySet().forEach {
            this[it] =
              launchContextBundle.getString(it)
                ?: throw NotImplementedError("launchContext with null is not currently supported")
          }
        }
      questionnaireFragmentBuilder.setQuestionnaireLaunchContextMap(launchContextMap)
    }

    fragment = questionnaireFragmentBuilder.build()
    supportFragmentManager.registerFragmentLifecycleCallbacks(
      object : FragmentManager.FragmentLifecycleCallbacks() {
        override fun onFragmentCreated(
          fm: FragmentManager,
          f: Fragment,
          savedInstanceState: Bundle?,
        ) {
          super.onFragmentCreated(fm, f, savedInstanceState)
          tracer.stopTrace(QUESTIONNAIRE_TRACE)
        }

        override fun onFragmentViewCreated(
          fm: FragmentManager,
          f: Fragment,
          v: View,
          savedInstanceState: Bundle?,
        ) {
          super.onFragmentViewCreated(fm, f, v, savedInstanceState)
          if (f is QuestionnaireFragment) {
            v.findViewById<Button>(com.google.android.fhir.datacapture.R.id.submit_questionnaire)
              ?.apply {
                layoutParams.width =
                  ViewGroup.LayoutParams
                    .MATCH_PARENT // Override by Styles xml does not seem to work for this layout
                // param
                text = submitButtonText()
              }
          }
        }
      },
      false,
    )
    supportFragmentManager.commit { replace(R.id.container, fragment, QUESTIONNAIRE_FRAGMENT_TAG) }
    supportFragmentManager.setFragmentResultListener(
      QuestionnaireFragment.SUBMIT_REQUEST_KEY,
      this,
    ) { _, _ ->
      onSubmitRequestResult()
    }

    supportFragmentManager.addFragmentOnAttachListener { _, frag ->
      Timber.e(frag.tag?.uppercase())
    }
  }

  open fun submitButtonText(): String {
    return if (questionnaireType.isReadOnly() || questionnaire.experimental) {
      getString(R.string.done)
    } else if (questionnaireType.isEditMode()) {
      // setting the save button text from Questionnaire Config
      questionnaireConfig.saveButtonText
        ?: getString(R.string.questionnaire_alert_submit_button_title)
    } else {
      getString(R.string.questionnaire_alert_submit_button_title)
    }
  }

  open fun onSubmitRequestResult() {
    if (questionnaireType.isReadOnly()) {
      finish()
    } else {
      showFormSubmissionConfirmAlert()
    }
  }

  suspend fun loadQuestionnaireAndConfig(formName: String) =
    // form is either name of form in asset/form-config or questionnaire-id
    // load from assets and get questionnaire or if not found build it from questionnaire
    kotlin
      .runCatching {
        val resultPair =
          questionnaireViewModel.getQuestionnaireConfigPair(
            this@QuestionnaireActivity,
            formName,
            questionnaireType,
          )

        questionnaireConfig = resultPair.first
        questionnaire = resultPair.second

        populateInitialValues(questionnaire)
      }
      .onFailure {
        Timber.e(it)

        withContext(dispatcherProvider.main()) {
          if (it is QuestionnaireNotFoundException) {
            Toast.makeText(this@QuestionnaireActivity, it.message, Toast.LENGTH_LONG).show()
            finish()
          }
        }
      }

  private fun setBarcode(questionnaire: Questionnaire, code: String, readonly: Boolean) {
    questionnaire.find(QUESTIONNAIRE_ARG_BARCODE_KEY)?.apply {
      initial =
        mutableListOf(Questionnaire.QuestionnaireItemInitialComponent().setValue(StringType(code)))
      readOnly = readonly
    }
  }

  override fun onClick(view: View) {
    if (view.id == R.id.btn_edit_qr) {
      questionnaireType = QuestionnaireType.EDIT
      val loadProgress = showProgressAlert(this, R.string.loading)
      lifecycleScope.launch(dispatcherProvider.io()) {
        // Reload the questionnaire and reopen the fragment
        loadQuestionnaireAndConfig(questionnaireConfig.identifier)
        supportFragmentManager.commit { detach(fragment) }
        renderFragment()
        withContext(dispatcherProvider.main()) {
          updateViews()
          loadProgress.dismiss()
        }
      }
    } else {
      showToast(getString(R.string.error_saving_form))
    }
  }

  open fun showFormSubmissionConfirmAlert() {
    showConfirmAlert(
      context = this,
      message =
        if (questionnaire.experimental) {
          R.string.questionnaire_alert_test_only_message
        } else R.string.questionnaire_alert_submit_message,
      title =
        if (questionnaire.experimental) {
          R.string.questionnaire_alert_test_only_title
        } else R.string.questionnaire_alert_submit_title,
      confirmButtonListener = { handleQuestionnaireSubmit() },
      confirmButtonText =
        if (questionnaire.experimental) {
          R.string.questionnaire_alert_test_only_button_title
        } else R.string.questionnaire_alert_submit_button_title,
    )
  }

  suspend fun getQuestionnaireResponse(): QuestionnaireResponse {
    val questionnaireFragment =
      supportFragmentManager.findFragmentByTag(QUESTIONNAIRE_FRAGMENT_TAG) as QuestionnaireFragment
    return questionnaireFragment.getQuestionnaireResponse()
  }

  fun dismissSaveProcessing() {
    if (::saveProcessingAlertDialog.isInitialized && saveProcessingAlertDialog.isShowing) {
      saveProcessingAlertDialog.dismiss()
    }
  }

  open fun handleQuestionnaireSubmit() {
    saveProcessingAlertDialog =
      showProgressAlert(this@QuestionnaireActivity, R.string.form_progress_message)
    val doHandleQuestionnaireResponse = suspend {
      getQuestionnaireResponse()
        .takeIf { validQuestionnaireResponse(it) }
        ?.let { handleQuestionnaireResponse(it) }
        ?: saveProcessingAlertDialog.dismiss().also {
          AlertDialogue.showErrorAlert(
            this@QuestionnaireActivity,
            R.string.questionnaire_alert_invalid_message,
            R.string.questionnaire_alert_invalid_title,
          )
        }
    }
    lifecycleScope.launch { doHandleQuestionnaireResponse() }
  }

  private fun onExtractionSuccess(
    questionnaireResponse: QuestionnaireResponse,
    extras: List<Resource>? = null,
  ) {
    dismissSaveProcessing()
    syncBroadcaster.runSync()
    postSaveSuccessful(questionnaireResponse, extras)
  }

  private fun onExtractionFailed(questionnaireResponse: QuestionnaireResponse, err: Throwable) {
    dismissSaveProcessing()
    Timber.e("An error occurred during '${questionnaireResponse.questionnaire}' extraction: $err")
    showConfirmAlert(
      context = this,
      message = R.string.error_extraction,
      title =
        if (questionnaire.experimental) {
          R.string.questionnaire_alert_test_only_title
        } else R.string.questionnaire_alert_submit_title,
      confirmButtonListener = { handleQuestionnaireSubmit() },
      confirmButtonText = R.string.retry_extraction,
    )
  }

  open fun populateInitialValues(questionnaire: Questionnaire) = Unit

  open fun postSaveSuccessful(
    questionnaireResponse: QuestionnaireResponse,
    extras: List<Resource>? = null,
  ) {
    val message = questionnaireViewModel.extractionProgressMessage.value
    if (message?.isNotEmpty() == true) {
      AlertDialogue.showInfoAlert(
        this,
        message,
        getString(R.string.done),
        {
          it.dismiss()
          finishActivity(questionnaireResponse, extras)
        },
      )
    } else {
      finishActivity(questionnaireResponse, extras)
    }
  }

  fun finishActivity(questionnaireResponse: QuestionnaireResponse, extras: List<Resource>? = null) {
    val parcelResponse = questionnaireResponse.copy()
    questionnaire
      .find(FieldType.TYPE, Questionnaire.QuestionnaireItemType.ATTACHMENT.name)
      .forEach { parcelResponse.find(it.linkId)?.answer?.clear() }
    setResult(
      Activity.RESULT_OK,
      Intent().apply {
        putExtra(QUESTIONNAIRE_RESPONSE, parser.encodeResourceToString(parcelResponse))
        putExtra(QUESTIONNAIRE_ARG_FORM, questionnaire.logicalId)
        putExtra(
          QUESTIONNAIRE_BACK_REFERENCE_KEY,
          intent.getStringExtra(QUESTIONNAIRE_BACK_REFERENCE_KEY),
        )
        extras?.map { res ->
          if (res is Encounter) {
            putExtra(QUESTIONNAIRE_RES_ENCOUNTER, res.status.toCode())
          }
        }
      },
    )
    finish()
  }

  suspend fun validQuestionnaireResponse(questionnaireResponse: QuestionnaireResponse) =
    QuestionnaireResponseValidator.validateQuestionnaireResponse(
        questionnaire = questionnaire,
        questionnaireResponse = questionnaireResponse,
        context = this,
      )
      .values
      .flatten()
      .none { it is Invalid }

  open fun handleQuestionnaireResponse(questionnaireResponse: QuestionnaireResponse) {
    questionnaireViewModel.extractAndSaveResources(
      context = this,
      questionnaire = questionnaire,
      questionnaireResponse = questionnaireResponse,
      resourceId = intent.getStringExtra(QUESTIONNAIRE_ARG_PATIENT_KEY),
      groupResourceId = intent.getStringExtra(QUESTIONNAIRE_ARG_GROUP_KEY),
      questionnaireType = questionnaireType,
      backReference = intent.getStringExtra(QUESTIONNAIRE_BACK_REFERENCE_KEY),
    )
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    return when (item.itemId) {
      android.R.id.home -> {
        handleBackPress()
        true
      }
      else -> super.onOptionsItemSelected(item)
    }
  }

  private fun handleBackPress() {
    if (questionnaireType.isReadOnly()) {
      finish()
    } else {
      showConfirmAlert(
        this,
        getDismissDialogMessage(),
        R.string.questionnaire_alert_back_pressed_title,
        { finish() },
        R.string.questionnaire_alert_back_pressed_button_title,
      )
    }
  }

  open fun getDismissDialogMessage() = R.string.questionnaire_alert_back_pressed_message

  companion object {
    const val QUESTIONNAIRE_TITLE_KEY = "questionnaire-title-key"
    const val QUESTIONNAIRE_POPULATION_RESOURCES = "questionnaire-population-resources"
    const val QUESTIONNAIRE_FRAGMENT_TAG = "questionnaire-fragment-tag"
    const val QUESTIONNAIRE_ARG_PATIENT_KEY = "questionnaire_patient_item_id"
    const val QUESTIONNAIRE_ARG_GROUP_KEY = "questionnaire_group_item_id"
    const val FORM_CONFIGURATIONS = "configurations/form/form_configurations.json"
    const val QUESTIONNAIRE_ARG_FORM = "questionnaire-form-name"
    const val QUESTIONNAIRE_ARG_TYPE = "questionnaire-type"
    const val QUESTIONNAIRE_RES_ENCOUNTER = "questionnaire-encounter"
    const val QUESTIONNAIRE_RESPONSE = "questionnaire-response"
    const val QUESTIONNAIRE_BACK_REFERENCE_KEY = "questionnaire-back-reference"
    const val QUESTIONNAIRE_ARG_BARCODE_KEY = "patient-barcode"
    const val QUESTIONNAIRE_AGE = "PR-age"
    const val QUESTIONNAIRE_LAUNCH_CONTEXTS =
      "org.smartregister.fhircore.engine.ui.questionnaire.launchContext"
    const val QUESTIONNAIRE_TRACE = "Questionnaire.renderFragment"

    fun intentArgs(
      clientIdentifier: String? = null,
      groupIdentifier: String? = null,
      formName: String,
      questionnaireType: QuestionnaireType = QuestionnaireType.DEFAULT,
      questionnaireResponse: QuestionnaireResponse? = null,
      backReference: String? = null,
      launchContexts: Map<String, Resource> = emptyMap(),
      populationResources: ArrayList<out Resource> = ArrayList(),
    ) =
      bundleOf(
          Pair(QUESTIONNAIRE_ARG_PATIENT_KEY, clientIdentifier),
          Pair(QUESTIONNAIRE_ARG_GROUP_KEY, groupIdentifier),
          Pair(QUESTIONNAIRE_ARG_FORM, formName),
          Pair(QUESTIONNAIRE_ARG_TYPE, questionnaireType.name),
          Pair(QUESTIONNAIRE_BACK_REFERENCE_KEY, backReference),
        )
        .apply {
          questionnaireResponse?.let {
            putString(QUESTIONNAIRE_RESPONSE, it.encodeResourceToString())
          }
          val resourcesList = populationResources.map { it.encodeResourceToString() }
          if (resourcesList.isNotEmpty()) {
            putStringArrayList(
              QUESTIONNAIRE_POPULATION_RESOURCES,
              resourcesList.toCollection(ArrayList()),
            )
          }
          val actualContexts = launchContexts.toMutableMap()
          if (launchContexts.isEmpty()) {
            populationResources.forEach {
              actualContexts[it.resourceType.toString().lowercase()] = it
            }
          }
          actualContexts
            .takeIf { it.isNotEmpty() }
            ?.let { kv ->
              val launchContextsBundlePairs =
                kv.map { it.key to it.value.encodeResourceToString() }.toTypedArray()
              putBundle(QUESTIONNAIRE_LAUNCH_CONTEXTS, bundleOf(*launchContextsBundlePairs))
            }
        }

    fun launchQuestionnaire(
      context: Context,
      questionnaireId: String,
      clientIdentifier: String? = null,
      groupIdentifier: String? = null,
      questionnaireType: QuestionnaireType = QuestionnaireType.DEFAULT,
      intentBundle: Bundle = Bundle.EMPTY,
      launchContexts: Map<String, Resource> = emptyMap(),
      populationResources: ArrayList<Resource>? = null,
    ) {
      context.startActivity(
        createQuestionnaireIntent(
          context = context,
          questionnaireId = questionnaireId,
          clientIdentifier = clientIdentifier,
          groupIdentifier = groupIdentifier,
          questionnaireType = questionnaireType,
          intentBundle = intentBundle,
          launchContexts = launchContexts,
          populationResources = populationResources,
        ),
      )
    }

    fun launchQuestionnaireForResult(
      context: Activity,
      questionnaireId: String,
      clientIdentifier: String? = null,
      questionnaireType: QuestionnaireType = QuestionnaireType.DEFAULT,
      backReference: String? = null,
      intentBundle: Bundle = Bundle.EMPTY,
      launchContexts: Map<String, Resource> = emptyMap(),
      populationResources: ArrayList<Resource>? = null,
    ) {
      context.startActivityForResult(
        createQuestionnaireResultIntent(
          context = context,
          questionnaireId = questionnaireId,
          clientIdentifier = clientIdentifier,
          questionnaireType = questionnaireType,
          backReference = backReference,
          intentBundle = intentBundle,
          launchContexts = launchContexts,
          populationResources = populationResources,
        ),
        0,
      )
    }

    fun createQuestionnaireResultIntent(
      context: Activity,
      questionnaireId: String,
      clientIdentifier: String? = null,
      questionnaireType: QuestionnaireType = QuestionnaireType.DEFAULT,
      backReference: String? = null,
      intentBundle: Bundle = Bundle.EMPTY,
      launchContexts: Map<String, Resource> = emptyMap(),
      populationResources: ArrayList<Resource>? = null,
    ): Intent {
      return Intent(context, QuestionnaireActivity::class.java)
        .putExtras(intentBundle)
        .putExtras(
          intentArgs(
            clientIdentifier = clientIdentifier,
            formName = questionnaireId,
            questionnaireType = questionnaireType,
            backReference = backReference,
            launchContexts = launchContexts,
            populationResources = populationResources ?: ArrayList(),
          ),
        )
    }

    fun createQuestionnaireIntent(
      context: Context,
      questionnaireId: String,
      clientIdentifier: String? = null,
      groupIdentifier: String? = null,
      questionnaireType: QuestionnaireType = QuestionnaireType.DEFAULT,
      intentBundle: Bundle = Bundle.EMPTY,
      launchContexts: Map<String, Resource> = emptyMap(),
      populationResources: ArrayList<Resource>? = null,
    ): Intent {
      return Intent(context, QuestionnaireActivity::class.java)
        .putExtras(intentBundle)
        .putExtras(
          intentArgs(
            clientIdentifier = clientIdentifier,
            groupIdentifier = groupIdentifier,
            formName = questionnaireId,
            questionnaireType = questionnaireType,
            launchContexts = launchContexts,
            populationResources = populationResources ?: ArrayList(),
          ),
        )
    }
  }
}
