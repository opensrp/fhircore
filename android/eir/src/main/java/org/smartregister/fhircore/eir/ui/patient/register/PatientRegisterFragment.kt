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

package org.smartregister.fhircore.eir.ui.patient.register

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.paging.compose.LazyPagingItems
import org.hl7.fhir.r4.model.Immunization
import org.hl7.fhir.r4.model.Patient
import org.smartregister.fhircore.eir.EirApplication
import org.smartregister.fhircore.eir.data.PatientRepository
import org.smartregister.fhircore.eir.data.model.PatientItem
import org.smartregister.fhircore.eir.data.model.VaccineStatus
import org.smartregister.fhircore.eir.ui.patient.details.PatientDetailsActivity
import org.smartregister.fhircore.eir.ui.patient.register.components.PatientRegisterList
import org.smartregister.fhircore.eir.ui.vaccine.RecordVaccineActivity
import org.smartregister.fhircore.eir.util.RECORD_VACCINE_FORM
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity
import org.smartregister.fhircore.engine.ui.register.ComposeRegisterFragment
import org.smartregister.fhircore.engine.ui.register.RegisterDataViewModel
import org.smartregister.fhircore.engine.ui.register.model.RegisterFilterType
import org.smartregister.fhircore.engine.util.ListenerIntent
import org.smartregister.fhircore.engine.util.extension.createFactory

class PatientRegisterFragment :
  ComposeRegisterFragment<Pair<Patient, List<Immunization>>, PatientItem>() {

  override fun navigateToDetails(uniqueIdentifier: String) {
    startActivity(
      Intent(requireActivity(), PatientDetailsActivity::class.java).apply {
        putExtras(PatientDetailsActivity.requiredIntentArgs(uniqueIdentifier))
      }
    )
  }

  @Composable
  override fun ConstructRegisterList(pagingItems: LazyPagingItems<PatientItem>) {
    PatientRegisterList(
      pagingItems = pagingItems,
      modifier = Modifier,
      clickListener = { listenerIntent, data -> onItemClicked(listenerIntent, data) }
    )
  }

  override fun onItemClicked(listenerIntent: ListenerIntent, data: PatientItem) {
    if (listenerIntent is PatientRowClickListenerIntent) {
      when (listenerIntent) {
        OpenPatientProfile -> navigateToDetails(data.patientIdentifier)
        RecordPatientVaccine -> {
          startActivity(
            Intent(requireContext(), RecordVaccineActivity::class.java)
              .putExtras(
                QuestionnaireActivity.requiredIntentArgs(
                  clientIdentifier = data.patientIdentifier,
                  form = RECORD_VACCINE_FORM
                )
              )
          )
        }
      }
    }
  }

  override fun performFilter(
    registerFilterType: RegisterFilterType,
    data: PatientItem,
    value: Any
  ): Boolean {
    return when (registerFilterType) {
      RegisterFilterType.SEARCH_FILTER -> {
        if (value is String && value.isEmpty()) return true
        else
          data.name.contains(value.toString(), ignoreCase = true) ||
            data.patientIdentifier.contentEquals(value.toString())
      }
      RegisterFilterType.OVERDUE_FILTER -> {
        if (value is Boolean && value) data.vaccineStatus.status == VaccineStatus.OVERDUE
        else return true
      }
    }
  }

  @Suppress("UNCHECKED_CAST")
  override fun initializeRegisterDataViewModel():
    RegisterDataViewModel<Pair<Patient, List<Immunization>>, PatientItem> {
    val patientRepository =
      PatientRepository(
        (requireActivity().application as EirApplication).fhirEngine,
        PatientItemMapper
      )
    return ViewModelProvider(
      requireActivity(),
      RegisterDataViewModel(
          application = requireActivity().application,
          registerRepository = patientRepository
        )
        .createFactory()
    )[RegisterDataViewModel::class.java] as
      RegisterDataViewModel<Pair<Patient, List<Immunization>>, PatientItem>
  }
}
