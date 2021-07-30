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

package org.smartregister.fhircore.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.RecyclerView
import org.smartregister.fhircore.R
import org.smartregister.fhircore.adapter.PatientDetailsCardRecyclerViewAdapter
import org.smartregister.fhircore.model.CovaxDetailView
import org.smartregister.fhircore.model.PatientItem
import org.smartregister.fhircore.util.Utils
import org.smartregister.fhircore.viewmodel.CovaxListViewModel

/**
 * A fragment representing a single Patient detail screen. This fragment is contained in a
 * [PatientDetailActivity].
 */
class CovaxDetailFragment : Fragment() {

  val viewModel by activityViewModels<CovaxListViewModel>()
  lateinit var patientId: String
  lateinit var adapter: PatientDetailsCardRecyclerViewAdapter

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    // patient id must be supplied
    patientId = arguments?.getString(CovaxDetailView.COVAX_ARG_ITEM_ID)!!

    val rootView = inflater.inflate(R.layout.patient_detail, container, false)
    adapter = PatientDetailsCardRecyclerViewAdapter()

    val recyclerView: RecyclerView = rootView.findViewById(R.id.observation_list)
    recyclerView.adapter = adapter

    // bind profile data
    loadProfile()

    return rootView
  }

  private fun loadProfile() {
    // bind patient summary
    viewModel.getPatientItem(patientId).observe(viewLifecycleOwner, { setupPatientData(it) })

    // bind patient details
    viewModel
      .fetchPatientDetailsCards(requireContext(), patientId)
      .observe(
        viewLifecycleOwner,
        {
          if (it.size < 3) {
            activity?.findViewById<Button>(R.id.btn_record_vaccine)?.visibility = View.VISIBLE
          }
          adapter.submitList(it)
        }
      )
  }

  override fun onResume() {
    super.onResume()
    loadProfile()
  }

  private fun setupPatientData(patientItem: PatientItem?) {
    if (patientItem != null) {
      val (age, gender) = Utils.getPatientAgeGender(patientItem)
      val patientDetailLabel =
        patientItem.name + ", " + gender + ", " + patientItem.dob.let { dobString -> age }
      activity?.findViewById<TextView>(R.id.patient_bio_data)?.text = patientDetailLabel
      activity?.findViewById<TextView>(R.id.id_patient_number)?.text =
        "ID: " + patientItem.logicalId
      activity?.findViewById<TextView>(R.id.risk_flag)?.text = patientItem.risk
      activity?.findViewById<TextView>(R.id.risk_flag)?.visibility =
        if (patientItem.risk.isNullOrEmpty()) View.INVISIBLE else View.VISIBLE
    }
  }
}
