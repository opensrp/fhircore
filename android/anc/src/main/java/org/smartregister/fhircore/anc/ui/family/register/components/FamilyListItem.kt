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

package org.smartregister.fhircore.anc.ui.family.register.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.smartregister.fhircore.anc.R
import org.smartregister.fhircore.anc.data.family.model.FamilyItem
import org.smartregister.fhircore.anc.ui.family.register.OpenFamilyProfile
import org.smartregister.fhircore.engine.ui.theme.OverdueColor
import org.smartregister.fhircore.engine.ui.theme.SubtitleTextColor
import org.smartregister.fhircore.engine.util.ListenerIntent

@Composable
fun FamilyRow(
  familyItem: FamilyItem,
  clickListener: (ListenerIntent, FamilyItem) -> Unit = { _, _ -> },
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
          .clickable { clickListener(OpenFamilyProfile, familyItem) }
          .padding(10.dp)
          .weight(0.65f)
    ) {
      Text(
        text = familyItem.extractDemographics(),
        fontSize = 16.sp,
        modifier = modifier.wrapContentWidth()
      )
      Spacer(modifier = modifier.height(8.dp))
      Row {
        Text(
          color = SubtitleTextColor,
          text = familyItem.address,
          fontSize = 12.sp,
          modifier = modifier.wrapContentWidth()
        )
      }
      Row {
        familyItem.members.filter { it.pregnant }.forEach { _ ->
          Image(
            painter = painterResource(R.drawable.ic_pregnant),
            contentDescription = "Contact anc picture",
            modifier = Modifier.size(20.dp)
          )
        }
      }
    }
    Column(modifier = modifier.weight(0.15f)) {
      if (familyItem.servicesOverdue > 0) {
        Card(
          elevation = 2.dp,
          backgroundColor = OverdueColor,
          shape = CircleShape,
          modifier = modifier.padding(5.dp)
        ) {
          Text(
            color = Color.White,
            text = familyItem.servicesOverdue.toString(),
            fontSize = 18.sp,
            modifier = modifier.wrapContentWidth()
          )
        }
      }
    }
    Column(modifier = modifier.weight(0.15f)) {
      if (familyItem.servicesDue > 0) {
        Card(elevation = 4.dp, backgroundColor = OverdueColor, shape = CircleShape) {
          Text(
            color = Color.White,
            text = familyItem.servicesDue.toString(),
            fontSize = 18.sp,
            modifier = modifier.wrapContentWidth()
          )
        }
      }
    }
  }
}
