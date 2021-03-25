/*
 * Copyright 2020 Google LLC
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

package org.smartregister.fhircore

import android.annotation.SuppressLint
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.MenuItemCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ca.uhn.fhir.rest.param.ParamPrefixEnum
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.search.filter.FilterCriterion
import com.google.android.fhir.search.filter.string
import com.google.android.material.snackbar.Snackbar
import org.hl7.fhir.r4.model.Patient

/** An activity representing a list of Patients. */
class PatientListActivity : AppCompatActivity() {
    private lateinit var fhirEngine: FhirEngine
    private lateinit var patientListViewModel: PatientListViewModel
    // TODO : Generify this adapter
    private lateinit var adapter : PatientItemRecyclerViewAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("PatientListActivity", "onCreate() called")
        setContentView(R.layout.activity_patient_list)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.title = title

        fhirEngine = FhirApplication.fhirEngine(this)

        patientListViewModel = ViewModelProvider(this, PatientListViewModelFactory(
            this.application, fhirEngine
        )).get(PatientListViewModel::class.java)
        val recyclerView: RecyclerView = findViewById(R.id.patient_list)

        adapter = PatientItemRecyclerViewAdapter(this::onPatientItemClicked)
        recyclerView.adapter = adapter

        fetchUsers()
    }

    private fun fetchUsers(filterCriterion: FilterCriterion? = null){
        patientListViewModel.getSearchedPatients(filterCriterion).observe(this,
            {
                Log.d("PatientListActivity", "Submitting ${it.count()} patient records")
                adapter.submitList(it)
            }
        )
    }

    // Click handler to help display the details about the patients from the list.
    private fun onPatientItemClicked(patientItem: PatientListViewModel.PatientItem) {
        val intent = Intent(this.applicationContext,
            PatientDetailActivity::class.java).apply {
            putExtra(PatientDetailFragment.ARG_ITEM_ID, patientItem.id)
        }
        this.startActivity(intent)
    }

    // To suppress the warning. Seems to be an issue with androidx library.
    // "MenuBuilder.setOptionalIconsVisible can only be called from within the same library group
    // prefix (referenced groupId=androidx.appcompat with prefix androidx from groupId=fhir-engine"
    @SuppressLint("RestrictedApi")
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.list_options_menu, menu)
        // To ensure that icons show up in the overflow options menu. Icons go missing without this.
        if (menu is MenuBuilder) {
            menu.setOptionalIconsVisible(true)
        }
        menu?.let { bindSearchMenu(it) }
        return true
    }

    private fun bindSearchMenu(menu: Menu){
        val searchItem: MenuItem? = menu.findItem(R.id.action_search)
        if (searchItem != null) {
            val searchView = searchItem.actionView as SearchView
            searchView.setOnCloseListener { true }

            val searchPlate =
                searchView.findViewById(androidx.appcompat.R.id.search_src_text) as EditText
            searchPlate.hint = "Search"
            val searchPlateView: View =
                searchView.findViewById(androidx.appcompat.R.id.search_plate)
            searchPlateView.setBackgroundColor(
                ContextCompat.getColor(
                    this,
                    android.R.color.transparent
                )
            )

            searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    return false
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    fetchUsers(
                        if(newText.isNullOrBlank()) null else
                            string(Patient.GIVEN, ParamPrefixEnum.EQUAL, newText)
                                .or(string(Patient.FAMILY, ParamPrefixEnum.EQUAL, newText))
                    )
                    return false
                }
            })

            val searchManager =
                getSystemService(Context.SEARCH_SERVICE) as SearchManager
            searchView.setSearchableInfo(searchManager.getSearchableInfo(componentName))
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val view: View = findViewById(R.id.app_bar)

        // Handle item selection
        return when (item.itemId) {
            R.id.sync_resources -> {
                syncResources(view)
                true
            }
            R.id.add_patient -> {
                Snackbar.make(view, "Add Patient", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show()
                addPatient(view)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun syncResources(view: View) {
        Snackbar.make(view, "Getting Patients List", Snackbar.LENGTH_LONG)
            .setAction("Action", null).show()
        patientListViewModel.searchPatients()
    }

    private fun addPatient(view: View) {
        // TO DO: Open patient registration form
        val context = view.context
        context.startActivity(Intent(context, QuestionnaireActivity::class.java).apply {
            putExtra(
                    QuestionnaireActivity.QUESTIONNAIRE_TITLE_KEY,
                    "Patient registration"
            )
            putExtra(
                    QuestionnaireActivity.QUESTIONNAIRE_FILE_PATH_KEY,
                    "patient-registration.json"
            )
        })
    }
}
