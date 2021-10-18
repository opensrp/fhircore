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
import org.smartregister.fhircore.anc.AncApplication
import org.smartregister.fhircore.anc.data.patient.PatientRepository
import org.smartregister.fhircore.anc.data.model.AncPatientItem
import org.smartregister.fhircore.anc.data.model.VisitStatus
import org.smartregister.fhircore.anc.ui.anccare.details.AncDetailsActivity
import org.smartregister.fhircore.anc.ui.anccare.register.components.AncPatientList
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity
import org.smartregister.fhircore.engine.ui.register.ComposeRegisterFragment
import org.smartregister.fhircore.engine.ui.register.RegisterDataViewModel
import org.smartregister.fhircore.engine.ui.register.model.RegisterFilterType
import org.smartregister.fhircore.engine.util.ListenerIntent
import org.smartregister.fhircore.engine.util.extension.createFactory

class AncRegisterFragment : ComposeRegisterFragment<Anc, AncPatientItem>() {

  override fun navigateToDetails(uniqueIdentifier: String) {
    startActivity(
      Intent(requireActivity(), AncDetailsActivity::class.java).apply {
        putExtra(QuestionnaireActivity.QUESTIONNAIRE_ARG_PATIENT_KEY, uniqueIdentifier)
      }
    )
  }

  @Composable
  override fun ConstructRegisterList(pagingItems: LazyPagingItems<AncPatientItem>) {
    AncPatientList(
      pagingItems = pagingItems,
      modifier = Modifier,
      clickListener = { listenerIntent, data -> onItemClicked(listenerIntent, data) }
    )
  }

  override fun onItemClicked(listenerIntent: ListenerIntent, data: AncPatientItem) {
    if (listenerIntent is AncRowClickListenerIntent) {
      when (listenerIntent) {
        OpenPatientProfile -> navigateToDetails(data.patientIdentifier)
      }
    }
  }

  override fun performFilter(
    registerFilterType: RegisterFilterType,
    data: AncPatientItem,
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
  override fun initializeRegisterDataViewModel(): RegisterDataViewModel<Anc, AncPatientItem> {
    val ancPatientRepository =
      PatientRepository((requireActivity().application as AncApplication).fhirEngine, AncItemMapper)
    return ViewModelProvider(
      requireActivity(),
      RegisterDataViewModel(
          application = requireActivity().application,
          registerRepository = ancPatientRepository
        )
        .createFactory()
    )[RegisterDataViewModel::class.java] as
      RegisterDataViewModel<Anc, AncPatientItem>
  }
}
