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

package org.smartregister.fhircore.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.google.android.fhir.FhirEngine
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import org.smartregister.fhircore.FhirApplication
import org.smartregister.fhircore.PatientListViewModel
import org.smartregister.fhircore.PatientListViewModelFactory
import org.smartregister.fhircore.R
import org.smartregister.fhircore.adapter.PatientItemRecyclerViewAdapter
import org.smartregister.fhircore.fragment.PatientDetailFragment

/** An activity representing a list of Patients. */
class PatientListActivity : AppCompatActivity() {

    private lateinit var fhirEngine: FhirEngine
    private lateinit var patientListViewModel: PatientListViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("PatientListActivity", "onCreate() called")
        setContentView(R.layout.activity_patient_list)

        val toolbar = findViewById<Toolbar>(R.id.patient_register_toolbar)
        setSupportActionBar(toolbar)
        toolbar.title = title

        fhirEngine = FhirApplication.fhirEngine(this)

        patientListViewModel = ViewModelProvider(this, PatientListViewModelFactory(
                this.application, fhirEngine
        )).get(PatientListViewModel::class.java)
        val recyclerView: RecyclerView = findViewById(R.id.patient_list)

        val adapter = PatientItemRecyclerViewAdapter(this::onPatientItemClicked)
        recyclerView.adapter = adapter

        patientListViewModel.getSearchedPatients().observe(this,
                {
                    Log.d("PatientListActivity", "Submitting ${it.count()} patient records")
                    adapter.submitList(it)
                }
        )

        setUpViews();
    }

    private fun setUpViews() {
        findViewById<Button>(R.id.btn_register_new_patient).setOnClickListener {
            Snackbar.make(it, "Add Patient", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
            addPatient(it)
            true
        }
        setupDrawerContent();
    }

    private fun setupDrawerContent() {
        val drawerLayout = findViewById<DrawerLayout>(R.id.drawer_layout);
        findViewById<ImageButton>(R.id.btn_drawer_menu).setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }
        val navigationView = findViewById<NavigationView>(R.id.nav_view)
    }

    // Click handler to help display the details about the patients from the list.
    private fun onPatientItemClicked(patientItem: PatientListViewModel.PatientItem) {
        val intent = Intent(this.applicationContext,
                PatientDetailActivity::class.java).apply {
            putExtra(PatientDetailFragment.ARG_ITEM_ID, patientItem.id)
        }
        this.startActivity(intent)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val view: View = findViewById(R.id.app_bar)

        // Handle item selection
        return when (item.itemId) {
            R.id.sync_resources -> {
                syncResources(view)
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
