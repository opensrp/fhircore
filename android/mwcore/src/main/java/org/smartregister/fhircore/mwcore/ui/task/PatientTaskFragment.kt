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

package org.smartregister.fhircore.mwcore.ui.task

import android.os.Bundle
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.paging.compose.LazyPagingItems
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.view.RegisterViewConfiguration
import org.smartregister.fhircore.engine.ui.register.ComposeRegisterFragment
import org.smartregister.fhircore.engine.ui.register.RegisterDataViewModel
import org.smartregister.fhircore.engine.ui.register.model.RegisterFilterType
import org.smartregister.fhircore.engine.util.ListenerIntent
import org.smartregister.fhircore.engine.util.extension.createFactory
import org.smartregister.fhircore.mwcore.data.task.PatientTaskRepository
import org.smartregister.fhircore.mwcore.data.task.model.PatientTaskItem
import org.smartregister.fhircore.mwcore.ui.task.component.PatientTaskList
import org.smartregister.fhircore.mwcore.util.MwCoreConfigClassification
import org.smartregister.fhircore.mwcore.util.QuestJsonSpecificationProvider

@AndroidEntryPoint
class PatientTaskFragment : ComposeRegisterFragment<PatientTask, PatientTaskItem>() {

  @Inject lateinit var patientTaskRepository: PatientTaskRepository
  @Inject lateinit var questJsonSpecificationProvider: QuestJsonSpecificationProvider
  @Inject lateinit var configurationRegistry: ConfigurationRegistry

  override lateinit var registerViewConfiguration: RegisterViewConfiguration

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    registerViewConfiguration =
      configurationRegistry.retrieveConfiguration(
        configClassification = MwCoreConfigClassification.PATIENT_TASK_REGISTER,
        questJsonSpecificationProvider.getJson()
      )
    configureViews(registerViewConfiguration)
  }

  override fun navigateToDetails(uniqueIdentifier: String) {
    // TODO: Nothing to navigate at the moment
  }

  @Composable
  override fun ConstructRegisterList(
    pagingItems: LazyPagingItems<PatientTaskItem>,
    modifier: Modifier
  ) {
    PatientTaskList(
      pagingItems = pagingItems,
      useLabel = registerViewConfiguration.useLabel,
      modifier = Modifier,
      clickListener = { listenerIntent, data -> onItemClicked(listenerIntent, data) }
    )
  }

  override fun onItemClicked(listenerIntent: ListenerIntent, data: PatientTaskItem) {
    // Nothing to navigate at the moment
  }

  override fun performFilter(
    registerFilterType: RegisterFilterType,
    data: PatientTaskItem,
    value: Any
  ): Boolean {
    // Nothing to filter at the moment
    return false
  }

  override fun initializeRegisterDataViewModel():
    RegisterDataViewModel<PatientTask, PatientTaskItem> {
    val registerDataViewModel =
      RegisterDataViewModel(
        application = requireActivity().application,
        registerRepository = patientTaskRepository
      )
    return ViewModelProvider(viewModelStore, registerDataViewModel.createFactory())[
      registerDataViewModel::class.java]
  }

  companion object {
    const val TAG = "PatientTaskFragment"
  }
}
