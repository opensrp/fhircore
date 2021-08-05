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

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
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
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.google.android.fhir.FhirEngine
import com.google.android.material.switchmaterial.SwitchMaterial
import org.smartregister.fhircore.FhirApplication
import org.smartregister.fhircore.R
import org.smartregister.fhircore.activity.PATIENT_ID
import org.smartregister.fhircore.activity.PatientDetailActivity
import org.smartregister.fhircore.activity.PatientListActivity
import org.smartregister.fhircore.activity.QuestionnaireActivity
import org.smartregister.fhircore.activity.RecordVaccineActivity
import org.smartregister.fhircore.adapter.PatientItemRecyclerViewAdapter
import org.smartregister.fhircore.domain.Pagination
import org.smartregister.fhircore.domain.currentPageNumber
import org.smartregister.fhircore.domain.hasNextPage
import org.smartregister.fhircore.domain.hasPreviousPage
import org.smartregister.fhircore.domain.totalPages
import org.smartregister.fhircore.model.PatientItem
import org.smartregister.fhircore.viewmodel.PatientListViewModel
import org.smartregister.fhircore.viewmodel.PatientListViewModelFactory
import timber.log.Timber

const val PAGE_COUNT = 7

class PatientListFragment : Fragment() {

  internal lateinit var patientListViewModel: PatientListViewModel
  private lateinit var fhirEngine: FhirEngine
  //  internal val liveBarcodeScanningFragment by lazy { LiveBarcodeScanningFragment() }
  private var search: String? = null
  private lateinit var adapter: PatientItemRecyclerViewAdapter
  private lateinit var paginationView: RelativeLayout
  private lateinit var recyclerView: RecyclerView
  private lateinit var nextButton: Button
  private lateinit var prevButton: Button
  private lateinit var infoTextView: TextView
  private var activePageNum = 0

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    return inflater.inflate(R.layout.fragment_patient_list, container, false)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    recyclerView = view.findViewById<RecyclerView>(R.id.patient_list)
    adapter = PatientItemRecyclerViewAdapter(this::onPatientItemClicked)
    paginationView = view.findViewById(R.id.rl_pagination)
    nextButton = view.findViewById(R.id.btn_next_page)
    prevButton = view.findViewById(R.id.btn_previous_page)
    infoTextView = view.findViewById(R.id.txt_page_info)

    recyclerView.adapter = adapter

    requireActivity().findViewById<TextView>(R.id.tv_sync).setOnClickListener {
      requireActivity()
        .findViewById<DrawerLayout>(R.id.drawer_layout)
        .closeDrawer(GravityCompat.START)
      syncResources()
    }

    patientListViewModel.liveSearchedPaginatedPatients.observe(requireActivity(), { setData(it) })

    requireActivity()
      .findViewById<EditText>(R.id.edit_text_search)
      .addTextChangedListener(
        object : TextWatcher {
          override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

          override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            search = s?.toString()
            patientListViewModel.searchResults(s?.toString(), 0, PAGE_COUNT)
          }

          override fun afterTextChanged(s: Editable?) {}
        }
      )

    requireActivity()
      .findViewById<SwitchMaterial>(R.id.btn_show_overdue_patients)
      .setOnCheckedChangeListener { button, isChecked ->
        patientListViewModel.showOverduePatientsOnly.value = isChecked
      }

    patientListViewModel.showOverduePatientsOnly.observe(
      requireActivity(),
      {
        if (patientListViewModel.loadingListObservable.value!! != -1) {
          patientListViewModel.searchResults(
            requireActivity().findViewById<EditText>(R.id.edit_text_search).text.toString(),
            0,
            PAGE_COUNT
          )
        }
      }
    )

    patientListViewModel.loadingListObservable.observe(
      requireActivity(),
      {
        if (it != -1) {
          requireActivity().findViewById<ConstraintLayout>(R.id.loader_overlay).visibility =
            if (it == 1) View.VISIBLE else View.GONE
        }
      }
    )

    syncResources()
    super.onViewCreated(view, savedInstanceState)
  }

  override fun onResume() {
    if (patientListViewModel.loadingListObservable.value!! == 0) {
      patientListViewModel.searchResults(
        requireActivity().findViewById<EditText>(R.id.edit_text_search).text.toString(),
        page = activePageNum,
        pageSize = PAGE_COUNT
      )
    }
    adapter.notifyDataSetChanged()
    super.onResume()
  }

  fun hideEmptyListViews() {
    setVisibility(R.id.empty_list_message_container, View.GONE)
  }

  fun showEmptyListViews() {
    setVisibility(R.id.empty_list_message_container, View.VISIBLE)
  }

  private fun setVisibility(id: Int, visibility: Int) {
    requireActivity().findViewById<View>(id).visibility = visibility
  }

  // Click handler to help display the details about the patients from the list.
  private fun onNavigationClicked(direction: NavigationDirection, currentPage: Int) {
    val nextPage = currentPage + if (direction == NavigationDirection.NEXT) 1 else -1
    patientListViewModel.searchResults(search, nextPage, PAGE_COUNT)
  }

  // Click handler to help display the details about the patients from the list.
  fun onPatientItemClicked(intention: Intention, patientItem: PatientItem) {
    when (intention) {
      Intention.RECORD_VACCINE -> {
        startActivity(
          Intent(requireContext(), RecordVaccineActivity::class.java).apply {
            putExtra(
              QuestionnaireActivity.QUESTIONNAIRE_TITLE_KEY,
              activity?.getString(R.string.record_vaccine)
            )
            putExtra(QuestionnaireActivity.QUESTIONNAIRE_PATH_KEY, recordVaccineQuestionnaireId)
            putExtra(PATIENT_ID, patientItem.logicalId)
          }
        )
      }
      Intention.VIEW -> {
        this.startActivity(
          Intent(requireContext(), PatientDetailActivity::class.java).apply {
            putExtra(PatientDetailFragment.ARG_ITEM_ID, patientItem.logicalId)
          }
        )
      }
    }
  }

  private fun syncResources() {
    patientListViewModel.runSync()
    Toast.makeText(requireContext(), R.string.syncing, Toast.LENGTH_LONG).show()
  }

  enum class Intention {
    RECORD_VACCINE,
    VIEW
  }

  private fun updatePagination(pagination: Pagination) {
    activePageNum = pagination.currentPage
    nextButton.setOnClickListener {
      onNavigationClicked(NavigationDirection.NEXT, pagination.currentPage)
    }
    prevButton.setOnClickListener {
      onNavigationClicked(NavigationDirection.PREVIOUS, pagination.currentPage)
    }

    nextButton.visibility = if (pagination.hasNextPage()) View.GONE else View.VISIBLE
    prevButton.visibility = if (pagination.hasPreviousPage()) View.GONE else View.VISIBLE
    paginationView.visibility =
      if (nextButton.visibility == View.VISIBLE || prevButton.visibility == View.VISIBLE)
        View.VISIBLE
      else View.GONE

    this.infoTextView.text =
      if (pagination.totalPages() < 2) ""
      else
        resources.getString(
          R.string.str_page_info,
          pagination.currentPageNumber(),
          pagination.totalPages()
        )
  }

  fun setData(data: Pair<List<PatientItem>, Pagination>) {
    Timber.d("Submitting ${data.first.count()} patient records")
    val list = ArrayList<PatientItem>(data.first)
    updatePagination(data.second)
    adapter.submitList(list)

    if (data.first.count() == 0) {
      showEmptyListViews()
    } else {
      hideEmptyListViews()
    }
  }
}
