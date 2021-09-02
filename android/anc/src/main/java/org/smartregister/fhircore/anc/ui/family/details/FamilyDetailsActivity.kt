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

package org.smartregister.fhircore.anc.ui.family.details

import android.os.Bundle
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.fhir.FhirEngine
import org.hl7.fhir.r4.model.Encounter
import org.hl7.fhir.r4.model.Patient
import org.smartregister.fhircore.anc.AncApplication
import org.smartregister.fhircore.anc.R
import org.smartregister.fhircore.anc.data.family.FamilyMemberRepository
import org.smartregister.fhircore.anc.data.family.model.FamilyMemberItem
import org.smartregister.fhircore.anc.ui.family.details.adapter.FamilyEncounterAdapter
import org.smartregister.fhircore.anc.ui.family.details.adapter.FamilyMemberAdapter
import org.smartregister.fhircore.engine.configuration.app.ConfigurableApplication
import org.smartregister.fhircore.engine.ui.base.BaseMultiLanguageActivity

class FamilyDetailsActivity : BaseMultiLanguageActivity() {

  private lateinit var familyId: String
  private lateinit var fhirEngine: FhirEngine
  private lateinit var familyMemberAdapter: FamilyMemberAdapter
  private lateinit var familyEncounterAdapter: FamilyEncounterAdapter

  lateinit var familyMemberRepository: FamilyMemberRepository

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_family_details)
    setSupportActionBar(findViewById(R.id.familyDetailToolbar))

    //familyId = intent.extras?.getString(FamilyDetailView.FAMILY_ARG_ITEM_ID) ?: ""

    fhirEngine = (AncApplication.getContext() as ConfigurableApplication).fhirEngine

    familyMemberRepository = FamilyMemberRepository(familyId, fhirEngine)

    familyMemberAdapter =
      FamilyMemberAdapter(this::onFamilyMemberItemClicked, this::onAddNewMemberButtonClicked)
    findViewById<RecyclerView>(R.id.memberList).adapter = familyMemberAdapter

    familyEncounterAdapter = FamilyEncounterAdapter(this::onFamilyEncounterItemClicked)
    findViewById<RecyclerView>(R.id.encounterList).adapter = familyEncounterAdapter
  }

  override fun onResume() {
    super.onResume()
    familyMemberRepository.fetchDemographics().observe(this, this::handlePatientDemographics)
    familyMemberRepository.fetchFamilyMembers().observe(this, this::handleFamilyMembers)
    familyMemberRepository.fetchEncounters().observe(this, this::handleFamilyEncounters)
  }

  private fun handlePatientDemographics(family: Patient) {
    with(family) {
      findViewById<TextView>(R.id.familyName).text = name?.firstOrNull()?.given?.firstOrNull()?.value
      findViewById<TextView>(R.id.familySurname).text = name?.firstOrNull()?.family
    }
  }

  private fun handleFamilyMembers(familyMembers: List<FamilyMemberItem>) {
    familyMemberAdapter.submitList(familyMembers)
  }

  private fun handleFamilyEncounters(familyEncounters: List<Encounter>) {
    familyEncounterAdapter.submitList(familyEncounters)
  }

  private fun onFamilyMemberItemClicked(familyMemberItem: FamilyMemberItem) {}

  private fun onAddNewMemberButtonClicked() {}

  private fun onFamilyEncounterItemClicked(encounter: Encounter) {}
}