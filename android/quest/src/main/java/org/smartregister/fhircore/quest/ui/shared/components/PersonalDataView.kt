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

package org.smartregister.fhircore.quest.ui.shared.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.accompanist.flowlayout.FlowRow
import org.smartregister.fhircore.engine.configuration.view.CompoundTextProperties
import org.smartregister.fhircore.engine.configuration.view.PersonalDataItem
import org.smartregister.fhircore.engine.configuration.view.PersonalDataProperties
import org.smartregister.fhircore.engine.ui.theme.PersonalDataBackgroundColor

@Composable
fun PersonalDataView(
  modifier: Modifier = Modifier,
  personalDataCardProperties: PersonalDataProperties,
  computedValuesMap: Map<String, Any> = emptyMap()
) {
  FlowRow(
    modifier =
      modifier
        .padding(end = 8.dp)
        .clip(RoundedCornerShape(size = 8.dp))
        .background(PersonalDataBackgroundColor)
  ) {
    PersonalDataItem(
      personalDataCardProperties = personalDataCardProperties,
      computedValuesMap = computedValuesMap
    )
  }
}

@Composable
private fun PersonalDataItem(
  personalDataCardProperties: PersonalDataProperties,
  modifier: Modifier = Modifier,
  computedValuesMap: Map<String, Any> = emptyMap()
) {
  personalDataCardProperties.personalDataItems.forEach {
    Column(modifier = modifier.padding(vertical = 16.dp, horizontal = 24.dp)) {
      CompoundText(compoundTextProperties = it.label, computedValuesMap = computedValuesMap)
      CompoundText(compoundTextProperties = it.displayValue, computedValuesMap = computedValuesMap)
    }
  }
}

@Composable
@Preview(showBackground = true)
fun PersonalDataViewPreview() {
  val genderLabel =
    CompoundTextProperties(
      primaryText = "Sex",
    )
  val genderValue =
    CompoundTextProperties(
      primaryText = "Female",
    )
  val genderDataItem = PersonalDataItem(label = genderLabel, displayValue = genderValue)

  val dobLabel =
    CompoundTextProperties(
      primaryText = "DOB",
    )
  val dobValue =
    CompoundTextProperties(
      primaryText = "01 2000",
    )
  val dobDataItem = PersonalDataItem(label = dobLabel, displayValue = dobValue)

  val ageTitle =
    CompoundTextProperties(
      primaryText = "Age",
    )
  val ageValue =
    CompoundTextProperties(
      primaryText = "22y",
    )
  val ageDataItem = PersonalDataItem(label = ageTitle, displayValue = ageValue)

  val personaDataItems = listOf(genderDataItem, dobDataItem, ageDataItem)
  val personalDataCardProperties = PersonalDataProperties(personalDataItems = personaDataItems)

  PersonalDataView(personalDataCardProperties = personalDataCardProperties)
}
