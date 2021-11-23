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

package org.smartregister.fhircore.anc.ui.anccare.details

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import org.smartregister.fhircore.anc.AncApplication
import org.smartregister.fhircore.anc.R
import org.smartregister.fhircore.anc.data.model.AncOverviewItem
import org.smartregister.fhircore.anc.data.model.CarePlanItem
import org.smartregister.fhircore.anc.data.model.EncounterItem
import org.smartregister.fhircore.anc.data.model.UpcomingServiceItem
import org.smartregister.fhircore.anc.data.patient.PatientRepository
import org.smartregister.fhircore.anc.databinding.FragmentAncDetailsBinding
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity
import org.smartregister.fhircore.engine.util.extension.createFactory
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class AncDetailsFragment : Fragment() {

  lateinit var patientId: String

  val ancDetailsViewModel by viewModels<AncDetailsViewModel>()

  @Inject lateinit var patientRepository: PatientRepository
  @Inject lateinit var ancPatientItemMapper: AncPatientItemMapper

  private var carePlanAdapter = CarePlanAdapter()

  private val upcomingServicesAdapter = UpcomingServicesAdapter()

  private val lastSeen = EncounterAdapter()

  lateinit var binding: FragmentAncDetailsBinding

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    binding = DataBindingUtil.inflate(inflater, R.layout.fragment_anc_details, container, false)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    patientId = arguments?.getString(QuestionnaireActivity.QUESTIONNAIRE_ARG_PATIENT_KEY) ?: ""

    setupViews()

    // TODO: We might have a crash if patientRepository is used here
    //patientRepository = getAncPatientRepository()

    patientRepository.domainMapperInUse = ancPatientItemMapper
    ancDetailsViewModel.patientId = patientId
    Timber.d(patientId)

    ancDetailsViewModel.fetchCarePlan().observe(viewLifecycleOwner, this::handleCarePlan)

    ancDetailsViewModel.fetchObservation().observe(viewLifecycleOwner, this::handleObservation)

    ancDetailsViewModel
      .fetchUpcomingServices()
      .observe(viewLifecycleOwner, this::handleUpcomingServices)
    ancDetailsViewModel.fetchCarePlan().observe(viewLifecycleOwner, this::handleCarePlan)

    ancDetailsViewModel.fetchLastSeen().observe(viewLifecycleOwner, this::handleLastSeen)
  }

  private fun handleObservation(ancOverviewItem: AncOverviewItem) {
    binding.txtViewEDDDoseDate.text = ancOverviewItem.edd
    binding.txtViewGAPeriod.text = ancOverviewItem.ga
    binding.txtViewFetusesCount.text = ancOverviewItem.noOfFetuses
    binding.txtViewRiskValue.text = ancOverviewItem.risk
  }

  private fun handleUpcomingServices(listEncounters: List<UpcomingServiceItem>) {
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

  private fun handleLastSeen(listEncounters: List<EncounterItem>) {
    when {
      listEncounters.isEmpty() -> {
        binding.txtViewNoLastSeenServices.visibility = View.VISIBLE
        binding.lastSeenListView.visibility = View.GONE
      }
      else -> {
        binding.txtViewNoLastSeenServices.visibility = View.GONE
        binding.lastSeenListView.visibility = View.VISIBLE
        populateLastSeenList(listEncounters)
      }
    }
  }

  private fun setupViews() {
    binding.carePlanListView.apply {
      adapter = carePlanAdapter
      layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
    }

    binding.upcomingServicesListView.apply {
      adapter = upcomingServicesAdapter
      layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
    }

    binding.lastSeenListView.apply {
      adapter = lastSeen
      layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
    }
  }

  companion object {
    fun newInstance(bundle: Bundle = Bundle()) = AncDetailsFragment().apply { arguments = bundle }
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
        populateImmunizationList(immunizations)
      }
    }
  }

  private fun populateImmunizationList(listCarePlan: List<CarePlanItem>) {
    carePlanAdapter.submitList(listCarePlan)
  }

  private fun populateUpcomingServicesList(upcomingServiceItem: List<UpcomingServiceItem>) {
    upcomingServicesAdapter.submitList(upcomingServiceItem)
  }
  private fun populateLastSeenList(upcomingServiceItem: List<EncounterItem>) {
    lastSeen.submitList(upcomingServiceItem)
  }
}
