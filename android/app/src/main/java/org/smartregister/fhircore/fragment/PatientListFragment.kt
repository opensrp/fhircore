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

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
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
import org.smartregister.fhircore.activity.PatientDetailActivity
import org.smartregister.fhircore.activity.QuestionnaireActivity
import org.smartregister.fhircore.activity.RecordVaccineActivity
import org.smartregister.fhircore.activity.USER_ID
import org.smartregister.fhircore.adapter.PatientItemRecyclerViewAdapter
import org.smartregister.fhircore.viewmodel.PatientListViewModel
import org.smartregister.fhircore.viewmodel.PatientListViewModelFactory
import timber.log.Timber

class PatientListFragment : Fragment(), OnPatientSearchResult {

  private lateinit var patientListViewModel: PatientListViewModel
  private lateinit var fhirEngine: FhirEngine
    private var liveBarcodeScanningFragment: LiveBarcodeScanningFragment? = null
  private var search: String? = null
  private val pageCount: Int = 7


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
      setUpBarcodeScanner()
    super.onViewCreated(view, savedInstanceState)
  }

    override fun onResume() {
        patientListViewModel.searchResults(page = 0, pageSize = pageCount) // TODO: might need to move this to happen when a user clicks a button
        super.onResume()
    }

    private fun setUpBarcodeScanner() {
        val btnScanBarcode: View = requireActivity().findViewById(R.id.layout_scan_barcode)
        requireActivity().supportFragmentManager.setFragmentResultListener(
                "result",
                this,
                { _, result ->
                    val barcode = result.getString("result")!!.trim()
                    patientListViewModel.isPatientExists(barcode, this)
                }
        )

        val requestPermissionLauncher = getBarcodePermissionLauncher()
        btnScanBarcode.setOnClickListener {
            launchBarcodeReader(requestPermissionLauncher)
        }
    }

    private fun getBarcodeScannerInstance(): LiveBarcodeScanningFragment {
        if (liveBarcodeScanningFragment == null) {
            liveBarcodeScanningFragment = LiveBarcodeScanningFragment()
        }
        return liveBarcodeScanningFragment!!
    }

    private fun getBarcodePermissionLauncher(): ActivityResultLauncher<String> {
        return registerForActivityResult(ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                getBarcodeScannerInstance().show(requireActivity().supportFragmentManager, "TAG")
            } else {
                Toast.makeText(requireContext(), "Camera permissions are needed to launch barcode reader!", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun launchBarcodeReader(requestPermissionLauncher: ActivityResultLauncher<String>) {
        if (ContextCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED) {
            getBarcodeScannerInstance().show(requireActivity().supportFragmentManager, "TAG")
        } else  {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

  // Click handler to help display the details about the patients from the list.
  private fun onNavigationClicked(direction: NavigationDirection, currentPage: Int) {
    val nextPage = currentPage + if (direction == NavigationDirection.NEXT) 1 else -1
    patientListViewModel.searchResults(search, nextPage, pageCount)
  }

  // Click handler to help display the details about the patients from the list.
  private fun onPatientItemClicked(patientItem: PatientListViewModel.PatientItem) {
    launchPatientDetailActivity(patientItem.logicalId)
  }

    private fun launchPatientDetailActivity(patientLogicalId: String) {
        val intent =
                Intent(requireContext(), PatientDetailActivity::class.java).apply {
                    putExtra(PatientDetailFragment.ARG_ITEM_ID, patientLogicalId)
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

    override fun onSearchDone(isPatientFound: Boolean, patientLogicalId: String) {
        if (isPatientFound) {
            launchPatientDetailActivity(patientLogicalId)
        } else {
            patientListViewModel.clearPatientList()
        }
        getBarcodeScannerInstance().onDestroy()
    }
}
