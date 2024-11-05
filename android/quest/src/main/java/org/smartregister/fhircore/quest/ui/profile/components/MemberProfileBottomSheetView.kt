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

@file:OptIn(ExperimentalMaterialApi::class)

package org.smartregister.fhircore.quest.ui.profile.components

import android.graphics.Bitmap
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.configuration.view.ButtonProperties
import org.smartregister.fhircore.engine.domain.model.ResourceData
import org.smartregister.fhircore.engine.ui.theme.DefaultColor
import org.smartregister.fhircore.engine.ui.theme.DividerColor
import org.smartregister.fhircore.engine.ui.theme.InfoColor
import org.smartregister.fhircore.engine.util.annotation.PreviewWithBackgroundExcludeGenerated
import org.smartregister.fhircore.quest.R
import org.smartregister.fhircore.quest.ui.shared.components.ActionableButton

const val VIEW_PROFILE_TAG = "view-profile-tag"
const val TOP_SECTION_ROW = "top-section-row"
const val ICON_FIELD_TAG = "iconTestTag"

@Composable
fun MemberProfileBottomSheetView(
  modifier: Modifier = Modifier,
  coroutineScope: CoroutineScope = rememberCoroutineScope(),
  bottomSheetScaffoldState: BottomSheetScaffoldState = rememberBottomSheetScaffoldState(),
  title: String,
  buttonProperties: List<ButtonProperties>,
  resourceData: ResourceData,
  navController: NavController = rememberNavController(),
  onViewProfile: () -> Unit,
  decodeImage: ((String) -> Bitmap?)?,
) {
  Column {
    // Top section displays the name, gender and age for member
    Spacer(modifier = modifier.height(16.dp))
    Row(
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
      modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp).testTag(TOP_SECTION_ROW),
    ) {
      Column(modifier = modifier.wrapContentWidth(Alignment.Start)) {
        Text(
          text = stringResource(id = R.string.what_to_do),
          fontWeight = FontWeight.SemiBold,
          modifier = modifier.padding(bottom = 4.dp),
        )
        Text(text = title, color = DefaultColor.copy(0.8f))
      }
      Icon(
        imageVector = Icons.Filled.Clear,
        contentDescription = null,
        tint = DefaultColor.copy(0.8f),
        modifier =
          modifier
            .clickable {
              coroutineScope.launch {
                if (!bottomSheetScaffoldState.bottomSheetState.isCollapsed) {
                  bottomSheetScaffoldState.bottomSheetState.collapse()
                }
              }
            }
            .testTag(ICON_FIELD_TAG),
      )
    }
    Spacer(modifier = modifier.height(8.dp))
    Divider(color = DividerColor)

    if (buttonProperties.isNotEmpty()) {
      Spacer(modifier = modifier.height(8.dp))
      buttonProperties.forEach {
        ActionableButton(
          buttonProperties = it,
          resourceData = resourceData,
          navController = navController,
          decodeImage = decodeImage,
        )
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
          .testTag(VIEW_PROFILE_TAG),
    )
    Spacer(modifier = modifier.height(16.dp))
  }
}

@PreviewWithBackgroundExcludeGenerated
@Composable
private fun MemberProfileBottomSheetViewPreview() {
  MemberProfileBottomSheetView(
    coroutineScope = rememberCoroutineScope(),
    bottomSheetScaffoldState = rememberBottomSheetScaffoldState(),
    title = "John Doe, M, 35y",
    buttonProperties = emptyList(),
    navController = rememberNavController(),
    onViewProfile = { /*Do nothing*/},
    resourceData = ResourceData("id", ResourceType.Patient, emptyMap()),
    decodeImage = null,
  )
}

@PreviewWithBackgroundExcludeGenerated
@Composable
private fun MemberProfileBottomSheetViewWithFormDataPreview() {
  MemberProfileBottomSheetView(
    coroutineScope = rememberCoroutineScope(),
    bottomSheetScaffoldState = rememberBottomSheetScaffoldState(),
    title = "John Doe, M, 35y",
    buttonProperties =
      listOf(
        ButtonProperties(text = "Issue bednet", status = "OVERDUE"),
        ButtonProperties(text = "Sick child", status = "UPCOMING"),
        ButtonProperties(text = "Pregnancy visit", status = "COMPLETED"),
      ),
    navController = rememberNavController(),
    onViewProfile = { /*Do nothing*/},
    resourceData = ResourceData("id", ResourceType.Patient, emptyMap()),
    decodeImage = null,
  )
}
