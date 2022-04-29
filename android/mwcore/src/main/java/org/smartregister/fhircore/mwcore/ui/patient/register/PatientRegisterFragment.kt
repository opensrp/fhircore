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

package org.smartregister.fhircore.mwcore.ui.patient.register

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.paging.compose.LazyPagingItems
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import org.hl7.fhir.r4.model.Patient
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity.Companion.QUESTIONNAIRE_ARG_PATIENT_KEY
import org.smartregister.fhircore.engine.ui.register.ComposeRegisterFragment
import org.smartregister.fhircore.engine.ui.register.RegisterDataViewModel
import org.smartregister.fhircore.engine.ui.register.model.RegisterFilterType
import org.smartregister.fhircore.engine.util.ListenerIntent
import org.smartregister.fhircore.engine.util.extension.createFactory
import org.smartregister.fhircore.mwcore.data.patient.PatientRepository
import org.smartregister.fhircore.mwcore.data.patient.model.PatientItem
import org.smartregister.fhircore.mwcore.ui.patient.details.QuestPatientDetailActivity
import org.smartregister.fhircore.mwcore.ui.patient.register.components.PatientRegisterList

@AndroidEntryPoint
class PatientRegisterFragment : ComposeRegisterFragment<Patient, PatientItem>() {

  @Inject lateinit var patientRepository: PatientRepository

  override fun navigateToDetails(uniqueIdentifier: String) {
    startActivity(
      Intent(requireActivity(), QuestPatientDetailActivity::class.java)
        .putExtra(QUESTIONNAIRE_ARG_PATIENT_KEY, uniqueIdentifier)
    )
  }

  @Composable
  override fun ConstructRegisterList(
    pagingItems: LazyPagingItems<PatientItem>,
    modifier: Modifier
  ) {
    PatientRegisterList(
      pagingItems = pagingItems,
      modifier = modifier,
      clickListener = { listenerIntent, data -> onItemClicked(listenerIntent, data) }
    )
  }

  override fun onItemClicked(listenerIntent: ListenerIntent, data: PatientItem) {
    when (listenerIntent) {
      OpenPatientProfile -> navigateToDetails(data.id)
      else -> throw UnsupportedOperationException("Given ListenerIntent is not supported")
    }
  }

  /**
   * Filters the given data if there is a matching condition.
   *
   * SEARCH_FILTER will filters data based on name OR id OR identifier.
   *
   * @param registerFilterType the filter type
   * @param data the data that will be filtered
   * @param value the query
   *
   * @return true if the data should be filtered
   */
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
            data.identifier.contentEquals(value.toString()) ||
            data.id == value.toString()
      }
      else -> false
    }
  }

  override fun initializeRegisterDataViewModel(): RegisterDataViewModel<Patient, PatientItem> {
    val registerDataViewModel =
      RegisterDataViewModel(
        application = requireActivity().application,
        registerRepository = patientRepository
      )
    return ViewModelProvider(viewModelStore, registerDataViewModel.createFactory())[
      registerDataViewModel::class.java]
  }

  companion object {
    const val TAG = "PatientRegisterFragment"
  }
}
