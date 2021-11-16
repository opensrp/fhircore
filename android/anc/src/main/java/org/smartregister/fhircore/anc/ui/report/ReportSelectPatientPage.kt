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
import androidx.compose.ui.Modifier
import androidx.paging.compose.LazyPagingItems
import org.smartregister.fhircore.anc.data.model.PatientItem
import org.smartregister.fhircore.anc.ui.anccare.register.components.AncPatientList
import org.smartregister.fhircore.engine.util.ListenerIntent

// @Composable
// @Preview(showBackground = true)
// @ExcludeFromJacocoGeneratedReport
// fun ReportSelectPatientPreview() {
//  ReportSelectPatientScreen(
//    pagingItems = emptyFlow(),
//    onSelectPatientItemClicked = {}
//  )
// }

@Composable
fun ReportSelectPatientScreen(
  pagingItems: LazyPagingItems<PatientItem>,
  onSelectPatientItemClicked: (ListenerIntent, PatientItem) -> Unit
) {
  AncPatientList(
    pagingItems = pagingItems,
    modifier = Modifier,
    clickListener = { listenerIntent, data -> onSelectPatientItemClicked(listenerIntent, data) },
    showAncVisitButton = false
  )
}
