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
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import org.smartregister.fhircore.anc.R
import org.smartregister.fhircore.anc.data.model.AncOverviewItem
import org.smartregister.fhircore.anc.data.model.CarePlanItem
import org.smartregister.fhircore.anc.data.model.EncounterItem
import org.smartregister.fhircore.anc.data.model.UpcomingServiceItem
import org.smartregister.fhircore.anc.databinding.FragmentAncDetailsBinding
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity
import org.smartregister.fhircore.engine.util.extension.hide
import org.smartregister.fhircore.engine.util.extension.show
import timber.log.Timber

@AndroidEntryPoint
class AncDetailsFragment : Fragment() {

  @Inject lateinit var ancPatientItemMapper: AncPatientItemMapper

  lateinit var patientId: String

  val ancDetailsViewModel by viewModels<AncDetailsViewModel>()

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

    // Set the patient id and correct DomainMapper to use
    ancDetailsViewModel.patientRepository.domainMapperInUse = ancPatientItemMapper
    ancDetailsViewModel.patientId = patientId

    Timber.d(patientId)

    ancDetailsViewModel.run {
      val detailsFragment = this@AncDetailsFragment
      fetchCarePlan().observe(viewLifecycleOwner, detailsFragment::handleCarePlan)
      fetchObservation().observe(viewLifecycleOwner, detailsFragment::handleObservation)
      fetchUpcomingServices().observe(viewLifecycleOwner, detailsFragment::handleUpcomingServices)
      fetchLastSeen().observe(viewLifecycleOwner, detailsFragment::handleLastSeen)
    }
  }

  private fun handleObservation(ancOverviewItem: AncOverviewItem) {
    binding.apply {
      txtViewEDDDoseDate.text = ancOverviewItem.edd
      txtViewGAPeriod.text = ancOverviewItem.ga
      txtViewFetusesCount.text = ancOverviewItem.noOfFetuses
      txtViewRiskValue.text = ancOverviewItem.risk
    }
  }

  private fun handleUpcomingServices(listEncounters: List<UpcomingServiceItem>) {
    when {
      listEncounters.isEmpty() -> {
        binding.apply {
          txtViewNoUpcomingServices.show()
          upcomingServicesListView.hide()
          txtViewUpcomingServicesSeeAllHeading.hide()
          imageViewUpcomingServicesSeeAllArrow.hide()
        }
      }
      else -> {
        binding.apply {
          txtViewNoUpcomingServices.hide()
          upcomingServicesListView.show()
          txtViewUpcomingServicesSeeAllHeading.show()
          txtViewUpcomingServicesSeeAllHeading.show()
        }
        upcomingServicesAdapter.submitList(listEncounters)
      }
    }
  }

  private fun handleLastSeen(listEncounters: List<EncounterItem>) {
    when {
      listEncounters.isEmpty() -> {
        binding.txtViewNoLastSeenServices.show()
        binding.lastSeenListView.hide()
      }
      else -> {
        binding.txtViewNoLastSeenServices.hide()
        binding.lastSeenListView.show()
        lastSeen.submitList(listEncounters)
      }
    }
  }

  private fun setupViews() {

    binding.upcomingServicesListView.apply {
      adapter = upcomingServicesAdapter
      layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
    }

    binding.lastSeenListView.apply {
      adapter = lastSeen
      layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
    }
  }

  private fun handleCarePlan(immunizations: List<CarePlanItem>) {
    when {
      immunizations.isEmpty() -> {
        binding.apply {
          txtViewNoCarePlan.show()
          txtViewCarePlanSeeAllHeading.hide()
          imageViewSeeAllArrow.hide()
          txtViewCarePlan.hide()
        }
      }
      else -> {
        binding.apply {
          txtViewNoCarePlan.hide()
          txtViewCarePlanSeeAllHeading.show()
          imageViewSeeAllArrow.show()
          txtViewCarePlan.show()
        }

        populateImmunizationList(immunizations)
      }
    }
  }

  private fun populateImmunizationList(listCarePlan: List<CarePlanItem>) {
    val countOverdue = listCarePlan.filter { it.overdue }.size
    val countDue = listCarePlan.filter { it.due }.size
    if (countOverdue > 0) {
      binding.txtViewCarePlan.text =
        this.getString(R.string.anc_record_visit_button_title) +
          " $countOverdue " +
          this.getString(R.string.overdue)
      binding.txtViewCarePlan.setTextColor(resources.getColor(R.color.status_red))
    } else if (countDue > 0) {
      binding.txtViewCarePlan.text = this.getString(R.string.anc_record_visit_button_title)
      binding.txtViewCarePlan.setTextColor(resources.getColor(R.color.colorPrimaryLight))
    }
  }

  companion object {
    fun newInstance(bundle: Bundle = Bundle()) = AncDetailsFragment().apply { arguments = bundle }
  }
}
