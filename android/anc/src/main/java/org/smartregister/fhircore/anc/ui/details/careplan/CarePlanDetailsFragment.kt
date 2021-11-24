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

package org.smartregister.fhircore.anc.ui.details.careplan

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import org.smartregister.fhircore.anc.R
import org.smartregister.fhircore.anc.data.model.CarePlanItem
import org.smartregister.fhircore.anc.data.model.UpcomingServiceItem
import org.smartregister.fhircore.anc.databinding.FragmentNonAncDetailsBinding
import org.smartregister.fhircore.anc.ui.details.adapter.CarePlanAdapter
import org.smartregister.fhircore.anc.ui.details.adapter.UpcomingServicesAdapter
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity

@AndroidEntryPoint
class CarePlanDetailsFragment : Fragment() {

  private lateinit var patientId: String
  val ancDetailsViewModel by viewModels<CarePlanDetailsViewModel>()
  private val carePlanAdapter = CarePlanAdapter()
  private val upcomingServicesAdapter = UpcomingServicesAdapter()

  lateinit var binding: FragmentNonAncDetailsBinding

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    binding = DataBindingUtil.inflate(inflater, R.layout.fragment_non_anc_details, container, false)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    patientId = arguments?.getString(QuestionnaireActivity.QUESTIONNAIRE_ARG_PATIENT_KEY) ?: ""

    setupViews()

    ancDetailsViewModel.patientId = patientId
    ancDetailsViewModel.fetchCarePlan().observe(viewLifecycleOwner, this::handleCarePlan)
    ancDetailsViewModel.fetchEncounters().observe(viewLifecycleOwner, this::handleEncounters)
  }

  private fun handleEncounters(listEncounters: List<UpcomingServiceItem>) {
    when {
      listEncounters.isEmpty() -> {
        binding.txtViewNoUpcomingServices.visibility = View.VISIBLE
        binding.upcomingServicesListView.visibility = View.GONE
        binding.txtViewUpcomingServicesSeeAllHeading.visibility = View.GONE
        binding.imageViewUpcomingServicesSeeAllArrow.visibility = View.GONE
      }
      else -> {
        binding.txtViewNoUpcomingServices.visibility = View.GONE
        binding.upcomingServicesListView.visibility = View.VISIBLE
        binding.txtViewUpcomingServicesSeeAllHeading.visibility = View.VISIBLE
        binding.txtViewUpcomingServicesSeeAllHeading.visibility = View.VISIBLE
        populateUpcomingServicesList(listEncounters)
      }
    }
  }

  private fun setupViews() {
    binding.carePlanListView.apply {
      adapter = carePlanAdapter
      layoutManager = LinearLayoutManager(requireContext())
    }
  }

  companion object {
    fun newInstance(bundle: Bundle = Bundle()) =
      CarePlanDetailsFragment().apply { arguments = bundle }
  }

  private fun handleCarePlan(immunizations: List<CarePlanItem>) {
    when {
      immunizations.isEmpty() -> {
        binding.txtViewNoCarePlan.visibility = View.VISIBLE
        binding.txtViewCarePlanSeeAllHeading.visibility = View.GONE
        binding.imageViewSeeAllArrow.visibility = View.GONE
        binding.carePlanListView.visibility = View.GONE
      }
      else -> {
        binding.txtViewNoCarePlan.visibility = View.GONE
        binding.txtViewCarePlanSeeAllHeading.visibility = View.VISIBLE
        binding.imageViewSeeAllArrow.visibility = View.VISIBLE
        binding.carePlanListView.visibility = View.VISIBLE
        populateCarePlanList(immunizations)
      }
    }
  }

  private fun populateCarePlanList(listCarePlan: List<CarePlanItem>) {
    carePlanAdapter.submitList(listCarePlan)
  }

  private fun populateUpcomingServicesList(upcomingServiceItem: List<UpcomingServiceItem>) {
    upcomingServicesAdapter.submitList(upcomingServiceItem)
  }
}
