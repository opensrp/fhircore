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

package org.smartregister.fhircore.anc.ui.details.vitalsigns

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import org.smartregister.fhircore.anc.R
import org.smartregister.fhircore.anc.data.model.EncounterItem
import org.smartregister.fhircore.anc.data.model.PatientVitalItem
import org.smartregister.fhircore.anc.databinding.FragmentVitalDetailsBinding
import org.smartregister.fhircore.anc.ui.anccare.shared.AncItemMapper
import org.smartregister.fhircore.anc.ui.details.adapter.AllergiesAdapter
import org.smartregister.fhircore.anc.ui.details.adapter.ConditionsAdapter
import org.smartregister.fhircore.anc.ui.details.adapter.EncounterAdapter
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity
import org.smartregister.fhircore.engine.util.extension.hide
import org.smartregister.fhircore.engine.util.extension.show

@AndroidEntryPoint
class VitalSignsDetailsFragment : Fragment() {

  @Inject lateinit var allergiesAdapter: AllergiesAdapter

  @Inject lateinit var conditionsAdapter: ConditionsAdapter

  @Inject lateinit var encounterAdapter: EncounterAdapter

  lateinit var binding: FragmentVitalDetailsBinding

  val ancDetailsViewModel by viewModels<VitalSignsDetailsViewModel>()

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    binding = DataBindingUtil.inflate(inflater, R.layout.fragment_vital_details, container, false)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    val patientId = arguments?.getString(QuestionnaireActivity.QUESTIONNAIRE_ARG_PATIENT_KEY) ?: ""

    setupViews()

    ancDetailsViewModel.patientRepository.setAncItemMapperType(
      AncItemMapper.AncItemMapperType.DETAILS
    )
    ancDetailsViewModel
      .fetchEncounters(patientId)
      .observe(viewLifecycleOwner, this::handleEncounters)

    ancDetailsViewModel
      .fetchVitalSigns(patientId)
      .observe(viewLifecycleOwner, this::handleVitalSigns)

    binding.swipeContainer.setOnRefreshListener {
      ancDetailsViewModel
        .fetchEncounters(patientId)
        .observe(viewLifecycleOwner, this::handleEncounters)
      ancDetailsViewModel
        .fetchVitalSigns(patientId)
        .observe(viewLifecycleOwner, this::handleVitalSigns)
    }

    binding.swipeContainer.setColorSchemeResources(
      R.color.colorPrimary,
      R.color.colorPrimaryLight,
      R.color.colorAccent,
      R.color.colorPrimaryLightDull
    )
  }

  private fun handleEncounters(listEncounters: List<EncounterItem>) {
    when {
      listEncounters.isEmpty() -> {
        binding.apply {
          txtViewNoEncounter.show()
          encounterListView.hide()
        }
      }
      else -> {
        binding.apply {
          txtViewNoEncounter.hide()
          encounterListView.show()
        }
        encounterAdapter.submitList(listEncounters)
      }
    }
  }

  private fun handleVitalSigns(patientVitalItem: PatientVitalItem) {
    binding.swipeContainer.isRefreshing = false
    binding.apply {
      txtViewWeightValue.text =
        if (patientVitalItem.weight.isEmpty()) "-" else patientVitalItem.weight
      txtViewWeightUnit.text = patientVitalItem.weightUnit
      txtViewHeightValue.text =
        if (patientVitalItem.height.isEmpty()) "-" else patientVitalItem.height
      txtViewHeightUnit.text = patientVitalItem.heightUnit
      txtViewBgValue.text = if (patientVitalItem.BG.isEmpty()) "-" else patientVitalItem.BG
      txtViewBgUnit.text = patientVitalItem.BGUnit
      txtViewSpValue.text = if (patientVitalItem.sp02.isEmpty()) "-" else patientVitalItem.sp02
      txtViewSpUnit.text = patientVitalItem.sp02Unit
      txtViewPulseValue.text = if (patientVitalItem.pulse.isEmpty()) "-" else patientVitalItem.pulse
      txtViewPulseUnit.text = patientVitalItem.pulseUnit
      txtViewBpValue.text = if (patientVitalItem.BPS.isEmpty()) "-" else patientVitalItem.BPS
      txtViewBpUnit.text = patientVitalItem.BPSUnit
      txtViewWeightValue.text = patientVitalItem.weight
      txtViewHeightValue.text = patientVitalItem.height
      txtViewBgValue.text = patientVitalItem.BG
      txtViewSpValue.text = patientVitalItem.sp02
      txtViewPulseValue.text = patientVitalItem.pulse
      txtViewBpValue.text = patientVitalItem.BPS
      txtViewHeightUnit.text = patientVitalItem.heightUnit
      txtViewWeightUnit.text = patientVitalItem.weightUnit
      if (patientVitalItem.bmi.isNotEmpty()) {
        linearLayoutBmi.show()
        txtViewBmiValue.text = patientVitalItem.bmi
        txtViewBmiUnit.text = patientVitalItem.bmiUnit
      }
    }
  }

  private fun setupViews() {
    binding.allergiesListView.apply {
      adapter = allergiesAdapter
      layoutManager = LinearLayoutManager(requireContext())
    }
    binding.conditionsListView.apply {
      adapter = conditionsAdapter
      layoutManager = LinearLayoutManager(requireContext())
    }
    binding.encounterListView.apply {
      adapter = encounterAdapter
      layoutManager = LinearLayoutManager(requireContext())
    }
  }

  companion object {
    const val TAG = "VitalSignsDetailsFragment"

    fun newInstance(bundle: Bundle = Bundle()) =
      VitalSignsDetailsFragment().apply { arguments = bundle }
  }
}
