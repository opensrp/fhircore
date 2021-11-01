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
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.fhir.FhirEngine
import org.smartregister.fhircore.anc.AncApplication
import org.smartregister.fhircore.anc.R
import org.smartregister.fhircore.anc.data.model.EncounterItem
import org.smartregister.fhircore.anc.data.patient.PatientRepository
import org.smartregister.fhircore.anc.databinding.FragmentVitalDetailsBinding
import org.smartregister.fhircore.anc.ui.anccare.details.AncPatientItemMapper
import org.smartregister.fhircore.anc.ui.details.adapter.AllergiesAdapter
import org.smartregister.fhircore.anc.ui.details.adapter.ConditionsAdapter
import org.smartregister.fhircore.anc.ui.details.adapter.EncounterAdapter
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity
import org.smartregister.fhircore.engine.util.extension.createFactory

class VitalSignsDetailsFragment : Fragment() {

  private lateinit var patientId: String
  private lateinit var fhirEngine: FhirEngine

  lateinit var ancDetailsViewModel: VitalSignsDetailsViewModel

  private val allergiesAdapter = AllergiesAdapter()
  private val conditionsAdapter = ConditionsAdapter()
  private val encounterAdapter = EncounterAdapter()

  private lateinit var ancPatientRepository: PatientRepository

  lateinit var binding: FragmentVitalDetailsBinding

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
    patientId = arguments?.getString(QuestionnaireActivity.QUESTIONNAIRE_ARG_PATIENT_KEY) ?: ""

    fhirEngine = AncApplication.getContext().fhirEngine

    setupViews()

    ancPatientRepository =
      PatientRepository(
        (requireActivity().application as AncApplication).fhirEngine,
        AncPatientItemMapper
      )

    ancDetailsViewModel =
      ViewModelProvider(
        viewModelStore,
        VitalSignsDetailsViewModel(ancPatientRepository, patientId = patientId).createFactory()
      )[VitalSignsDetailsViewModel::class.java]

    ancDetailsViewModel.fetchEncounters().observe(viewLifecycleOwner, this::handleEncounters)
  }

  private fun handleEncounters(listEncounters: List<EncounterItem>) {
    when {
      listEncounters.isEmpty() -> {
        binding.txtViewNoEncounter.visibility = View.VISIBLE
        binding.encounterListView.visibility = View.GONE
      }
      else -> {
        binding.txtViewNoEncounter.visibility = View.GONE
        binding.encounterListView.visibility = View.VISIBLE
        populateEncounterList(listEncounters)
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

  private fun populateEncounterList(listEncounters: List<EncounterItem>) {
    encounterAdapter.submitList(arrayListOf())
  }

  companion object {
    fun newInstance(bundle: Bundle = Bundle()) =
      VitalSignsDetailsFragment().apply { arguments = bundle }
  }
}
