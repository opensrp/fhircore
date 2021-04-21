/*
 * Copyright 2021 Ona Systems Inc
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

package org.smartregister.fhircore.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.google.android.fhir.FhirEngine
import org.smartregister.fhircore.FhirApplication
import org.smartregister.fhircore.R
import org.smartregister.fhircore.adapter.ObservationItemRecyclerViewAdapter
import org.smartregister.fhircore.util.Utils
import org.smartregister.fhircore.viewmodel.PatientListViewModel
import org.smartregister.fhircore.viewmodel.PatientListViewModelFactory

/**
 * A fragment representing a single Patient detail screen. This fragment is contained in a
 * [PatientDetailActivity].
 */
class PatientDetailFragment : Fragment() {

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    val rootView = inflater.inflate(R.layout.patient_detail, container, false)

    val recyclerView: RecyclerView = rootView.findViewById(R.id.observation_list)
    val adapter = ObservationItemRecyclerViewAdapter()
    recyclerView.adapter = adapter

    val fhirEngine: FhirEngine = FhirApplication.fhirEngine(requireContext())
    var patient: PatientListViewModel.PatientItem? = null

    val viewModel: PatientListViewModel =
      ViewModelProvider(
          this,
          PatientListViewModelFactory(this.requireActivity().application, fhirEngine)
        )
        .get(PatientListViewModel::class.java)

    viewModel
      .getObservations()
      .observe(
        viewLifecycleOwner,
        Observer<List<PatientListViewModel.ObservationItem>> { adapter.submitList(it) }
      )

    arguments?.let {
      if (it.containsKey(ARG_ITEM_ID)) {
        it.getString(ARG_ITEM_ID)?.let { it1 -> observePatientList(viewModel, it1) }
      }
    }

    viewModel.getSearchResults()

    return rootView
  }

  //Workaround till search by id is implemented
  private fun observePatientList(viewModel: PatientListViewModel, itemId: String) {
    viewModel.liveSearchedPatients.observe(
      viewLifecycleOwner,
      Observer<List<PatientListViewModel.PatientItem>> {
        setupPatientData(it.associateBy { it.id }[itemId])
      }
    )
  }

  private fun setupPatientData(patient: PatientListViewModel.PatientItem?) {
    if (patient != null) {
      var patientDetailLabel =
        patient?.name +
          ", " +
          patient?.gender +
          ", " +
          patient?.dob?.let { it1 -> Utils.getAgeFromDate(it1) }
      activity?.findViewById<TextView>(R.id.patient_bio_data)?.text = patientDetailLabel
      activity?.findViewById<TextView>(R.id.id_patient_number)?.text = "ID: " + patient.id
    }
  }

  companion object {
    /** The fragment argument representing the patient item ID that this fragment represents. */
    const val ARG_ITEM_ID = "patient_item_id"
  }
}
