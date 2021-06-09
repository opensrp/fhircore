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

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.google.android.fhir.FhirEngine
import org.smartregister.fhircore.FhirApplication
import org.smartregister.fhircore.R
import org.smartregister.fhircore.activity.PatientDetailActivity
import org.smartregister.fhircore.activity.QuestionnaireActivity
import org.smartregister.fhircore.activity.RecordVaccineActivity
import org.smartregister.fhircore.activity.USER_ID
import org.smartregister.fhircore.adapter.PatientItemRecyclerViewAdapter
import org.smartregister.fhircore.viewmodel.PatientListViewModel
import org.smartregister.fhircore.viewmodel.PatientListViewModelFactory
import timber.log.Timber

class PatientListFragment : Fragment() {

  private lateinit var patientListViewModel: PatientListViewModel
  private lateinit var fhirEngine: FhirEngine
  private var search: String? = null
  private val pageCount: Int = 6

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    return inflater.inflate(R.layout.fragment_patient_list, container, false)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    fhirEngine = FhirApplication.fhirEngine(requireContext())

    val recyclerView = view.findViewById<RecyclerView>(R.id.patient_list)
    val adapter =
      PatientItemRecyclerViewAdapter(
        this::onPatientItemClicked,
        this::onNavigationClicked,
        this::onRecordVaccineClicked
      )
    recyclerView.adapter = adapter

    requireActivity().findViewById<TextView>(R.id.tv_sync).setOnClickListener {
      requireActivity()
        .findViewById<DrawerLayout>(R.id.drawer_layout)
        .closeDrawer(GravityCompat.START)
      syncResources()
    }

    patientListViewModel =
      ViewModelProvider(
          this,
          PatientListViewModelFactory(requireActivity().application, fhirEngine)
        )
        .get(PatientListViewModel::class.java)

    patientListViewModel.liveSearchedPaginatedPatients.observe(
      requireActivity(),
      {
        Timber.d("Submitting ${it.first.count()} patient records")
        val list = ArrayList<Any>(it.first)
        list.add(it.second)
        adapter.submitList(list)
        adapter.notifyDataSetChanged()

        if (it.first.count() == 0) {
          showEmptyListViews()
        } else {
          hideEmptyListViews()
        }
      }
    )

    requireActivity()
      .findViewById<EditText>(R.id.edit_text_search)
      .addTextChangedListener(
        object : TextWatcher {
          override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

          override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            search = s?.toString()
            patientListViewModel.searchResults(s?.toString(), 0, pageCount)
          }

          override fun afterTextChanged(s: Editable?) {}
        }
      )

    patientListViewModel.searchResults(page = 0, pageSize = pageCount)
    super.onViewCreated(view, savedInstanceState)
  }

  private fun hideEmptyListViews() {
    setVisibility(R.id.empty_list_message_container, View.INVISIBLE)
    setRegisterButtonAlignment(RelativeLayout.ALIGN_PARENT_BOTTOM)
  }

  private fun showEmptyListViews() {
    setVisibility(R.id.empty_list_message_container, View.VISIBLE)
    setRegisterButtonAlignment(RelativeLayout.BELOW)
  }

  private fun setVisibility(id: Int, visibility: Int) {
    requireActivity().findViewById<View>(id).visibility = visibility
  }

  private fun setRegisterButtonAlignment(alignment: Int) {
    val button = requireActivity().findViewById<Button>(R.id.btn_register_new_patient)
    val params = button.layoutParams as RelativeLayout.LayoutParams

    if (alignment == RelativeLayout.BELOW) {
      params.removeRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
      params.addRule(RelativeLayout.BELOW, R.id.empty_list_message_container)
    } else {
      params.removeRule(RelativeLayout.BELOW)
      params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE)
    }

    button.layoutParams = params
  }

  // Click handler to help display the details about the patients from the list.
  private fun onNavigationClicked(direction: NavigationDirection, currentPage: Int) {
    val nextPage = currentPage + if (direction == NavigationDirection.NEXT) 1 else -1
    patientListViewModel.searchResults(search, nextPage, pageCount)
  }

  // Click handler to help display the details about the patients from the list.
  private fun onPatientItemClicked(patientItem: PatientListViewModel.PatientItem) {
    val intent =
      Intent(requireContext(), PatientDetailActivity::class.java).apply {
        putExtra(PatientDetailFragment.ARG_ITEM_ID, patientItem.logicalId)
      }
    this.startActivity(intent)
  }

  private fun onRecordVaccineClicked(patientItem: PatientListViewModel.PatientItem) {
    startActivity(
      Intent(requireContext(), RecordVaccineActivity::class.java).apply {
        putExtra(QuestionnaireActivity.QUESTIONNAIRE_TITLE_KEY, "Record Vaccine")
        putExtra(QuestionnaireActivity.QUESTIONNAIRE_FILE_PATH_KEY, "record-vaccine.json")
        putExtra(USER_ID, patientItem.logicalId)
      }
    )
  }

  private fun syncResources() {
    patientListViewModel.searchResults()
    Toast.makeText(requireContext(), "Syncing...", Toast.LENGTH_LONG).show()
    patientListViewModel.syncUpload()
  }
}
