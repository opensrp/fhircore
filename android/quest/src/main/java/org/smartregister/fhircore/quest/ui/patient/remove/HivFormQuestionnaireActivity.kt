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

package org.smartregister.fhircore.quest.ui.patient.remove

import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.activity.viewModels
import org.hl7.fhir.r4.model.Binary
import org.hl7.fhir.r4.model.CarePlan
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.StringType
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity
import org.smartregister.fhircore.engine.util.extension.asCarePlanResource
import org.smartregister.fhircore.engine.util.extension.extractName
import org.smartregister.fhircore.engine.util.extension.find
import org.smartregister.fhircore.quest.R
import org.smartregister.fhircore.quest.ui.patient.profile.PatientProfileViewModel.Companion.HIV_TEST_AND_NEXT_APPOINTMENT_FORM
import org.smartregister.fhircore.quest.ui.patient.profile.PatientProfileViewModel.Companion.HIV_TEST_AND_RESULTS_FORM
import org.smartregister.fhircore.quest.ui.patient.profile.PatientProfileViewModel.Companion.VIRAL_LOAD_RESULTS_FORM

class HivFormQuestionnaireActivity : QuestionnaireActivity() {

  private val viewModel by viewModels<HivPatientViewModel>()

  private lateinit var btnRemove: Button
  private lateinit var profileId: String
  private lateinit var profileName: String

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    profileId = intent.extras?.getString(QUESTIONNAIRE_ARG_PATIENT_KEY) ?: ""

    //    btnRemove = findViewById(org.smartregister.fhircore.engine.R.id.btn_save_client_info)
    //    btnRemove.text = setRemoveButtonText()

    viewModel.apply {
      //      isRemoved.observe(this@HivFormQuestionnaireActivity) { if (it) onRemove() }
      isDiscarded.observe(this@HivFormQuestionnaireActivity) { if (it) finish() }
      profile.observe(this@HivFormQuestionnaireActivity) { onReceive(it) }
    }
    viewModel.fetch(profileId)
  }

  override fun populateInitialValues(
    questionnaire: Questionnaire,
    initialResource: Array<Resource>
  ) {

    val binaryItems = initialResource.filterIsInstance<Binary>()
    val targetResource = binaryItems[0]
    val carePlan: CarePlan = targetResource.asCarePlanResource()
    println("careplan-id===" + carePlan.id)
    //    val resourceList: ArrayList<CarePlan> =  (binaryItems as
    // ArrayList<Binary>).asCarePlanResource()
    if (initialResource.isNotEmpty() && questionnaireConfig.form == VIRAL_LOAD_RESULTS_FORM ||
        questionnaireConfig.form == HIV_TEST_AND_RESULTS_FORM ||
        questionnaireConfig.form == HIV_TEST_AND_NEXT_APPOINTMENT_FORM
    ) {
      questionnaire.find(CARE_PLAN_ID_KEY)!!.initialFirstRep.value = StringType(carePlan.id)
    }
    super.populateInitialValues(questionnaire, initialResource)
  }

  //  @OptIn(ExperimentalMaterialApi::class)
  //  fun onRemove() {
  //    val intent =
  //      Intent(this, AppMainActivity::class.java).apply {
  //        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
  //      }
  //    run {
  //      startActivity(intent)
  //      finish()
  //    }
  //  }

  fun onReceive(profile: Patient) {
    profileName = profile.extractName()
  }

  override fun onClick(view: View) {
    if (view.id == org.smartregister.fhircore.engine.R.id.btn_save_client_info) {
      handleQuestionnaireSubmit()
    }
  }

  //  override fun handleQuestionnaireResponse(questionnaireResponse: QuestionnaireResponse) {
  //    dismissSaveProcessing()
  //    confirmationDialog(profileId = profileId, profileName = profileName)
  //  }
  //
  //  private fun confirmationDialog(profileId: String, profileName: String) {
  //    AlertDialogue.showAlert(
  //      context = this,
  //      alertIntent = AlertIntent.CONFIRM,
  //      title = setRemoveDialogTitle(),
  //      message = setRemoveDialogMessage(profileName),
  //      confirmButtonListener = { dialog ->
  //        viewModel.remove(profileId)
  //        dialog.dismiss()
  //      },
  //      neutralButtonListener = { dialog ->
  //        viewModel.discard()
  //        dialog.dismiss()
  //      }
  //    )
  //  }
  //
  //  fun setRemoveButtonText(): String = getString(R.string.remove_this_person)
  //
  //  fun setRemoveDialogTitle(): String = getString(R.string.remove_this_person)
  //
  //  fun setRemoveDialogMessage(profileName: String): String =
  //    getString(R.string.remove_hiv_patient_warning, profileName)

  companion object {
    const val CARE_PLAN_ID_KEY = "care-plan-id"
  }
}
