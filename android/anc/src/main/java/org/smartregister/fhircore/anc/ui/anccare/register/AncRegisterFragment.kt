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

package org.smartregister.fhircore.anc.ui.anccare.register

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.paging.compose.LazyPagingItems
import com.google.android.fhir.FhirEngine
import org.smartregister.fhircore.anc.data.model.PatientItem
import org.smartregister.fhircore.anc.data.model.VisitStatus
import org.smartregister.fhircore.anc.data.patient.PatientRepository
import org.smartregister.fhircore.anc.ui.anccare.register.components.AncPatientList
import org.smartregister.fhircore.anc.ui.details.PatientDetailsActivity
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity
import org.smartregister.fhircore.engine.ui.register.ComposeRegisterFragment
import org.smartregister.fhircore.engine.ui.register.RegisterDataViewModel
import org.smartregister.fhircore.engine.ui.register.model.RegisterFilterType
import org.smartregister.fhircore.engine.util.ListenerIntent
import org.smartregister.fhircore.engine.util.extension.createFactory
import javax.inject.Inject

class AncRegisterFragment : ComposeRegisterFragment<Anc, PatientItem>() {

  @Inject
  lateinit var fhirEngine: FhirEngine

  @Inject
  lateinit var ancPatientRepository: PatientRepository


  override fun navigateToDetails(uniqueIdentifier: String) {
    startActivity(
      Intent(requireActivity(), PatientDetailsActivity::class.java).apply {
        putExtra(QuestionnaireActivity.QUESTIONNAIRE_ARG_PATIENT_KEY, uniqueIdentifier)
      }
    )
  }

  @Composable
  override fun ConstructRegisterList(pagingItems: LazyPagingItems<PatientItem>) {
    AncPatientList(
      pagingItems = pagingItems,
      modifier = Modifier,
      clickListener = { listenerIntent, data -> onItemClicked(listenerIntent, data) }
    )
  }

  override fun onItemClicked(listenerIntent: ListenerIntent, data: PatientItem) {
    if (listenerIntent is AncRowClickListenerIntent) {
      when (listenerIntent) {
        OpenPatientProfile -> navigateToDetails(data.patientIdentifier)
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
        return data.visitStatus == VisitStatus.OVERDUE
      }
    }
  }

  @Suppress("UNCHECKED_CAST")
  override fun initializeRegisterDataViewModel(): RegisterDataViewModel<Anc, PatientItem> {
    return ViewModelProvider(
      viewModelStore,
      RegisterDataViewModel(
          application = requireActivity().application,
          registerRepository = ancPatientRepository
        )
        .createFactory()
    )[RegisterDataViewModel::class.java] as
      RegisterDataViewModel<Anc, PatientItem>
  }

  companion object {
    const val TAG = "AncRegisterFragment"
  }
}
