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

@file:OptIn(ExperimentalMaterialApi::class)

package org.smartregister.fhircore.quest.ui.family.profile.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.BottomSheetScaffoldState
import androidx.compose.material.Divider
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.smartregister.fhircore.engine.configuration.view.ButtonProperties
import org.smartregister.fhircore.engine.domain.model.ActionableButtonData
import org.smartregister.fhircore.engine.ui.components.ActionableButton
import org.smartregister.fhircore.engine.ui.theme.DefaultColor
import org.smartregister.fhircore.engine.ui.theme.DividerColor
import org.smartregister.fhircore.engine.ui.theme.InfoColor
import org.smartregister.fhircore.engine.ui.theme.OverdueColor
import org.smartregister.fhircore.quest.R

@Composable
fun FamilyMemberBottomSheet(
  coroutineScope: CoroutineScope,
  bottomSheetScaffoldState: BottomSheetScaffoldState,
  title: String,
  actionableButtonData: List<ActionableButtonData>,
  onFormClick: (String, String?) -> Unit,
  onViewProfile: () -> Unit,
  modifier: Modifier = Modifier
) {
  Column(modifier = modifier.verticalScroll(rememberScrollState())) {

    // Top section displays the name, gender and age for member
    Spacer(modifier = modifier.height(16.dp))
    Row(
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
      modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp)
    ) {
      Column(modifier = modifier.wrapContentWidth(Alignment.Start)) {
        Text(
          text = stringResource(id = R.string.what_to_do),
          fontWeight = FontWeight.SemiBold,
          modifier = modifier.padding(bottom = 4.dp)
        )
        Text(text = title, color = DefaultColor.copy(0.8f))
      }
      Icon(
        imageVector = Icons.Filled.Clear,
        contentDescription = null,
        tint = DefaultColor.copy(0.8f),
        modifier =
          modifier.clickable {
            coroutineScope.launch {
              if (!bottomSheetScaffoldState.bottomSheetState.isCollapsed)
                bottomSheetScaffoldState.bottomSheetState.collapse()
            }
          }
      )
    }
    Spacer(modifier = modifier.height(8.dp))
    Divider(color = DividerColor)

    if (actionableButtonData.isNotEmpty()) {
      Spacer(modifier = modifier.height(8.dp))
      actionableButtonData.forEach {
        ActionableButton(buttonProperties = ButtonProperties(status = "OVERDUE"), onAction = {})
      }
      Spacer(modifier = modifier.height(8.dp))
    }

    // View member profile
    Text(
      text = stringResource(R.string.view_profile),
      textAlign = TextAlign.Center,
      color = InfoColor.copy(0.8f),
      modifier =
        modifier
          .fillMaxWidth()

          .clickable { onViewProfile() }
          .padding(horizontal = 16.dp, vertical = 16.dp)
    )
    Spacer(modifier = modifier.height(16.dp))
  }
}

@Preview(showBackground = true)
@Composable
private fun FamilyMemberBottomSheetWithoutFormDataPreview() {
  FamilyMemberBottomSheet(
    coroutineScope = rememberCoroutineScope(),
    bottomSheetScaffoldState = rememberBottomSheetScaffoldState(),
    title = "John Doe, M, 35y",
    actionableButtonData = emptyList(),
    onFormClick = { _, _ -> /*Do nothing*/ },
    onViewProfile = { /*Do nothing*/}
  )
}

@Preview(showBackground = true)
@Composable
private fun FamilyMemberBottomSheetWithFormDataPreview() {
  FamilyMemberBottomSheet(
    coroutineScope = rememberCoroutineScope(),
    bottomSheetScaffoldState = rememberBottomSheetScaffoldState(),
    title = "John Doe, M, 35y",
    actionableButtonData =
      listOf(
        ActionableButtonData("Issue bednet", "12344", null, OverdueColor),
        ActionableButtonData("Sick child", "12345", null, OverdueColor),
        ActionableButtonData("Pregnancy visit", "12008")
      ),
    onFormClick = { _, _ -> /*Do nothing*/ },
    onViewProfile = { /*Do nothing*/}
  )
}
