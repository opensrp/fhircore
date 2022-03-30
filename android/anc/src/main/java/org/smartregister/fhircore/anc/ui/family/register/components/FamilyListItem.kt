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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Date
import org.smartregister.fhircore.anc.R
import org.smartregister.fhircore.anc.data.family.model.FamilyItem
import org.smartregister.fhircore.anc.data.family.model.FamilyMemberItem
import org.smartregister.fhircore.anc.ui.family.register.FamilyListenerIntent
import org.smartregister.fhircore.anc.ui.family.register.OpenFamilyProfile
import org.smartregister.fhircore.engine.ui.components.Separator
import org.smartregister.fhircore.engine.ui.theme.BlueTextColor
import org.smartregister.fhircore.engine.ui.theme.OverdueDarkRedColor
import org.smartregister.fhircore.engine.ui.theme.SubtitleTextColor
import org.smartregister.fhircore.engine.util.annotation.ExcludeFromJacocoGeneratedReport

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
          .padding(16.dp)
          .weight(0.70f)
    ) {
      Text(
        text = familyItem.extractDemographics(),
        fontSize = 18.sp,
        modifier = modifier.wrapContentWidth()
      )
      Spacer(modifier = modifier.height(8.dp))
      Row(
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          color = SubtitleTextColor,
          text = familyItem.address,
          fontSize = 14.sp,
          modifier = modifier.wrapContentWidth()
        )
        Separator(
          modifier = modifier,
          showSeparator =
            familyItem.address.isNotEmpty() && familyItem.members.any { it.pregnant == true }
        )
        familyItem.members.filter { it.pregnant == true }.forEach { _ ->
          Image(
            painter = painterResource(R.drawable.ic_pregnant),
            contentDescription = stringResource(id = R.string.pregnant_woman)
          )
        }
      }
    }
    Row(
      modifier = modifier.wrapContentWidth(Alignment.End).weight(0.30f).padding(horizontal = 16.dp),
      horizontalArrangement = Arrangement.SpaceAround,
      verticalAlignment = Alignment.CenterVertically
    ) {
      if (familyItem.servicesOverdue != null && familyItem.servicesOverdue > 0) {
        ServicesCard(
          modifier = modifier,
          text = familyItem.servicesOverdue.toString(),
          color = OverdueDarkRedColor
        )
      }
      if (familyItem.servicesDue != null && familyItem.servicesDue > 0) {
        ServicesCard(
          modifier = modifier,
          text = familyItem.servicesDue.toString(),
          color = BlueTextColor
        )
      }
    }
  }
}

@Composable
fun ServicesCard(modifier: Modifier, text: String, color: Color) {
  Spacer(modifier = modifier.width(6.dp))
  Box(
    contentAlignment = Alignment.Center,
    modifier = modifier.clip(CircleShape).background(color = color).size(32.dp).padding(4.dp)
  ) { Text(color = Color.White, text = text, fontSize = 12.8.sp, fontWeight = FontWeight.Bold) }
}

@Composable
@Preview(showBackground = true)
@ExcludeFromJacocoGeneratedReport
fun FamilyRowPreview() {
  val fmi = FamilyMemberItem("fmname", "fm1", Date(), "F", true, false, Date(), 2, 3)

  val familyItem = FamilyItem("fid", "1111", "Name ", "Nairobi", fmi, listOf(fmi, fmi, fmi), 4, 5)
  FamilyRow(familyItem = familyItem, { _, _ -> })
}
