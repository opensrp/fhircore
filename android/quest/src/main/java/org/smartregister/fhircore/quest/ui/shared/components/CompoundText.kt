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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.smartregister.fhircore.engine.configuration.view.CompoundTextProperties
import org.smartregister.fhircore.engine.ui.components.Separator
import org.smartregister.fhircore.engine.util.extension.interpolate
import org.smartregister.fhircore.engine.util.extension.parseColor

@Composable
fun CompoundText(
  modifier: Modifier = Modifier,
  compoundTextProperties: CompoundTextProperties,
  computedValuesMap: Map<String, Any>
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier =
      modifier
        .background(
          compoundTextProperties.backgroundColor?.interpolate(computedValuesMap).parseColor()
        )
        .padding(
          horizontal = compoundTextProperties.padding.dp,
          vertical = compoundTextProperties.padding.div(2).dp
        ),
  ) {
    if (compoundTextProperties.primaryText != null) {
      Text(
        text = compoundTextProperties.primaryText!!.interpolate(computedValuesMap),
        color = compoundTextProperties.primaryTextColor.parseColor(),
        modifier =
          modifier
            .wrapContentWidth(Alignment.Start)
            .background(
              compoundTextProperties.primaryTextBackgroundColor?.parseColor() ?: Color.Unspecified
            )
            .padding(2.dp),
        fontSize = compoundTextProperties.fontSize.sp,
      )
    }
    if (compoundTextProperties.secondaryText != null) {
      // Separate the primary and secondary text
      Separator(separator = compoundTextProperties.separator ?: "-")
      Text(
        text = compoundTextProperties.secondaryText!!.interpolate(computedValuesMap),
        color = compoundTextProperties.secondaryTextColor.parseColor(),
        modifier =
          modifier
            .wrapContentWidth(Alignment.Start)
            .background(
              compoundTextProperties.secondaryTextBackgroundColor?.parseColor() ?: Color.Unspecified
            )
            .padding(2.dp),
        fontSize = compoundTextProperties.fontSize.sp,
      )
    }
  }
}

@Preview(showBackground = true)
@Composable
private fun CompoundTextNoSecondaryTextPreview() {
  Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
    CompoundText(
      compoundTextProperties =
        CompoundTextProperties(
          primaryText = "Full Name, Age",
          primaryTextColor = "#000000",
        ),
      computedValuesMap = emptyMap()
    )
    CompoundText(
      compoundTextProperties =
        CompoundTextProperties(
          primaryText = "Sex",
          primaryTextColor = "#5A5A5A",
        ),
      computedValuesMap = emptyMap()
    )
  }
}

@Preview(showBackground = true)
@Composable
private fun CompoundTextWithSecondaryTextPreview() {
  Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
    CompoundText(
      compoundTextProperties =
        CompoundTextProperties(
          primaryText = "Full Name, Sex, Age",
          primaryTextColor = "#000000",
        ),
      computedValuesMap = emptyMap()
    )
    CompoundText(
      compoundTextProperties =
        CompoundTextProperties(
          primaryText = "Last visited",
          primaryTextColor = "#5A5A5A",
          secondaryText = "G6PD status",
          separator = "-",
          secondaryTextColor = "#FFFFFF",
          secondaryTextBackgroundColor = "#FFA500"
        ),
      computedValuesMap = emptyMap()
    )
  }
}
