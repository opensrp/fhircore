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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.smartregister.fhircore.engine.configuration.view.CompoundTextProperties
import org.smartregister.fhircore.engine.util.extension.interpolate
import org.smartregister.fhircore.engine.util.extension.parseColor

@Composable
fun CompoundText(
  modifier: Modifier = Modifier,
  compoundTextProperties: CompoundTextProperties,
  computedValuesMap: Map<String, Any>
) {
  Text(
    text =
      buildAnnotatedString {
        // Primary text
        withStyle(
          style =
            SpanStyle(
              background =
                compoundTextProperties
                  .primaryTextBackgroundColor
                  ?.interpolate(computedValuesMap)
                  .parseColor(),
              color =
                compoundTextProperties.primaryTextColor?.interpolate(computedValuesMap).parseColor()
            )
        ) { append(compoundTextProperties.primaryText?.interpolate(computedValuesMap) ?: "") }

        if (!compoundTextProperties.secondaryText?.interpolate(computedValuesMap).isNullOrEmpty()) {
          // Separator
          withStyle(
            style =
              SpanStyle(
                color =
                  compoundTextProperties
                    .primaryTextColor
                    ?.interpolate(computedValuesMap)
                    .parseColor(),
                letterSpacing = 16.sp
              )
          ) { append(compoundTextProperties.separator ?: "-") }
        }

        // Secondary text
        withStyle(
          style =
            SpanStyle(
              background =
                compoundTextProperties
                  .secondaryTextBackgroundColor
                  ?.interpolate(computedValuesMap)
                  .parseColor(),
              color =
                compoundTextProperties
                  .secondaryTextColor
                  ?.interpolate(computedValuesMap)
                  .parseColor()
            )
        ) { append(compoundTextProperties.secondaryText?.interpolate(computedValuesMap) ?: "") }
      },
    fontSize = compoundTextProperties.fontSize.sp,
    modifier =
      modifier
        .wrapContentWidth()
        .background(
          compoundTextProperties.backgroundColor?.interpolate(computedValuesMap).parseColor()
        )
        .clip(RoundedCornerShape(compoundTextProperties.borderRadius.dp))
        .padding(compoundTextProperties.padding.dp)
  )
}

@Preview(showBackground = true)
@Composable
private fun CompoundTextNoSecondaryTextPreview() {
  Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
    CompoundText(
      compoundTextProperties =
        CompoundTextProperties(primaryText = "Full Name, Age", primaryTextColor = "#000000"),
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
        CompoundTextProperties(primaryText = "Full Name, Sex, Age", primaryTextColor = "#000000"),
      computedValuesMap = emptyMap()
    )
    CompoundText(
      compoundTextProperties =
        CompoundTextProperties(
          primaryText = "Last visited",
          primaryTextColor = "#5A5A5A",
          secondaryText = "G6PD status",
          secondaryTextColor = "#FFFFFF",
          separator = "-",
          secondaryTextBackgroundColor = "#FFA500"
        ),
      computedValuesMap = emptyMap()
    )
  }
}
