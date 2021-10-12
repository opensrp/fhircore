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
import androidx.compose.foundation.border
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.smartregister.fhircore.anc.R
import org.smartregister.fhircore.anc.data.family.model.FamilyItem
import org.smartregister.fhircore.anc.data.family.model.FamilyMemberItem
import org.smartregister.fhircore.anc.ui.family.register.FamilyListenerIntent
import org.smartregister.fhircore.anc.ui.family.register.OpenFamilyProfile
import org.smartregister.fhircore.engine.ui.theme.DueColor
import org.smartregister.fhircore.engine.ui.theme.OverdueColor
import org.smartregister.fhircore.engine.ui.theme.SubtitleTextColor

@Composable
fun FamilyRow(
  familyItem: FamilyItem,
  clickListener: (FamilyListenerIntent, FamilyItem) -> Unit,
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
        if (familyItem.isPregnant) {
          Image(
            painter = painterResource(R.drawable.ic_pregnant),
            contentDescription = "Contact anc picture",
            modifier = Modifier.size(20.dp).border(1.dp, Color.LightGray, CircleShape)
          )
        }

        familyItem.members.filter { it.pregnant }.forEach { _ ->
          Image(
            painter = painterResource(R.drawable.ic_pregnant),
            contentDescription = "Contact anc picture",
            modifier = Modifier.size(20.dp)
          )
        }

        Text(
          color = SubtitleTextColor,
          text = familyItem.address,
          fontSize = 15.sp,
          modifier = modifier.wrapContentWidth()
        )
      }
    }

    if (familyItem.servicesOverdue > 0) {
      Column(modifier = modifier.weight(0.08f), horizontalAlignment = Alignment.End) {
        servicesCard(
          modifier = modifier,
          text = familyItem.servicesOverdue.toString(),
          color = OverdueColor
        )
      }
    }
    if (familyItem.servicesDue > 0) {
      Column(modifier = modifier.weight(0.08f), horizontalAlignment = Alignment.End) {
        servicesCard(
          modifier = modifier,
          text = familyItem.servicesDue.toString(),
          color = DueColor
        )
      }
    }
  }
}

@Composable
fun servicesCard(modifier: Modifier, text: String, color: Color) {
  Card(
    backgroundColor = color,
    shape = CircleShape,
    modifier = modifier.size(36.dp).padding(5.dp)
  ) {
    Text(
      color = Color.White,
      text = text,
      fontSize = 13.sp,
      modifier = modifier.wrapContentWidth().padding(4.dp),
      softWrap = false
    )
  }
}

@Composable
@Preview(showBackground = true)
fun FamilyRowPreview() {
  val fmi = FamilyMemberItem("fmname", "fm1", "21", "F", true)

  val familyItem =
    FamilyItem("fid", "1111", "Name ", "M", "27", "Nairobi", true, listOf(fmi, fmi, fmi), 4, 5)
  FamilyRow(familyItem = familyItem, { _, _ -> })
}
