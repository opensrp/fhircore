/*
 * Copyright 2021-2024 Ona Systems, Inc
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

package org.smartregister.fhircore.quest.ui.bottomsheet

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import org.smartregister.fhircore.engine.configuration.view.ViewProperties
import org.smartregister.fhircore.engine.domain.model.ResourceData
import org.smartregister.fhircore.quest.ui.shared.components.ViewRenderer

@Composable
fun SummaryBottomSheetView(
  properties: List<ViewProperties>,
  resourceData: ResourceData,
  navController: NavController,
) {
  ViewRenderer(
    viewProperties = properties,
    resourceData = resourceData,
    navController = navController,
    decodeImage = null,
  )
}
