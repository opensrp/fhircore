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

package org.smartregister.fhircore.quest.ui.patient.register

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import ca.uhn.fhir.context.FhirContext
import com.famoco.desfireservicelib.CardReaderState
import com.famoco.desfireservicelib.DESFireServiceAccess
import com.famoco.desfireservicelib.ServiceConnectionState
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.Resource
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.view.RegisterViewConfiguration
import org.smartregister.fhircore.engine.configuration.view.getString
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.nfc.MainViewModel
import org.smartregister.fhircore.engine.nfc.main.PatientNfcItem
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity.Companion.QUESTIONNAIRE_ARG_PATIENT_KEY
import org.smartregister.fhircore.engine.ui.register.BaseRegisterActivity
import org.smartregister.fhircore.engine.ui.register.model.NavigationMenuOption
import org.smartregister.fhircore.engine.ui.register.model.RegisterItem
import org.smartregister.fhircore.engine.ui.userprofile.UserProfileFragment
import org.smartregister.fhircore.quest.R
import org.smartregister.fhircore.quest.ui.patient.details.QuestPatientDetailActivity
import org.smartregister.fhircore.quest.util.QuestConfigClassification

@AndroidEntryPoint
class PatientRegisterActivity : BaseRegisterActivity() {

  @Inject lateinit var configurationRegistry: ConfigurationRegistry
  @Inject lateinit var defaultRepository: DefaultRepository

  private val mainViewModel: MainViewModel by viewModels()

  private lateinit var eventJob: Job

  private var scanForRegistration = true
  private var desFireServiceObserversAdded = false
  private val desFireServiceConnectionStateObserver: Observer<in ServiceConnectionState> =
      Observer {
    val state = it.name
  }
  private val cardReaderStateObserver: Observer<in CardReaderState> = Observer {
    val cardReaderState = it.name
  }
  private var readResultObserver : Observer<in Array<String>>? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val registerViewConfiguration =
      configurationRegistry.retrieveConfiguration<RegisterViewConfiguration>(
        configClassification = QuestConfigClassification.PATIENT_REGISTER
      )
    configureViews(registerViewConfiguration)
    // Connect to DES service
    mainViewModel.connectService(this)

    // Add DES service listeners
    addDesServiceListeners()

    // loadLocalDevWfpCodaFiles()
  }

  fun loadLocalDevWfpCodaFiles() {
    val files =
      listOf(
        "fhir-questionnaires/CODA/anthro-following-visit.json",
        "fhir-questionnaires/CODA/assistance-visit.json",
        "fhir-questionnaires/CODA/coda-child-registration.json",
        "fhir-questionnaires/CODA/coda-child-structure-map.json",
      )

    files.forEach { fileName ->
      val jsonString = assets.open(fileName).bufferedReader().readText()
      val questionnaire = FhirContext.forR4Cached().newJsonParser().parseResource(jsonString)

      GlobalScope.launch { defaultRepository.addOrUpdate(questionnaire as Resource) }
    }
  }

  override fun bottomNavigationMenuOptions(): List<NavigationMenuOption> {
    return listOf(
      NavigationMenuOption(
        id = R.id.menu_item_clients,
        title = getString(R.string.menu_clients),
        iconResource = ContextCompat.getDrawable(this, R.drawable.ic_users)!!
      ),
      NavigationMenuOption(
        id = R.id.scan_nfc,
        title = getString(R.string.scan_nfc),
        iconResource = ContextCompat.getDrawable(this, R.drawable.ic_nfc_card)!!
      ),
      NavigationMenuOption(
        id = R.id.menu_item_settings,
        title = getString(R.string.menu_settings),
        iconResource = ContextCompat.getDrawable(this, R.drawable.ic_settings)!!
      )
    )
  }

  override fun onNavigationOptionItemSelected(item: MenuItem): Boolean {
    when (item.itemId) {
      R.id.menu_item_clients -> switchFragment(mainFragmentTag())
      R.id.menu_item_settings ->
        switchFragment(
          tag = UserProfileFragment.TAG,
          isRegisterFragment = false,
          toolbarTitle = getString(R.string.settings)
        )
      R.id.scan_nfc -> readFromCard(false)
    }
    return true
  }

  override fun mainFragmentTag() = PatientRegisterFragment.TAG

  override fun supportedFragments(): Map<String, Fragment> =
    mapOf(
      Pair(PatientRegisterFragment.TAG, PatientRegisterFragment()),
      Pair(UserProfileFragment.TAG, UserProfileFragment())
    )

  override fun registersList(): List<RegisterItem> =
    listOf(
      RegisterItem(
        uniqueTag = PatientRegisterFragment.TAG,
        title = registerViewModel.registerViewConfiguration.value?.appTitle_lang?.getString(this)
            ?: getString(R.string.clients),
        isSelected = true
      )
    )

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

  override fun onPause() {
    if (desFireServiceObserversAdded) {
      DESFireServiceAccess.DESFireServiceConnectionState.removeObserver(desFireServiceConnectionStateObserver)
      DESFireServiceAccess.cardReaderState.removeObserver(cardReaderStateObserver)
      DESFireServiceAccess.readResult.removeObserver(readResultObserver!!)
    }

    super.onPause()
    // In order to consume the event in ReadNfcActivity or WriteNfcActivity,
    // we need to cancel this eventJob before leaving the MainActivity,
    // because the event will be consumed only once
    eventJob.cancel()
  }

  private fun readFromCard(isRegistration: Boolean = true) {
    scanForRegistration = isRegistration
    mainViewModel.generateProtoFile()
    // InitializeSAM
    mainViewModel.initSAM()
    // Perform Read action with the UI given by the Service
    mainViewModel.readSerialized()
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
      readResultObserver = Observer { result ->
        if (!result.isNullOrEmpty()) {
          val stringBuilder = StringBuilder().append("")
          result.forEach { stringBuilder.append(it) }
          val readResult = stringBuilder.toString()
          if (scanForRegistration) {
            if (readResult == "{\n}") {
              showAgeDialog { dialog, which -> dialog.dismiss() }
            } else {
              showEraseCardDialog { dialog, which -> dialog.dismiss() }
            }
          } else {
            val patientNFCItem = Gson().fromJson(readResult, PatientNfcItem::class.java)
            navigateToDetails(patientNFCItem.patientId)
          }
        }
      }
    }

    // After reading the card, the end-user will need the content that has been read on the card
    DESFireServiceAccess.readResult.observe(this, readResultObserver!!)
  }

  private fun writeToCard(isDelete: Boolean = false) {
    mainViewModel.generateProtoFile()
    // InitializeSAM
    mainViewModel.initSAM()
    // Perform Write action with the UI given by the Service
    val patient =
      PatientNfcItem(
        patientId = "",
        firstName = "",
        lastName = "",
        middleName = "",
        age = "",
        birthDate = "",
        gender = "",
        caretakerName = "",
        caretakerRelationship = "",
        village = "",
        healthCenter = "",
        beneficiaryGroup = "",
        registrationDate = "",
        creationDate = ""
      )
    val json: String =
      if (isDelete) {
        "{}"
      } else {
        Gson().toJson(patient)
      }
    mainViewModel.writeSerialized(json)
  }

  override fun registerClient(clientIdentifier: String?) {
    //showAgeDialog({ dialog, which -> dialog.dismiss() })
    readFromCard()
  }

  fun showAgeDialog(cancelClickListener: DialogInterface.OnClickListener) {
    val layout =
      LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        layoutParams =
          ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
          )
        setPadding(50, 20, 50, 20)
      }
    val input =
      EditText(this).apply {
        setHint(getString(R.string.enter_age_in_months))
        inputType = InputType.TYPE_CLASS_NUMBER
      }

    layout.addView(input)

    AlertDialog.Builder(this)
      .setTitle(getString(R.string.enter_beneficiary_age))
      .setView(layout)
      .setPositiveButton(android.R.string.ok) { dialog, which ->
        val age = input.text.toString()

        if (age.isNotEmpty() && age.toInt() in (6..59)) {
          initiateClientRegistration()
        } else {
          Toast.makeText(
              this,
              getString(R.string.beneficiary_not_eligible_for_program),
              Toast.LENGTH_LONG
            )
            .show()
        }
        dialog.dismiss()
      }
      .setNegativeButton(R.string.cancel, cancelClickListener)
      .show()
  }

  private fun showEraseCardDialog(cancelClickListener: DialogInterface.OnClickListener) {

    AlertDialog.Builder(this)
      .setTitle(getString(R.string.card_not_blank))
      .setMessage(getString(R.string.card_has_exisiting_data))
      .setPositiveButton(R.string.erase_card) { dialog, which ->
        writeToCard(true)
        dialog.dismiss()
      }
      .setNegativeButton(R.string.cancel, cancelClickListener)
      .show()
  }

  private fun initiateClientRegistration(clientIdentifier: String? = null) {
    startActivity(
      Intent(this, QuestionnaireActivity::class.java)
        .putExtras(
          QuestionnaireActivity.intentArgs(
            clientIdentifier = clientIdentifier,
            formName = registerViewModel.registerViewConfiguration.value?.registrationForm!!
          )
        )
    )
  }

  private fun navigateToDetails(uniqueIdentifier: String) {
    startActivity(
      Intent(this, QuestPatientDetailActivity::class.java)
        .putExtra(QUESTIONNAIRE_ARG_PATIENT_KEY, uniqueIdentifier)
    )
  }


}
