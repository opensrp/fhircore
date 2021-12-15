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
import androidx.core.content.ContextCompat
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
import org.smartregister.fhircore.anc.ui.anccare.shared.AncItemMapper
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity
import org.smartregister.fhircore.engine.util.extension.hide
import org.smartregister.fhircore.engine.util.extension.show

@AndroidEntryPoint
class AncDetailsFragment : Fragment() {

  @Inject lateinit var carePlanAdapter: CarePlanAdapter

  @Inject lateinit var upcomingServicesAdapter: UpcomingServicesAdapter

  @Inject lateinit var encounterAdapter: EncounterAdapter

  val ancDetailsViewModel by viewModels<AncDetailsViewModel>()

  lateinit var patientId: String

  lateinit var viewBinding: FragmentAncDetailsBinding

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    viewBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_anc_details, container, false)
    return viewBinding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    patientId = arguments?.getString(QuestionnaireActivity.QUESTIONNAIRE_ARG_PATIENT_KEY) ?: ""

    setupViews()

    ancDetailsViewModel.patientRepository.setAncItemMapperType(
      AncItemMapper.AncItemMapperType.DETAILS
    )

    ancDetailsViewModel.run {
      val detailsFragment = this@AncDetailsFragment
      fetchObservation(patientId).observe(viewLifecycleOwner, detailsFragment::handleObservation)
      fetchUpcomingServices(patientId)
        .observe(viewLifecycleOwner, detailsFragment::handleUpcomingServices)
      fetchCarePlan(patientId).observe(viewLifecycleOwner, detailsFragment::handleCarePlan)
      fetchLastSeen(patientId).observe(viewLifecycleOwner, detailsFragment::handleLastSeen)
    }
  }

  private fun handleObservation(ancOverviewItem: AncOverviewItem) {
    viewBinding.apply {
      txtViewEDDDoseDate.text = ancOverviewItem.edd
      txtViewGAPeriod.text = ancOverviewItem.ga
      txtViewFetusesCount.text = ancOverviewItem.noOfFetuses
      txtViewRiskValue.text = ancOverviewItem.risk
    }
  }

  private fun handleUpcomingServices(listEncounters: List<UpcomingServiceItem>) {
    when {
      listEncounters.isEmpty() -> {
        viewBinding.apply {
          txtViewNoUpcomingServices.show()
          upcomingServicesListView.hide()
          txtViewUpcomingServicesSeeAllHeading.hide()
          imageViewUpcomingServicesSeeAllArrow.hide()
        }
      }
      else -> {
        viewBinding.apply {
          txtViewNoUpcomingServices.hide()
          upcomingServicesListView.show()
          txtViewUpcomingServicesSeeAllHeading.show()
          imageViewUpcomingServicesSeeAllArrow.show()
        }
        upcomingServicesAdapter.submitList(listEncounters)
      }
    }
  }

  private fun handleLastSeen(listEncounters: List<EncounterItem>) {
    when {
      listEncounters.isEmpty() -> {
        viewBinding.apply {
          txtViewNoLastSeenServices.show()
          lastSeenListView.hide()
        }
      }
      else -> {
        viewBinding.apply {
          txtViewNoLastSeenServices.hide()
          lastSeenListView.show()
        }
        encounterAdapter.submitList(listEncounters)
      }
    }
  }

  private fun setupViews() {
    viewBinding.upcomingServicesListView.apply {
      adapter = upcomingServicesAdapter
      layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
    }

    viewBinding.lastSeenListView.apply {
      adapter = encounterAdapter
      layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
    }
  }

  private fun handleCarePlan(immunizations: List<CarePlanItem>) {
    when {
      immunizations.isEmpty() -> {
        viewBinding.apply {
          txtViewNoCarePlan.show()
          txtViewCarePlanSeeAllHeading.hide()
          imageViewSeeAllArrow.hide()
          txtViewCarePlan.hide()
        }
      }
      else -> {
        viewBinding.apply {
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
      viewBinding.apply {
        txtViewCarePlan.text = getString(R.string.anc_record_visit_with_overdue, countOverdue)
        txtViewCarePlan.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_red))
      }
    } else if (countDue > 0) {
      viewBinding.apply {
        txtViewCarePlan.text = getString(R.string.anc_record_visit)
        txtViewCarePlan.setTextColor(
          ContextCompat.getColor(requireContext(), R.color.colorPrimaryLight)
        )
      }
    }
  }

  companion object {
    const val TAG = "AncDetailsFragment"
    fun newInstance(bundle: Bundle = Bundle()) = AncDetailsFragment().apply { arguments = bundle }
  }
}
