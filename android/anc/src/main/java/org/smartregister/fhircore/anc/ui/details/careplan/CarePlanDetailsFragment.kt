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
import javax.inject.Inject
import org.smartregister.fhircore.anc.R
import org.smartregister.fhircore.anc.data.model.CarePlanItem
import org.smartregister.fhircore.anc.data.model.UpcomingServiceItem
import org.smartregister.fhircore.anc.databinding.FragmentNonAncDetailsBinding
import org.smartregister.fhircore.anc.ui.details.adapter.CarePlanAdapter
import org.smartregister.fhircore.anc.ui.details.adapter.UpcomingServicesAdapter
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity

@AndroidEntryPoint
class CarePlanDetailsFragment : Fragment() {

  @Inject lateinit var carePlanAdapter: CarePlanAdapter

  @Inject lateinit var upcomingServicesAdapter: UpcomingServicesAdapter

  val ancDetailsViewModel by viewModels<CarePlanDetailsViewModel>()

  private lateinit var patientId: String

  lateinit var viewBinding: FragmentNonAncDetailsBinding

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    viewBinding =
      DataBindingUtil.inflate(inflater, R.layout.fragment_non_anc_details, container, false)
    return viewBinding.root
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
        viewBinding.txtViewNoUpcomingServices.visibility = View.VISIBLE
        viewBinding.upcomingServicesListView.visibility = View.GONE
        viewBinding.txtViewUpcomingServicesSeeAllHeading.visibility = View.GONE
        viewBinding.imageViewUpcomingServicesSeeAllArrow.visibility = View.GONE
      }
      else -> {
        viewBinding.txtViewNoUpcomingServices.visibility = View.GONE
        viewBinding.upcomingServicesListView.visibility = View.VISIBLE
        viewBinding.txtViewUpcomingServicesSeeAllHeading.visibility = View.VISIBLE
        viewBinding.txtViewUpcomingServicesSeeAllHeading.visibility = View.VISIBLE
        populateUpcomingServicesList(listEncounters)
      }
    }
  }

  private fun setupViews() {
    viewBinding.carePlanListView.apply {
      adapter = carePlanAdapter
      layoutManager = LinearLayoutManager(requireContext())
    }
  }

  private fun handleCarePlan(immunizations: List<CarePlanItem>) {
    when {
      immunizations.isEmpty() -> {
        viewBinding.txtViewNoCarePlan.visibility = View.VISIBLE
        viewBinding.txtViewCarePlanSeeAllHeading.visibility = View.GONE
        viewBinding.imageViewSeeAllArrow.visibility = View.GONE
        viewBinding.carePlanListView.visibility = View.GONE
      }
      else -> {
        viewBinding.txtViewNoCarePlan.visibility = View.GONE
        viewBinding.txtViewCarePlanSeeAllHeading.visibility = View.VISIBLE
        viewBinding.imageViewSeeAllArrow.visibility = View.VISIBLE
        viewBinding.carePlanListView.visibility = View.VISIBLE
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

  companion object {
    fun newInstance(bundle: Bundle = Bundle()) =
      CarePlanDetailsFragment().apply { arguments = bundle }
    const val TAG = "CarePlanDetailsFragment"
  }
}
