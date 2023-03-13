/*
 * Copyright 2021-2023 Ona Systems, Inc
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

package org.smartregister.fhircore.quest.ui.questionnaire

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.activity.viewModels
import androidx.annotation.VisibleForTesting
import androidx.core.os.bundleOf
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.whenStarted
import ca.uhn.fhir.parser.IParser
import com.google.android.fhir.datacapture.QuestionnaireFragment
import com.google.android.fhir.datacapture.validation.NotValidated
import com.google.android.fhir.datacapture.validation.QuestionnaireResponseValidator
import com.google.android.fhir.datacapture.validation.Valid
import com.google.android.fhir.logicalId
import dagger.hilt.android.AndroidEntryPoint
import java.util.Date
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.StringType
import org.smartregister.fhircore.engine.configuration.QuestionnaireConfig
import org.smartregister.fhircore.engine.domain.model.ActionParameter
import org.smartregister.fhircore.engine.domain.model.ActionParameterType
import org.smartregister.fhircore.engine.domain.model.QuestionnaireType
import org.smartregister.fhircore.engine.ui.base.AlertDialogue
import org.smartregister.fhircore.engine.ui.base.AlertDialogue.showCancelAlert
import org.smartregister.fhircore.engine.ui.base.AlertDialogue.showConfirmAlert
import org.smartregister.fhircore.engine.ui.base.AlertDialogue.showProgressAlert
import org.smartregister.fhircore.engine.ui.base.AlertIntent
import org.smartregister.fhircore.engine.ui.base.BaseMultiLanguageActivity
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.engine.util.extension.FieldType
import org.smartregister.fhircore.engine.util.extension.decodeResourceFromString
import org.smartregister.fhircore.engine.util.extension.encodeResourceToString
import org.smartregister.fhircore.engine.util.extension.find
import org.smartregister.fhircore.engine.util.extension.generateMissingItems
import org.smartregister.fhircore.engine.util.extension.showToast
import org.smartregister.fhircore.quest.R
import timber.log.Timber

/**
 * Launches Questionnaire/ Implement a subclass of this [QuestionnaireActivity] to provide
 * functionality on how to [handleQuestionnaireResponse]
 */
@AndroidEntryPoint
open class QuestionnaireActivity : BaseMultiLanguageActivity(), View.OnClickListener {

  @Inject lateinit var dispatcherProvider: DefaultDispatcherProvider

  @Inject lateinit var parser: IParser
  open val questionnaireViewModel: QuestionnaireViewModel by viewModels()
  private lateinit var questionnaire: Questionnaire
  private lateinit var fragment: QuestionnaireFragment
  private lateinit var saveProcessingAlertDialog: AlertDialog
  private lateinit var questionnaireConfig: QuestionnaireConfig
  private lateinit var actionParams: List<ActionParameter>
  private lateinit var prePopulationParams: List<ActionParameter>

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    outState.clear()
  }

  @Suppress("UNCHECKED_CAST")
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_questionnaire)

    questionnaireConfig = (intent.getSerializableExtra(QUESTIONNAIRE_CONFIG) as QuestionnaireConfig)

    actionParams =
      intent.getSerializableExtra(QUESTIONNAIRE_ACTION_PARAMETERS) as List<ActionParameter>?
        ?: emptyList()

    prePopulationParams =
      actionParams.filter {
        it.paramType == ActionParameterType.PREPOPULATE &&
          !it.value.isNullOrEmpty() &&
          !it.value.contains(STRING_INTERPOLATION_PREFIX)
      }

    val questionnaireActivity = this@QuestionnaireActivity
    questionnaireViewModel.removeOperation.observe(questionnaireActivity) { if (it) finish() }

    val loadProgress = showProgressAlert(questionnaireActivity, R.string.loading)

    lifecycleScope.launch {
      questionnaireViewModel.loadQuestionnaire(
          questionnaireConfig.id,
          questionnaireConfig.type,
          prePopulationParams
        )
        .let { thisQuestionnaire ->
          if (thisQuestionnaire == null) {
            questionnaireActivity.showToast(
              questionnaireActivity.getString(R.string.questionnaire_missing)
            )
            finish()
          } else {
            questionnaire = thisQuestionnaire
            // Only add the fragment once, when the activity is first created.
            if (savedInstanceState == null) renderFragment()

            withContext(dispatcherProvider.main()) {
              updateViews()
              fragment.whenStarted { loadProgress.dismiss() }
            }
          }
        }
    }
  }

  fun updateViews() {
    findViewById<Button>(R.id.btn_edit_qr).apply {
      visibility = if (questionnaireConfig.type.isReadOnly()) View.VISIBLE else View.GONE
      setOnClickListener(this@QuestionnaireActivity)
    }

    findViewById<Button>(R.id.submit_questionnaire)?.apply {
      layoutParams.width =
        ViewGroup.LayoutParams
          .MATCH_PARENT // Override by Styles xml does not seem to work for this layout param

      if (questionnaireConfig.type.isReadOnly() || questionnaire.experimental) {
        text = context.getString(R.string.done)
      } else if (questionnaireConfig.type.isEditMode()) {
        // setting the save button text from Questionnaire Config
        text = questionnaireConfig.saveButtonText ?: getString(R.string.str_save)
      }
    }

    supportActionBar?.apply {
      setDisplayHomeAsUpEnabled(true)
      title =
        if (questionnaireConfig.type.isEditMode())
          "${getString(R.string.edit)} ${questionnaireConfig.title}"
        else questionnaireConfig.title
    }
  }

  private suspend fun renderFragment() {
    // Pass questionnaire and questionnaire-response to fragment
    val questionnaireString = parser.encodeResourceToString(questionnaire)
    val fragmentBuilder = QuestionnaireFragment.builder().setQuestionnaire(questionnaireString)
    decodeQuestionnaireResponse(intent, questionnaireConfig)?.let {
      fragmentBuilder.setQuestionnaireResponse(it)
    }
    fragment = fragmentBuilder.build()
    supportFragmentManager.commit { add(R.id.container, fragment, QUESTIONNAIRE_FRAGMENT_TAG) }
    supportFragmentManager.setFragmentResultListener(
      QuestionnaireFragment.SUBMIT_REQUEST_KEY,
      this
    ) { _, _ ->
      if (this.getQuestionnaireConfig().type.isReadOnly() ||
          this.getQuestionnaireObject().experimental
      ) { // Experimental questionnaires should not be submitted
        this.finish()
      } else {
        this.handleQuestionnaireSubmit()
      }
    }
  }

  @VisibleForTesting
  internal suspend fun decodeQuestionnaireResponse(
    intent: Intent,
    questionnaireConfig: QuestionnaireConfig
  ): String? {
    var questionnaireResponse =
      intent
        .getStringExtra(QUESTIONNAIRE_RESPONSE)
        ?.decodeResourceFromString<QuestionnaireResponse>()
        ?.apply { generateMissingItems(this@QuestionnaireActivity.questionnaire) }

    if (questionnaireConfig.type.isReadOnly()) require(questionnaireResponse != null)

    if (questionnaireConfig.resourceIdentifier != null) {
      setBarcode(questionnaire, questionnaireConfig.resourceIdentifier!!)
    }

    if (questionnaireResponse == null && intentHasPopulationResources(intent)) {
      questionnaireResponse =
        questionnaireViewModel.generateQuestionnaireResponse(
          questionnaire = questionnaire,
          intent = intent,
          questionnaireConfig = questionnaireConfig
        )
    }

    return questionnaireResponse?.encodeResourceToString()
  }

  @VisibleForTesting
  internal fun intentHasPopulationResources(intent: Intent): Boolean {
    val resourceList = intent.getStringArrayListExtra(QUESTIONNAIRE_POPULATION_RESOURCES)
    return resourceList != null && resourceList.size > 0
  }

  private fun setBarcode(questionnaire: Questionnaire, code: String) {
    questionnaire.find(QUESTIONNAIRE_ARG_BARCODE)?.apply {
      initial =
        mutableListOf(Questionnaire.QuestionnaireItemInitialComponent().setValue(StringType(code)))
      readOnly = true
    }
  }

  override fun onClick(view: View) {
    if (view.id == R.id.btn_edit_qr) {
      questionnaireConfig = questionnaireConfig.copy(type = QuestionnaireType.EDIT)
      val loadProgress = showProgressAlert(this, R.string.loading)
      lifecycleScope.launch(dispatcherProvider.io()) {
        // Reload the questionnaire and reopen the fragment
        questionnaire =
          questionnaireViewModel.loadQuestionnaire(
            questionnaireConfig.id,
            questionnaireConfig.type,
            prePopulationParams
          )!!
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

  fun getQuestionnaireResponse(): QuestionnaireResponse {
    val questionnaireFragment =
      supportFragmentManager.findFragmentByTag(QUESTIONNAIRE_FRAGMENT_TAG) as QuestionnaireFragment
    return questionnaireFragment.getQuestionnaireResponse().apply {
      // TODO this is required until require condition in [QuestionnaireResponseValidator] is fixed
      this@QuestionnaireActivity.questionnaire.let { it.url = "${it.resourceType}/${it.logicalId}" }

      if (this.logicalId.isEmpty()) {
        this.id = UUID.randomUUID().toString()
        this.authored = Date()
      }

      this@QuestionnaireActivity.questionnaire
        .useContext
        .asSequence()
        .filter { it.hasValueCodeableConcept() }
        .forEach { it.valueCodeableConcept.coding.forEach { coding -> this.meta.addTag(coding) } }

      this.questionnaire =
        this@QuestionnaireActivity.questionnaire.let { "${it.resourceType}/${it.logicalId}" }
      // important to set response subject so that structure map can handle subject for all entities
      questionnaireViewModel.handleQuestionnaireResponseSubject(
        questionnaireConfig.resourceIdentifier,
        this@QuestionnaireActivity.questionnaire,
        this
      )
    }
  }

  fun dismissSaveProcessing() {
    if (::saveProcessingAlertDialog.isInitialized && saveProcessingAlertDialog.isShowing)
      saveProcessingAlertDialog.dismiss()
  }

  open fun handleSaveDraftQuestionnaire() {
    saveProcessingAlertDialog = showProgressAlert(this, R.string.form_progress_message)
    val questionnaireResponse = getQuestionnaireResponse()
    if (questionnaireViewModel.partialQuestionnaireResponseHasValues(questionnaireResponse)) {
      handlePartialQuestionnaireResponse(questionnaireResponse)
    }
    finish()
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

    questionnaireViewModel.extractionProgress.observe(this) { result ->
      onPostSave(result, questionnaireResponse)
    }
  }

  fun onPostSave(result: Boolean, questionnaireResponse: QuestionnaireResponse) {
    dismissSaveProcessing()
    if (result) {
      postSaveSuccessful(questionnaireResponse)
    } else {
      Timber.e("An error occurred during extraction")
    }
  }

  open fun postSaveSuccessful(questionnaireResponse: QuestionnaireResponse) {
    val message = questionnaireViewModel.extractionProgressMessage.value
    if (message?.isNotEmpty() == true)
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
        putExtra(QUESTIONNAIRE_RESPONSE, parcelResponse)
        putExtra(QUESTIONNAIRE_CONFIG, questionnaireConfig)
      }
    )
    finish()
  }

  fun validQuestionnaireResponse(questionnaireResponse: QuestionnaireResponse) =
    QuestionnaireResponseValidator.validateQuestionnaireResponse(
        questionnaire = questionnaire,
        questionnaireResponse = questionnaireResponse,
        context = this
      )
      .values
      .flatten()
      .all { it is Valid || it is NotValidated }

  open fun handleQuestionnaireResponse(questionnaireResponse: QuestionnaireResponse) {
    if (questionnaireConfig.confirmationDialog != null) {
      dismissSaveProcessing()
      confirmationDialog(questionnaireConfig = questionnaireConfig)
    } else {
      questionnaireResponse.status = QuestionnaireResponse.QuestionnaireResponseStatus.COMPLETED
      questionnaireViewModel.extractAndSaveResources(
        context = this,
        questionnaire = questionnaire,
        questionnaireResponse = questionnaireResponse,
        questionnaireConfig = questionnaireConfig
      )
    }
  }

  open fun handlePartialQuestionnaireResponse(questionnaireResponse: QuestionnaireResponse) {
    dismissSaveProcessing()
    questionnaireViewModel.savePartialQuestionnaireResponse(questionnaire, questionnaireResponse)
  }

  private fun confirmationDialog(questionnaireConfig: QuestionnaireConfig) {
    AlertDialogue.showAlert(
      context = this,
      alertIntent = AlertIntent.CONFIRM,
      title = questionnaireConfig.confirmationDialog!!.title,
      message = questionnaireConfig.confirmationDialog!!.message,
      confirmButtonListener = { dialog ->
        if (questionnaireConfig.resourceIdentifier != null &&
            questionnaireConfig.resourceType != null
        ) {
          questionnaireViewModel.deleteResource(
            questionnaireConfig.resourceType!!,
            questionnaireConfig.resourceIdentifier!!
          )
        } else if (questionnaireConfig.groupResource != null) {
          questionnaireViewModel.removeGroup(
            groupId = questionnaireConfig.groupResource!!.groupIdentifier,
            removeGroup = questionnaireConfig.groupResource?.removeGroup ?: false,
            deactivateMembers = questionnaireConfig.groupResource!!.deactivateMembers
          )
          questionnaireViewModel.removeGroupMember(
            memberId = questionnaireConfig.resourceIdentifier,
            removeMember = questionnaireConfig.groupResource?.removeMember ?: false,
            groupIdentifier = questionnaireConfig.groupResource!!.groupIdentifier,
            memberResourceType = questionnaireConfig.groupResource!!.memberResourceType
          )
        }
        dialog.dismiss()
      },
      neutralButtonListener = { dialog -> dialog.dismiss() }
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
    if (questionnaireConfig.type.isReadOnly()) {
      finish()
    } else if (questionnaireConfig.saveDraft) {
      showCancelQuestionnaireAlertDialog()
    } else {
      showConfirmAlertDialog()
    }
  }

  private fun showConfirmAlertDialog() {
    showConfirmAlert(
      this,
      getDismissDialogMessage(),
      R.string.questionnaire_alert_back_pressed_title,
      { finish() },
      R.string.questionnaire_alert_back_pressed_button_title,
    )
  }

  private fun showCancelQuestionnaireAlertDialog() {
    showCancelAlert(
      this,
      R.string.questionnaire_in_progress_alert_back_pressed_message,
      R.string.questionnaire_alert_back_pressed_title,
      { handleSaveDraftQuestionnaire() },
      R.string.questionnaire_alert_back_pressed_save_draft_button_title,
      { finish() },
      R.string.questionnaire_alert_back_pressed_button_title
    )
  }

  open fun getDismissDialogMessage() = R.string.questionnaire_alert_back_pressed_message

  fun getQuestionnaireObject() = questionnaire
  fun getQuestionnaireConfig() = questionnaireConfig

  companion object {
    const val QUESTIONNAIRE_POPULATION_RESOURCES = "questionnaire-population-resources"
    const val QUESTIONNAIRE_FRAGMENT_TAG = "questionnaire-fragment-tag"
    const val QUESTIONNAIRE_RESPONSE = "questionnaire-response"
    const val QUESTIONNAIRE_ARG_BARCODE = "patient-barcode"
    const val WHO_IDENTIFIER_SYSTEM = "WHO-HCID"
    const val QUESTIONNAIRE_AGE = "PR-age"
    const val QUESTIONNAIRE_CONFIG = "questionnaire-config"
    const val QUESTIONNAIRE_ACTION_PARAMETERS = "action-parameters"
    const val STRING_INTERPOLATION_PREFIX = "@{"

    fun Intent.questionnaireResponse() = this.getStringExtra(QUESTIONNAIRE_RESPONSE)
    fun Intent.populationResources() =
      this.getStringArrayListExtra(QUESTIONNAIRE_POPULATION_RESOURCES)

    fun intentArgs(
      questionnaireResponse: QuestionnaireResponse? = null,
      populationResources: ArrayList<Resource> = ArrayList(),
      questionnaireConfig: QuestionnaireConfig? = null,
      actionParams: List<ActionParameter> = emptyList()
    ) =
      bundleOf(
        Pair(QUESTIONNAIRE_CONFIG, questionnaireConfig),
        Pair(QUESTIONNAIRE_ACTION_PARAMETERS, actionParams)
      )
        .apply {
          questionnaireResponse?.let {
            putString(QUESTIONNAIRE_RESPONSE, it.encodeResourceToString())
          }
          val resourcesList = populationResources.map { it.encodeResourceToString() }
          if (resourcesList.isNotEmpty()) {
            putStringArrayList(
              QUESTIONNAIRE_POPULATION_RESOURCES,
              resourcesList.toCollection(ArrayList())
            )
          }
        }
  }
}
