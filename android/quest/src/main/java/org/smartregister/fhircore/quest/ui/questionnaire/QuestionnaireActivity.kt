/*
 * Copyright 2021-2024 Ona Systems, Inc
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

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.location.Location
import android.os.Bundle
import android.os.Parcelable
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.os.bundleOf
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import com.google.android.fhir.datacapture.QuestionnaireFragment
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import dagger.hilt.android.AndroidEntryPoint
import java.io.Serializable
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.Resource
import org.smartregister.fhircore.engine.configuration.QuestionnaireConfig
import org.smartregister.fhircore.engine.configuration.app.LocationLogOptions
import org.smartregister.fhircore.engine.domain.model.ActionParameter
import org.smartregister.fhircore.engine.domain.model.isReadOnly
import org.smartregister.fhircore.engine.domain.model.isSummary
import org.smartregister.fhircore.engine.ui.base.AlertDialogButton
import org.smartregister.fhircore.engine.ui.base.AlertDialogue
import org.smartregister.fhircore.engine.ui.base.AlertIntent
import org.smartregister.fhircore.engine.ui.base.BaseMultiLanguageActivity
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.extension.encodeResourceToString
import org.smartregister.fhircore.engine.util.extension.parcelable
import org.smartregister.fhircore.engine.util.extension.parcelableArrayList
import org.smartregister.fhircore.engine.util.extension.showToast
import org.smartregister.fhircore.engine.util.location.LocationUtils
import org.smartregister.fhircore.engine.util.location.PermissionUtils
import org.smartregister.fhircore.quest.R
import org.smartregister.fhircore.quest.databinding.QuestionnaireActivityBinding
import org.smartregister.fhircore.quest.ui.shared.ActivityOnResultType
import org.smartregister.fhircore.quest.ui.shared.ON_RESULT_TYPE
import org.smartregister.fhircore.quest.util.ResourceUtils
import timber.log.Timber

@AndroidEntryPoint
class QuestionnaireActivity : BaseMultiLanguageActivity() {

  @Inject lateinit var dispatcherProvider: DispatcherProvider
  val viewModel by viewModels<QuestionnaireViewModel>()
  private lateinit var questionnaireConfig: QuestionnaireConfig
  private lateinit var actionParameters: ArrayList<ActionParameter>
  private lateinit var viewBinding: QuestionnaireActivityBinding
  private var questionnaire: Questionnaire? = null
  private var alertDialog: AlertDialog? = null
  private lateinit var fusedLocationClient: FusedLocationProviderClient
  private var currentLocation: Location? = null
  private val locationPermissionLauncher: ActivityResultLauncher<Array<String>> =
    registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
      permissions: Map<String, Boolean> ->
      PermissionUtils.getLocationPermissionLauncher(
        permissions = permissions,
        onFineLocationPermissionGranted = { fetchLocation() },
        onCoarseLocationPermissionGranted = { fetchLocation() },
        onLocationPermissionDenied = {
          showToast(
            getString(R.string.location_permissions_denied),
            Toast.LENGTH_SHORT,
          )
        },
      )
    }

  private val activityResultLauncher: ActivityResultLauncher<Intent> =
    registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
      activityResult: ActivityResult ->
      if (activityResult.resultCode == Activity.RESULT_OK) {
        fetchLocation()
      }
    }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    setTheme(org.smartregister.fhircore.engine.R.style.AppTheme_Questionnaire)
    viewBinding = QuestionnaireActivityBinding.inflate(layoutInflater)
    setContentView(viewBinding.root)
    with(intent) {
      parcelable<QuestionnaireConfig>(QUESTIONNAIRE_CONFIG)?.also { questionnaireConfig = it }
      actionParameters = parcelableArrayList(QUESTIONNAIRE_ACTION_PARAMETERS) ?: arrayListOf()
    }

    if (!::questionnaireConfig.isInitialized) {
      showToast(getString(R.string.missing_questionnaire_config))
      finish()
      return
    }

    viewModel.questionnaireProgressStateLiveData.observe(this) { progressState ->
      alertDialog =
        if (progressState?.active == false) {
          alertDialog?.dismiss()
          null
        } else {
          when (progressState) {
            is QuestionnaireProgressState.ExtractionInProgress ->
              AlertDialogue.showProgressAlert(this, R.string.extraction_in_progress)
            is QuestionnaireProgressState.QuestionnaireLaunch ->
              AlertDialogue.showProgressAlert(this, R.string.loading_questionnaire)
            else -> null
          }
        }
    }

    if (savedInstanceState == null) renderQuestionnaire()

    setupLocationServices()

    this.onBackPressedDispatcher.addCallback(
      this,
      object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
          handleBackPress()
        }
      },
    )
  }

  private fun setupLocationServices() {
    if (
      viewModel.applicationConfiguration.logGpsLocation.contains(LocationLogOptions.QUESTIONNAIRE)
    ) {
      fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

      if (!LocationUtils.isLocationEnabled(this)) {
        showLocationSettingsDialog(
          Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).apply {
            putExtra(ON_RESULT_TYPE, ActivityOnResultType.LOCATION.name)
          },
        )
      }

      if (!PermissionUtils.hasLocationPermissions(this)) {
        locationPermissionLauncher.launch(
          arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
          ),
        )
      }

      if (
        currentLocation == null &&
          LocationUtils.isLocationEnabled(this) &&
          PermissionUtils.hasLocationPermissions(this)
      ) {
        fetchLocation()
      }
    }
  }

  private fun showLocationSettingsDialog(intent: Intent) {
    viewModel.setProgressState(QuestionnaireProgressState.QuestionnaireLaunch(false))
    AlertDialog.Builder(this)
      .setMessage(getString(R.string.location_services_disabled))
      .setCancelable(true)
      .setPositiveButton(getString(R.string.yes)) { _, _ -> activityResultLauncher.launch(intent) }
      .setNegativeButton(getString(R.string.no)) { dialog, _ -> dialog.cancel() }
      .show()
  }

  fun fetchLocation(highAccuracy: Boolean = true) {
    lifecycleScope.launch {
      try {
        currentLocation =
          async(dispatcherProvider.io()) {
              if (highAccuracy) {
                LocationUtils.getAccurateLocation(fusedLocationClient)
              } else {
                LocationUtils.getApproximateLocation(fusedLocationClient)
              }
            }
            .await()
      } catch (e: Exception) {
        Timber.e(e, "Failed to get GPS location for questionnaire: ${questionnaireConfig.id}")
        showToast("Failed to get GPS location", Toast.LENGTH_LONG)
      }
    }
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    outState.clear()
  }

  private fun renderQuestionnaire() {
    if (supportFragmentManager.findFragmentByTag(QUESTIONNAIRE_FRAGMENT_TAG) != null) return

    lifecycleScope.launch {
      viewModel.setProgressState(QuestionnaireProgressState.QuestionnaireLaunch(true))

      viewBinding.questionnaireToolbar.setNavigationIcon(R.drawable.ic_cancel)
      viewBinding.questionnaireToolbar.setNavigationOnClickListener { handleBackPress() }
      viewBinding.questionnaireTitle.text = questionnaireConfig.title
      viewBinding.clearAll.visibility =
        if (questionnaireConfig.showClearAll) View.VISIBLE else View.GONE

      questionnaire = viewModel.retrieveQuestionnaire(questionnaireConfig)

      if (questionnaire == null) {
        showToast(getString(R.string.questionnaire_not_found))
        finish()
        return@launch
      }
      if (questionnaire!!.subjectType.isNullOrEmpty()) {
        val subjectRequiredMessage = getString(R.string.missing_subject_type)
        showToast(subjectRequiredMessage)
        Timber.e(subjectRequiredMessage)
        finish()
        return@launch
      }

      val questionnaireFragment =
        getQuestionnaireFragmentBuilder(
            questionnaire = questionnaire!!,
            questionnaireConfig = questionnaireConfig,
          )
          .build()
      viewBinding.clearAll.setOnClickListener { questionnaireFragment.clearAllAnswers() }
      supportFragmentManager.commit {
        setReorderingAllowed(true)
        add(R.id.container, questionnaireFragment, QUESTIONNAIRE_FRAGMENT_TAG)
      }
      registerFragmentResultListener()

      viewModel.setProgressState(QuestionnaireProgressState.QuestionnaireLaunch(false))
    }
  }

  private suspend fun getQuestionnaireFragmentBuilder(
    questionnaire: Questionnaire,
    questionnaireConfig: QuestionnaireConfig,
  ): QuestionnaireFragment.Builder {
    val (questionnaireResponse, launchContextResources) =
      viewModel.populateQuestionnaire(questionnaire, this.questionnaireConfig, actionParameters)

    return QuestionnaireFragment.builder()
      .setQuestionnaire(questionnaire.json())
      .setCustomQuestionnaireItemViewHolderFactoryMatchersProvider(
        OPENSRP_ITEM_VIEWHOLDER_FACTORY_MATCHERS_PROVIDER,
      )
      .setSubmitButtonText(
        questionnaireConfig.saveButtonText ?: getString(R.string.submit_questionnaire),
      )
      .showAsterisk(this.questionnaireConfig.showRequiredTextAsterisk)
      .showRequiredText(this.questionnaireConfig.showRequiredText)
      .setIsReadOnly(questionnaireConfig.isSummary())
      .setShowSubmitAnywayButton(questionnaireConfig.showSubmitAnywayButton.toBooleanStrict())
      .apply {
        if (questionnaireResponse != null) {
          questionnaireResponse
            .takeIf {
              viewModel.validateQuestionnaireResponse(questionnaire, it, this@QuestionnaireActivity)
            }
            ?.let { setQuestionnaireResponse(it.json()) }
            ?: showToast(getString(R.string.error_populating_questionnaire))
        }

        launchContextResources
          .associate { Pair(it.resourceType.name.lowercase(), it.encodeResourceToString()) }
          .takeIf { it.isNotEmpty() }
          ?.let { setQuestionnaireLaunchContextMap(it) }
      }
  }

  private fun Resource.json(): String = this.encodeResourceToString()

  private fun registerFragmentResultListener() {
    supportFragmentManager.setFragmentResultListener(
      QuestionnaireFragment.SUBMIT_REQUEST_KEY,
      this,
    ) { _, _ ->
      if (questionnaireConfig.showSubmissionConfirmationDialog.toBooleanStrict()) {
        AlertDialogue.showAlert(
          context = this,
          alertIntent = AlertIntent.CONFIRM,
          message = getString(R.string.questionnaire_submission_confirmation_message),
          title = getString(R.string.questionnaire_submission_confirmation_title),
          confirmButton =
            AlertDialogButton(
              listener = { processSubmission() },
            ),
          neutralButton =
            AlertDialogButton(
              text = R.string.no,
              listener = { it.dismiss() },
            ),
        )
      } else {
        processSubmission()
      }
    }
  }

  private fun processSubmission() {
    lifecycleScope.launch {
      val questionnaireResponse = retrieveQuestionnaireResponse()

      // Close questionnaire if opened in read only mode or if experimental
      if (questionnaireConfig.isReadOnly() || questionnaire?.experimental == true) {
        finish()
      }
      if (questionnaireResponse != null && questionnaire != null) {
        viewModel.run {
          setProgressState(QuestionnaireProgressState.ExtractionInProgress(true))

          if (currentLocation != null) {
            questionnaireResponse.contained.add(
              ResourceUtils.createFhirLocationFromGpsLocation(gpsLocation = currentLocation!!),
            )
          }

          handleQuestionnaireSubmission(
            questionnaire = questionnaire!!,
            currentQuestionnaireResponse = questionnaireResponse,
            questionnaireConfig = questionnaireConfig,
            actionParameters = actionParameters,
            context = this@QuestionnaireActivity,
          ) { idTypes, questionnaireResponse ->
            // Dismiss progress indicator dialog, submit result then finish activity
            // TODO Ensure this dialog is dismissed even when an exception is encountered
            setProgressState(QuestionnaireProgressState.ExtractionInProgress(false))
            setResult(
              Activity.RESULT_OK,
              Intent().apply {
                putExtra(QUESTIONNAIRE_RESPONSE, questionnaireResponse as Serializable)
                putExtra(QUESTIONNAIRE_SUBMISSION_EXTRACTED_RESOURCE_IDS, idTypes as Serializable)
                putExtra(QUESTIONNAIRE_CONFIG, questionnaireConfig as Parcelable)
                putExtra(ON_RESULT_TYPE, ActivityOnResultType.QUESTIONNAIRE.name)
              },
            )
            finish()
          }
        }
      }
    }
  }

  private fun handleBackPress() {
    if (questionnaireConfig.isReadOnly()) {
      finish()
    } else if (questionnaireConfig.saveDraft) {
      AlertDialogue.showThreeButtonAlert(
        context = this,
        message =
          org.smartregister.fhircore.engine.R.string
            .questionnaire_in_progress_alert_back_pressed_message,
        title = org.smartregister.fhircore.engine.R.string.questionnaire_alert_back_pressed_title,
        confirmButton =
          AlertDialogButton(
            listener = {
              lifecycleScope.launch {
                retrieveQuestionnaireResponse()?.let { questionnaireResponse ->
                  viewModel.saveDraftQuestionnaire(questionnaireResponse, questionnaireConfig)
                  setResult(
                    Activity.RESULT_OK,
                    Intent().apply {
                      putExtra(QUESTIONNAIRE_RESPONSE, questionnaireResponse as Serializable)
                      putExtra(QUESTIONNAIRE_CONFIG, questionnaireConfig as Parcelable)
                      putExtra(ON_RESULT_TYPE, ActivityOnResultType.QUESTIONNAIRE.name)
                    },
                  )
                  finish()
                }
              }
            },
            text =
              org.smartregister.fhircore.engine.R.string
                .questionnaire_alert_back_pressed_save_draft_button_title,
            color = org.smartregister.fhircore.engine.R.color.colorPrimary,
          ),
        neutralButton =
          AlertDialogButton(
            listener = {},
            text =
              org.smartregister.fhircore.engine.R.string.questionnaire_alert_neutral_button_title,
          ),
        negativeButton =
          AlertDialogButton(
            listener = { finish() },
            text =
              org.smartregister.fhircore.engine.R.string.questionnaire_alert_negative_button_title,
            color = org.smartregister.fhircore.engine.R.color.colorPrimary,
          ),
      )
    } else {
      AlertDialogue.showConfirmAlert(
        context = this,
        message =
          org.smartregister.fhircore.engine.R.string.questionnaire_alert_back_pressed_message,
        title = org.smartregister.fhircore.engine.R.string.questionnaire_alert_back_pressed_title,
        confirmButtonListener = { finish() },
        confirmButtonText =
          org.smartregister.fhircore.engine.R.string.questionnaire_alert_back_pressed_button_title,
      )
    }
  }

  private suspend fun retrieveQuestionnaireResponse(): QuestionnaireResponse? =
    (supportFragmentManager.findFragmentByTag(QUESTIONNAIRE_FRAGMENT_TAG) as QuestionnaireFragment?)
      ?.getQuestionnaireResponse()

  companion object {

    const val QUESTIONNAIRE_FRAGMENT_TAG = "questionnaireFragment"
    const val QUESTIONNAIRE_CONFIG = "questionnaireConfig"
    const val QUESTIONNAIRE_SUBMISSION_EXTRACTED_RESOURCE_IDS = "questionnaireExtractedResourceIds"
    const val QUESTIONNAIRE_RESPONSE = "questionnaireResponse"
    const val QUESTIONNAIRE_ACTION_PARAMETERS = "questionnaireActionParameters"

    fun intentBundle(
      questionnaireConfig: QuestionnaireConfig,
      actionParams: List<ActionParameter>,
    ): Bundle =
      bundleOf(
        Pair(QUESTIONNAIRE_CONFIG, questionnaireConfig),
        Pair(QUESTIONNAIRE_ACTION_PARAMETERS, actionParams),
      )
  }
}
