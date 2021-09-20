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

package org.smartregister.fhircore.anc.ui.madx.details

import android.content.Intent
import android.os.Bundle
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.Menu
import android.view.MenuItem
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.google.android.fhir.FhirEngine
import com.google.android.material.tabs.TabLayoutMediator
import org.smartregister.fhircore.anc.AncApplication
import org.smartregister.fhircore.anc.R
import org.smartregister.fhircore.anc.data.NonAncPatientRepository
import org.smartregister.fhircore.anc.data.model.AncPatientDetailItem
import org.smartregister.fhircore.anc.databinding.ActivityNonAncDetailsBinding
import org.smartregister.fhircore.anc.ui.madx.details.form.NonAncDetailsFormConfig.ANC_VITAL_SIGNS_UNIT_OPTIONS
import org.smartregister.fhircore.anc.ui.madx.details.form.NonAncDetailsQuestionnaireActivity
import org.smartregister.fhircore.anc.ui.madx.details.nonanccareplan.CarePlanDetailsFragment
import org.smartregister.fhircore.engine.ui.base.BaseMultiLanguageActivity
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity
import org.smartregister.fhircore.engine.util.extension.createFactory

class NonAncDetailsActivity : BaseMultiLanguageActivity() {

    private lateinit var adapter: ViewPagerAdapter
    private lateinit var patientId: String

    private lateinit var fhirEngine: FhirEngine

    lateinit var ancDetailsViewModel: NonAncDetailsViewModel

    private lateinit var ancPatientRepository: NonAncPatientRepository

    private lateinit var activityAncDetailsBinding: ActivityNonAncDetailsBinding

    val details = arrayOf(
        "CARE PLAN",
        "DETAILS"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityAncDetailsBinding =
            DataBindingUtil.setContentView(this, R.layout.activity_non_anc_details)
        setSupportActionBar(activityAncDetailsBinding.patientDetailsToolbar)

        fhirEngine = AncApplication.getContext().fhirEngine


        if (savedInstanceState == null) {


            patientId =
                intent.extras?.getString(QuestionnaireActivity.QUESTIONNAIRE_ARG_PATIENT_KEY) ?: ""

            ancPatientRepository =
                NonAncPatientRepository(
                    (application as AncApplication).fhirEngine,
                    NonAncPatientItemMapper
                )

            ancDetailsViewModel =
                ViewModelProvider(
                    this,
                    NonAncDetailsViewModel(ancPatientRepository, patientId = patientId).createFactory()
                )[NonAncDetailsViewModel::class.java]

            activityAncDetailsBinding.txtViewPatientId.text = patientId

            ancDetailsViewModel
                .fetchDemographics()
                .observe(this@NonAncDetailsActivity, this::handlePatientDemographics)

             adapter = ViewPagerAdapter(
                supportFragmentManager, lifecycle, bundleOf(
                    Pair(
                        QuestionnaireActivity.QUESTIONNAIRE_ARG_PATIENT_KEY,
                        patientId
                    )
                )
            )
            activityAncDetailsBinding.pager.adapter = adapter

            TabLayoutMediator(
                activityAncDetailsBinding.tablayout,
                activityAncDetailsBinding.pager
            ) { tab, position ->
                tab.text = details[position]
            }.attach()

        }

        activityAncDetailsBinding.patientDetailsToolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.profile_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        val removeThisPerson = menu!!.findItem(R.id.remove_this_person)
        val markAsAncClient = menu!!.findItem(R.id.mark_as_anc_client)
        val addVitals = menu!!.findItem(R.id.add_vitals)
        val addConditions = menu!!.findItem(R.id.add_conditions)
        val viewPastEncounters = menu!!.findItem(R.id.view_past_encounters)
        val bmiWidget = menu!!.findItem(R.id.bmi_widget)

        viewPastEncounters.isVisible = true
        markAsAncClient.isVisible = false
        addVitals.isVisible = true
        addConditions.isVisible = false
        bmiWidget.isVisible = false

        val title = removeThisPerson.title.toString()
        val s = SpannableString(title)
        with(s) {
            setSpan(
                ForegroundColorSpan(android.graphics.Color.parseColor("#DD0000")),
                0,
                length,
                android.text.Spannable.SPAN_INCLUSIVE_INCLUSIVE
            )
        } // provide whatever color you want here.
        removeThisPerson.title = s
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.add_vitals) {
            startActivity(
                Intent(this, NonAncDetailsQuestionnaireActivity::class.java)
                    .putExtras(
                        QuestionnaireActivity.requiredIntentArgs(
                            clientIdentifier = patientId,
                            form = ANC_VITAL_SIGNS_UNIT_OPTIONS
                        )
                    )
            )
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun handlePatientDemographics(patient: AncPatientDetailItem) {
        with(patient) {
            val patientDetails =
                this.patientDetails.name +
                        ", " +
                        this.patientDetails.gender +
                        ", " +
                        this.patientDetails.age
            val patientId =
                this.patientDetailsHead.demographics + " ID: " + this.patientDetails.patientIdentifier
            activityAncDetailsBinding.txtViewPatientDetails.text = patientDetails
            activityAncDetailsBinding.txtViewPatientId.text = patientId
        }
    }

}
