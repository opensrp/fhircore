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

package org.smartregister.fhircore.anc.ui.anccare.register.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.smartregister.fhircore.anc.data.model.AncPatientItem
import org.smartregister.fhircore.anc.ui.anccare.register.AncRowClickListenerIntent
import org.smartregister.fhircore.anc.ui.anccare.register.OpenPatientProfile
import org.smartregister.fhircore.engine.data.local.repository.patient.model.PatientItem
import org.smartregister.fhircore.engine.ui.theme.WarningColor

@Composable
fun AncRow(
    ancPatientItem: AncPatientItem,
    clickListener: (AncRowClickListenerIntent, AncPatientItem) -> Unit,
    modifier: Modifier = Modifier,
) {
  Row(
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
    modifier = modifier.fillMaxWidth().height(IntrinsicSize.Min)
  ) {
    Column(
      modifier =
        modifier
          .clickable { clickListener(OpenPatientProfile, ancPatientItem) }
          .padding(24.dp)
          .weight(0.65f)
    ) {
      Text(text = ancPatientItem.demographics, fontSize = 16.sp, modifier = modifier.wrapContentWidth())
      Spacer(modifier = modifier.height(8.dp))
      Row {
        Text(
          color = WarningColor,
          text = ancPatientItem.atRisk,
          fontSize = 12.sp,
          modifier = modifier.wrapContentWidth().padding(horizontal = 8.dp)
        )
      }
    }
  }
}
