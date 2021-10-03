package org.smartregister.fhircore.quest.ui.patient.details

import androidx.lifecycle.LiveData
import org.hl7.fhir.r4.model.Patient

interface QuestPatientDetailDataProvider {

  fun getDemographics(): LiveData<Patient>
  fun onBackPressListener(): () -> Unit = {}
  fun onMenuItemClickListener(): (menuItem: String) -> Unit = {}
}