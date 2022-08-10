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

package org.smartregister.fhircore.quest.ui.profile.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Date
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.configuration.view.CompoundTextProperties
import org.smartregister.fhircore.engine.configuration.view.PersonalDataCardProperties
import org.smartregister.fhircore.engine.ui.theme.PersonalDataBackgroundColor
import org.smartregister.fhircore.engine.ui.theme.StatusTextColor
import org.smartregister.fhircore.engine.util.extension.asDdMmm
import org.smartregister.fhircore.engine.util.extension.plusYears
import org.smartregister.fhircore.quest.ui.shared.components.CompoundText
import org.smartregister.fhircore.quest.ui.shared.models.ProfileViewData

@Composable
fun PersonalDataCard(
  modifier: Modifier = Modifier,
  personalDataCardProperties: List<PersonalDataCardProperties> = emptyList()
) {
  Card(elevation = 3.dp, modifier = modifier.fillMaxWidth()) {
    Column(modifier = modifier.padding(16.dp)) {
      Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier =
        modifier
          .clip(RoundedCornerShape(size = 8.dp))
          .background(PersonalDataBackgroundColor)
      ) {
        detailsItem(personalDataCardProperties = personalDataCardProperties)

      }
    }
  }
}

@Composable
private fun detailsItem (personalDataCardProperties: List<PersonalDataCardProperties> = emptyList(),modifier: Modifier = Modifier){
  personalDataCardProperties.forEach {
    Column(modifier = modifier.padding(16.dp)) {
      CompoundText(
        compoundTextProperties =
        it.personalDataItem[0],
        computedValuesMap = emptyMap()
      )
      CompoundText(
        compoundTextProperties =
        it.personalDataItem[1],
        computedValuesMap = emptyMap()
      )
    }
  }

}

@Composable
@Preview(showBackground = true)
fun PersonalDataCardPreview() {
  PersonalDataCard()
}
