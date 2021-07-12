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
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.google.android.fhir.FhirEngine
import com.google.mlkit.md.LiveBarcodeScanningFragment
import org.smartregister.fhircore.FhirApplication
import org.smartregister.fhircore.R
import org.smartregister.fhircore.activity.PATIENT_ID
import org.smartregister.fhircore.activity.PatientDetailActivity
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

class PatientListFragment : Fragment() {

  internal lateinit var patientListViewModel: PatientListViewModel
  private lateinit var fhirEngine: FhirEngine
  private val liveBarcodeScanningFragment by lazy { LiveBarcodeScanningFragment() }
  private var search: String? = null
  private val pageCount: Int = 7
  private lateinit var adapter: PatientItemRecyclerViewAdapter
  private lateinit var paginationView: RelativeLayout
  private lateinit var recyclerView: RecyclerView
  private lateinit var nextButton: Button
  private lateinit var prevButton: Button
  private lateinit var infoTextView: TextView

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    return inflater.inflate(R.layout.fragment_patient_list, container, false)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    fhirEngine = FhirApplication.fhirEngine(requireContext())

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

    patientListViewModel =
      ViewModelProvider(
          this,
          PatientListViewModelFactory(requireActivity().application, fhirEngine)
        )
        .get(PatientListViewModel::class.java)

    patientListViewModel.liveSearchedPaginatedPatients.observe(requireActivity(), { setData(it) })

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
    setUpBarcodeScanner()
    super.onViewCreated(view, savedInstanceState)
  }

  override fun onResume() {
    patientListViewModel.searchResults(page = 0, pageSize = pageCount)
    adapter.notifyDataSetChanged()
    super.onResume()
  }

  private fun setUpBarcodeScanner() {
    val btnScanBarcode: View = requireActivity().findViewById(R.id.layout_scan_barcode)
    requireActivity()
      .supportFragmentManager
      .setFragmentResultListener(
        "result",
        this,
        { _, result ->
          val barcode = result.getString("result")!!.trim()
          patientListViewModel
            .isPatientExists(barcode)
            .observe(
              viewLifecycleOwner,
              {
                if (it.isSuccess) {
                  launchPatientDetailActivity(barcode)
                } else {
                  patientListViewModel.clearPatientList()
                }
                liveBarcodeScanningFragment.onDestroy()
              }
            )
        }
      )

    val requestPermissionLauncher = getBarcodePermissionLauncher()
    btnScanBarcode.setOnClickListener { launchBarcodeReader(requestPermissionLauncher) }
  }

  private fun getBarcodePermissionLauncher(): ActivityResultLauncher<String> {
    return registerForActivityResult(ActivityResultContracts.RequestPermission()) {
      isGranted: Boolean ->
      if (isGranted) {
        liveBarcodeScanningFragment.show(requireActivity().supportFragmentManager, "TAG")
      } else {
        Toast.makeText(
            requireContext(),
            "Camera permissions are needed to launch barcode reader!",
            Toast.LENGTH_LONG
          )
          .show()
      }
    }
  }

  private fun launchBarcodeReader(requestPermissionLauncher: ActivityResultLauncher<String>) {
    if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) ==
        PackageManager.PERMISSION_GRANTED
    ) {
      liveBarcodeScanningFragment.show(requireActivity().supportFragmentManager, "TAG")
    } else {
      requestPermissionLauncher.launch(Manifest.permission.CAMERA)
    }
  }

  fun hideEmptyListViews() {
    setVisibility(R.id.empty_list_message_container, View.INVISIBLE)
    setRegisterButtonAlignment(RelativeLayout.ALIGN_PARENT_BOTTOM)
  }

  fun showEmptyListViews() {
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

  private fun launchPatientDetailActivity(patientLogicalId: String) {
    val intent =
      Intent(requireContext(), PatientDetailActivity::class.java).apply {
        putExtra(PatientDetailFragment.ARG_ITEM_ID, patientLogicalId)
      }
    this.startActivity(intent)
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
            putExtra(QuestionnaireActivity.QUESTIONNAIRE_FILE_PATH_KEY, "record-vaccine.json")
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
    patientListViewModel.searchResults(pageSize = pageCount)
    Toast.makeText(requireContext(), "Syncing...", Toast.LENGTH_LONG).show()
  }

  enum class Intention {
    RECORD_VACCINE,
    VIEW
  }

  private fun updatePagination(pagination: Pagination) {
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
