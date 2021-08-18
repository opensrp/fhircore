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
import android.os.Bundle
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.lifecycle.ViewModelProvider
import androidx.paging.compose.collectAsLazyPagingItems
import org.hl7.fhir.r4.model.Immunization
import org.hl7.fhir.r4.model.Patient
import org.smartregister.fhircore.eir.form.config.QuestionnaireFormConfig
import org.smartregister.fhircore.eir.ui.patient.details.PatientDetailsActivity
import org.smartregister.fhircore.eir.ui.patient.register.components.PatientRow
import org.smartregister.fhircore.engine.data.local.repository.patient.PatientPaginatedDataSource
import org.smartregister.fhircore.engine.data.local.repository.patient.model.PatientItem
import org.smartregister.fhircore.engine.data.local.repository.patient.model.VaccineStatus
import org.smartregister.fhircore.engine.ui.components.PaginatedList
import org.smartregister.fhircore.engine.ui.register.BaseRegisterDataViewModel
import org.smartregister.fhircore.engine.ui.register.ComposeRegisterFragment
import org.smartregister.fhircore.engine.ui.register.model.RegisterFilterType
import org.smartregister.fhircore.engine.util.ListenerIntent
import org.smartregister.fhircore.engine.util.extension.createFactory

class PatientRegisterFragment :
  ComposeRegisterFragment<Pair<Patient, List<Immunization>>, PatientItem>() {

  override lateinit var paginatedDataSource: PatientPaginatedDataSource

  override lateinit var registerDataViewModel:
    BaseRegisterDataViewModel<Pair<Patient, List<Immunization>>, PatientItem>

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    registerDataViewModel =
      ViewModelProvider(
        requireActivity(),
        PatientRegisterDataViewModel(
            application = requireActivity().application,
            paginatedDataSource = paginatedDataSource,
          )
          .createFactory()
      )[PatientRegisterDataViewModel::class.java]
  }

  override fun navigateToDetails(uniqueIdentifier: String) {
    startActivity(
      Intent(requireActivity(), PatientDetailsActivity::class.java).apply {
        putExtra(QuestionnaireFormConfig.COVAX_ARG_ITEM_ID, uniqueIdentifier)
      }
    )
  }

  @Composable
  override fun ConstructRegisterList() {
    val registerData = registerDataViewModel.registerData.observeAsState()
    PaginatedList(
      pagingItems = registerData.value!!.collectAsLazyPagingItems(),
      { patientItem ->
        PatientRow(
          patientItem = patientItem,
          navigateToDetails = { identifier -> navigateToDetails(identifier) }
        )
      }
    )
  }

  override fun onItemClicked(listenerIntent: ListenerIntent, data: PatientItem) {
    // Overridden
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
}
