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

package org.smartregister.fhircore.quest.ui.patient.profile.guardians

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import org.smartregister.fhircore.engine.domain.model.RegisterData
import org.smartregister.fhircore.engine.ui.theme.FemalePinkColor
import org.smartregister.fhircore.engine.ui.theme.MaleBlueColor
import org.smartregister.fhircore.quest.R
import org.smartregister.fhircore.quest.ui.patient.register.components.HivPatientRegisterListRow
import org.smartregister.fhircore.quest.ui.shared.models.RegisterViewData

@Composable
fun GuardiansRoute(
  navigateRoute: (String) -> Unit,
  onBackPress: () -> Unit,
  viewModel: GuardianRegisterViewModel = hiltViewModel()
) {
  val uiState by viewModel.guardianUiDetails

  GuardiansRegisterScreen(
    navigateRoute = navigateRoute,
    onBackPress = onBackPress,
    patientFirstName = uiState.patientFirstName,
    viewGuardiansData = uiState.registerViewData
  )
}

@Composable
fun GuardiansRegisterScreen(
  navigateRoute: (String) -> Unit,
  onBackPress: () -> Unit,
  patientFirstName: String,
  viewGuardiansData: List<GuardianPatientRegisterData>
) {
  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text(stringResource(id = R.string.guardians_screen_title, patientFirstName)) },
        navigationIcon = {
          IconButton(onClick = { onBackPress() }) {
            Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = null)
          }
        }
      )
    }
  ) { contentPadding ->
    LazyColumn(modifier = Modifier.padding(contentPadding)) {
      items(viewGuardiansData, key = { it.viewData.logicalId }) { viewGuardianItem ->
        HivPatientRegisterListRow(
          data = viewGuardianItem.viewData,
          onItemClick = { navigateRoute(viewGuardianItem.profileNavRoute) }
        )
      }
    }
  }
}

@Preview(showBackground = true)
@Composable
fun PreviewGuardiansScreen() {
  GuardiansRegisterScreen(
    onBackPress = { /*TODO*/},
    patientFirstName = "Izzy",
    viewGuardiansData =
      listOf(
        GuardianPatientRegisterData(
          viewData =
            RegisterViewData(
              logicalId = "eddb38b9-5363-4bcc-8cb9-dbbc2b4cddd9",
              identifier = "38",
              title = "Isabel Iguana",
              subtitle = "24yr, ART Client",
              serviceTextIcon = R.drawable.baseline_pregnant_woman_24,
              serviceButtonBackgroundColor = FemalePinkColor,
              registerType = RegisterData.HivRegisterData::class,
            ),
          profileNavRoute = "*TODO*"
        ),
        GuardianPatientRegisterData(
          viewData =
            RegisterViewData(
              logicalId = "1212299",
              identifier = "    ",
              title = "Garry Iguana",
              serviceTextIcon = R.drawable.baseline_man_24,
              subtitle = "25yr, Not On ART",
              serviceButtonBackgroundColor = MaleBlueColor,
              registerType = RegisterData.HivRegisterData::class,
            ),
          profileNavRoute = "*TODO*"
        )
      ),
    navigateRoute = { /*TODO*/}
  )
}
