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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.smartregister.fhircore.quest.ui.family.profile.FamilyProfileViewState

@Composable
fun FamilyProfileTopBar(viewState: FamilyProfileViewState, modifier: Modifier) {
  Column(modifier = modifier.fillMaxWidth()) {
    Text(text = viewState.familyName, color = Color.White, modifier = modifier.padding(top = 4.dp))
    Text(
      text = "${viewState.houseNumber} ${viewState.villageTown}",
      color = Color.LightGray.copy(alpha = 0.4f),
      modifier = modifier.padding(top = 4.dp)
    )
  }
}

@Composable
@Preview(showBackground = true)
fun FamilyProfileTopBarPreview() {
  FamilyProfileTopBar(
    viewState =
      FamilyProfileViewState()
        .copy(familyName = "William Odinga", houseNumber = "#4", villageTown = "Sugoi, Bondo"),
    modifier = Modifier
  )
}
