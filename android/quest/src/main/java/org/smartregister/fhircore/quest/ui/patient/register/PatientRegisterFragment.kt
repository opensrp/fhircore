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

package org.smartregister.fhircore.quest.ui.patient.register

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.paging.compose.LazyPagingItems
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import org.hl7.fhir.r4.model.Patient
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity.Companion.QUESTIONNAIRE_ARG_PATIENT_KEY
import org.smartregister.fhircore.engine.ui.register.ComposeRegisterFragment
import org.smartregister.fhircore.engine.ui.register.RegisterDataViewModel
import org.smartregister.fhircore.engine.ui.register.model.RegisterFilterType
import org.smartregister.fhircore.engine.util.ListenerIntent
import org.smartregister.fhircore.engine.util.extension.createFactory
import org.smartregister.fhircore.quest.data.patient.PatientRepository
import org.smartregister.fhircore.quest.data.patient.model.PatientItem
import org.smartregister.fhircore.quest.ui.patient.details.QuestPatientDetailActivity
import org.smartregister.fhircore.quest.ui.patient.register.components.PatientRegisterList

@AndroidEntryPoint
class PatientRegisterFragment : ComposeRegisterFragment<Patient, PatientItem>() {

  @Inject lateinit var patientRepository: PatientRepository
  @Inject lateinit var configurationRegistry: ConfigurationRegistry

  override fun navigateToDetails(uniqueIdentifier: String) {
    startActivity(
      Intent(requireActivity(), QuestPatientDetailActivity::class.java)
        .putExtra(QUESTIONNAIRE_ARG_PATIENT_KEY, uniqueIdentifier)
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
    when (listenerIntent) {
      OpenPatientProfile -> navigateToDetails(data.id)
      else -> throw UnsupportedOperationException("Given ListenerIntent is not supported")
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
            data.identifier.contentEquals(value.toString()) ||
            data.id == value.toString()
      }
      else -> false
    }
  }

  @Suppress("UNCHECKED_CAST")
  override fun initializeRegisterDataViewModel(): RegisterDataViewModel<Patient, PatientItem> {
    return ViewModelProvider(
      viewModelStore,
      RegisterDataViewModel(
          application = requireActivity().application,
          registerRepository = patientRepository
        )
        .createFactory()
    )[RegisterDataViewModel::class.java] as
      RegisterDataViewModel<Patient, PatientItem>
  }

  companion object {
    const val TAG = "PatientRegisterFragment"
  }
}
