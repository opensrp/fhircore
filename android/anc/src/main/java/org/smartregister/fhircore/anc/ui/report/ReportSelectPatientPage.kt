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

package org.smartregister.fhircore.anc.ui.report

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.paging.compose.collectAsLazyPagingItems
import kotlinx.coroutines.flow.emptyFlow
import org.smartregister.fhircore.anc.ui.anccare.register.components.AncPatientList

@Composable
fun ReportSelectPatientScreen(viewModel: ReportViewModel) {
  val registerData = viewModel.allRegisterData.collectAsState(emptyFlow())
  val pagingItems = registerData.value.collectAsLazyPagingItems()
  AncPatientList(
    pagingItems = pagingItems,
    modifier = Modifier,
    clickListener = viewModel::onPatientItemClicked,
    showAncVisitButton = false,
    displaySelectContentOnly = true
  )
}
