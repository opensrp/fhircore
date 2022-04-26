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

package org.smartregister.fhircore.quest.ui.family.profile.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.smartregister.fhircore.quest.ui.patient.profile.model.ProfileViewData

@Composable
fun FamilyProfileTopBar(
  familyProfileViewData: ProfileViewData.FamilyProfileViewData,
  modifier: Modifier
) {
  Column(
    modifier = modifier.fillMaxWidth().background(MaterialTheme.colors.primary).padding(16.dp)
  ) {
    Text(
      text = familyProfileViewData.name,
      color = Color.White,
      fontSize = 20.sp,
      modifier = modifier.padding(top = 4.dp)
    )
    Text(
      text = familyProfileViewData.address,
      color = Color.LightGray.copy(alpha = 0.8f),
      fontSize = 20.sp,
      modifier = modifier.padding(top = 4.dp)
    )
  }
}

@Composable
@Preview(showBackground = true)
fun FamilyProfileTopBarPreview() {
  FamilyProfileTopBar(
    familyProfileViewData =
      ProfileViewData.FamilyProfileViewData(name = "William Odinga", address = "#4 Sugoi, Bondo"),
    modifier = Modifier
  )
}
