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

package org.smartregister.fhircore.eir.ui.patient.details

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import org.hl7.fhir.r4.model.Immunization
import org.hl7.fhir.r4.model.Patient
import org.smartregister.fhircore.eir.R
import org.smartregister.fhircore.eir.databinding.FragmentPatientDetailsBinding
import org.smartregister.fhircore.eir.ui.adverseevent.AdverseEventQuestionnaireActivity
import org.smartregister.fhircore.eir.ui.vaccine.RecordVaccineActivity
import org.smartregister.fhircore.eir.util.ADVERSE_EVENT_FORM
import org.smartregister.fhircore.eir.util.EirConfigClassification
import org.smartregister.fhircore.eir.util.RECORD_VACCINE_FORM
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.view.ConfigurableView
import org.smartregister.fhircore.engine.configuration.view.ImmunizationProfileViewConfiguration
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity
import org.smartregister.fhircore.engine.util.extension.extractAge
import org.smartregister.fhircore.engine.util.extension.extractGender
import org.smartregister.fhircore.engine.util.extension.extractName
import org.smartregister.fhircore.engine.util.extension.hide
import org.smartregister.fhircore.engine.util.extension.show
import org.smartregister.fhircore.engine.util.extension.toggleVisibility

@AndroidEntryPoint
class PatientDetailsFragment : Fragment(), ConfigurableView<ImmunizationProfileViewConfiguration> {

  @Inject lateinit var configurationRegistry: ConfigurationRegistry

  val patientDetailsViewModel: PatientDetailsViewModel by viewModels()

  private lateinit var patientId: String

  private lateinit var patientDetailsFragmentBinding: FragmentPatientDetailsBinding

  private val patientImmunizationsAdapter = PatientImmunizationsAdapter()

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    patientDetailsFragmentBinding =
      DataBindingUtil.inflate(inflater, R.layout.fragment_patient_details, container, false)
    return patientDetailsFragmentBinding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    patientId = arguments?.getString(QuestionnaireActivity.QUESTIONNAIRE_ARG_PATIENT_KEY) ?: ""

    setupViews(patientId)

    patientDetailsViewModel.patientDemographics.observe(
      viewLifecycleOwner,
      this::handlePatientDemographics
    )

    patientDetailsViewModel.patientImmunizations.observe(
      viewLifecycleOwner,
      this::handleImmunizations
    )
    val viewConfiguration =
      configurationRegistry.retrieveConfiguration<ImmunizationProfileViewConfiguration>(
        configClassification = EirConfigClassification.IMMUNIZATION_PROFILE
      )
    configureViews(viewConfiguration)
  }

  override fun onResume() {
    super.onResume()
    patientDetailsViewModel.run {
      fetchDemographics(patientId)
      fetchImmunizations(patientId)
    }
  }

  private fun setupViews(patientId: String) {
    patientDetailsFragmentBinding.immunizationsListView.apply {
      adapter = patientImmunizationsAdapter
      layoutManager = LinearLayoutManager(requireContext())
    }

    patientDetailsFragmentBinding.recordVaccineButton.setOnClickListener {
      startActivity(
        Intent(requireContext(), RecordVaccineActivity::class.java)
          .putExtras(
            QuestionnaireActivity.intentArgs(
              clientIdentifier = patientId,
              formName = RECORD_VACCINE_FORM
            )
          )
      )
    }

    patientDetailsFragmentBinding.reportAdverseEventButton.setOnClickListener {
      val immunizations = patientDetailsViewModel.patientImmunizations.value as List<Immunization>
      val immunizationItemWithIds =
        immunizations.toImmunizationAdverseEventItem(requireContext()).first()

      goToAdverseEventQuestionnaireActivity(immunizationItemWithIds, patientId)
    }
  }

  private fun goToAdverseEventQuestionnaireActivity(
    immunizationAdverseEventItem: ImmunizationAdverseEventItem,
    patientId: String
  ) {

    val list = arrayListOf<String>()
    immunizationAdverseEventItem.dosesWithAdverseEvents.forEach { dose -> list.add(dose.first) }

    AlertDialog.Builder(requireActivity(), R.style.Theme_AppCompat_Light_Dialog_Alert)
      .apply {
        setTitle(
          requireContext()
            .getString(R.string.choose_dose_adverse_event, immunizationAdverseEventItem.vaccine)
        )
        setNegativeButton(R.string.cancel) { dialog: DialogInterface, _ -> dialog.dismiss() }
        setSingleChoiceItems(list.toTypedArray(), -1) { dialog: DialogInterface, position: Int ->
          startActivity(
            Intent(requireContext(), AdverseEventQuestionnaireActivity::class.java)
              .putExtras(
                QuestionnaireActivity.intentArgs(
                  clientIdentifier = patientId,
                  formName = ADVERSE_EVENT_FORM,
                  immunizationId = immunizationAdverseEventItem.immunizationIds[position]
                )
              )
          )
          dialog.dismiss()
        }
      }
      .create()
      .show()
  }

  private fun handlePatientDemographics(patient: Patient) {
    with(patient) {
      patientDetailsFragmentBinding.patientNameTextView.text = extractName()
      patientDetailsFragmentBinding.patientGenderTextView.text = extractGender(requireContext())
      patientDetailsFragmentBinding.patientAgeTextView.text = getString(R.string.age, extractAge())
    }
  }

  private fun handleImmunizations(immunizations: List<Immunization>) {
    val configuration = patientDetailsViewModel.immunizationProfileConfiguration.value
    when {
      immunizations.isEmpty() -> {
        toggleImmunizationStatus(fullyImmunized = false)
        patientDetailsFragmentBinding.noVaccinesTextView.show()
        patientDetailsFragmentBinding.immunizationsListView.hide()
        patientDetailsFragmentBinding.recordVaccineButton.show()
        patientDetailsFragmentBinding.showQRCodeButton.hide()
        patientDetailsFragmentBinding.reportAdverseEventButton.hide()
      }
      immunizations.size == 1 -> {
        toggleImmunizationStatus(fullyImmunized = false)
        populateImmunizationList(immunizations)
        patientDetailsFragmentBinding.recordVaccineButton.show()
        patientDetailsFragmentBinding.showQRCodeButton.hide()
        val showAdverseReportButton =
          immunizations.size == 1 && configuration != null && configuration.showReportAdverseEvent
        patientDetailsFragmentBinding.reportAdverseEventButton.toggleVisibility(
          showAdverseReportButton
        )
        patientDetailsFragmentBinding.noVaccinesTextView.hide()
      }
      else -> {
        toggleImmunizationStatus(fullyImmunized = true)
        populateImmunizationList(immunizations)
        patientDetailsFragmentBinding.noVaccinesTextView.hide()
        patientDetailsFragmentBinding.recordVaccineButton.hide()
        patientDetailsFragmentBinding.showQRCodeButton.toggleVisibility(
          configuration != null && configuration.showScanBarcode
        )
        patientDetailsFragmentBinding.reportAdverseEventButton.toggleVisibility(
          configuration != null && configuration.showReportAdverseEvent
        )
      }
    }
  }

  private fun populateImmunizationList(immunizations: List<Immunization>) {
    patientDetailsFragmentBinding.immunizationsListView.show()
    patientImmunizationsAdapter.submitList(immunizations.toImmunizationItems(requireContext()))
  }

  private fun toggleImmunizationStatus(fullyImmunized: Boolean = false) {
    patientDetailsFragmentBinding.immuneStatusImageView.background =
      if (fullyImmunized) ContextCompat.getDrawable(requireContext(), R.drawable.ic_check)
      else ContextCompat.getDrawable(requireContext(), R.drawable.ic_close)

    patientDetailsFragmentBinding.immuneTextView.apply {
      text = if (fullyImmunized) getString(R.string.immune) else getString(R.string.not_immune)
      setTextColor(
        if (fullyImmunized) ContextCompat.getColor(requireContext(), R.color.immune)
        else ContextCompat.getColor(requireContext(), R.color.not_immune)
      )
    }
  }

  override fun configureViews(viewConfiguration: ImmunizationProfileViewConfiguration) {
    patientDetailsViewModel.updateViewConfiguration(viewConfiguration)
  }

  override fun setupConfigurableViews(viewConfiguration: ImmunizationProfileViewConfiguration) {
    // Overridden. The configurable views are updated dynamically this is not required
  }

  companion object {
    const val TAG = "PatientDetailsFragment"

    fun newInstance(bundle: Bundle = Bundle()) =
      PatientDetailsFragment().apply { arguments = bundle }
  }
}
