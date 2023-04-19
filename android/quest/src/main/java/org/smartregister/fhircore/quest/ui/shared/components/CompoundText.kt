/*
 * Copyright 2021-2023 Ona Systems, Inc
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

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.flowlayout.FlowRow
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.configuration.view.CompoundTextProperties
import org.smartregister.fhircore.engine.configuration.view.SpacerProperties
import org.smartregister.fhircore.engine.configuration.view.TextCase
import org.smartregister.fhircore.engine.configuration.view.TextFontWeight
import org.smartregister.fhircore.engine.configuration.view.TextOverFlow
import org.smartregister.fhircore.engine.configuration.view.ViewAlignment
import org.smartregister.fhircore.engine.domain.model.ActionConfig
import org.smartregister.fhircore.engine.domain.model.ResourceData
import org.smartregister.fhircore.engine.domain.model.ViewType
import org.smartregister.fhircore.engine.ui.theme.DefaultColor
import org.smartregister.fhircore.engine.util.annotation.PreviewWithBackgroundExcludeGenerated
import org.smartregister.fhircore.engine.util.extension.camelCase
import org.smartregister.fhircore.engine.util.extension.interpolate
import org.smartregister.fhircore.engine.util.extension.parseColor
import org.smartregister.fhircore.engine.util.extension.removeExtraWhiteSpaces
import org.smartregister.fhircore.quest.util.extensions.conditional
import org.smartregister.fhircore.quest.util.extensions.handleClickEvent

@Composable
fun CompoundText(
  modifier: Modifier = Modifier,
  compoundTextProperties: CompoundTextProperties,
  resourceData: ResourceData,
  navController: NavController
) {
  FlowRow(
    modifier =
      modifier
        .conditional(compoundTextProperties.fillMaxWidth, { fillMaxWidth() })
        .conditional(compoundTextProperties.fillMaxHeight, { fillMaxHeight() })
        .padding(
          horizontal = compoundTextProperties.padding.dp,
          vertical = compoundTextProperties.padding.div(2).dp
        )
        .background(
          compoundTextProperties
            .backgroundColor
            ?.interpolate(resourceData.computedValuesMap)
            .parseColor()
        )
  ) {
    val interpolatedPrimaryText =
      compoundTextProperties.primaryText?.interpolate(resourceData.computedValuesMap)
    val interpolatedSecondaryText =
      compoundTextProperties.secondaryText?.interpolate(resourceData.computedValuesMap)
    val interpolatedPrimaryTextColor =
      compoundTextProperties.primaryTextColor?.interpolate(resourceData.computedValuesMap)
    val interpolatedPrimaryTextBackgroundColor =
      compoundTextProperties.primaryTextBackgroundColor?.interpolate(resourceData.computedValuesMap)
    val interpolatedSecondaryTextColor =
      compoundTextProperties.secondaryTextColor?.interpolate(resourceData.computedValuesMap)
    val interpolatedSecondaryTextBackgroundColor =
      compoundTextProperties.secondaryTextBackgroundColor?.interpolate(
        resourceData.computedValuesMap
      )
    val interpolatedSeparator =
      compoundTextProperties.separator?.interpolate(resourceData.computedValuesMap)

    if (!interpolatedPrimaryText.isNullOrEmpty()) {
      CompoundTextPart(
        modifier = modifier,
        viewAlignment = compoundTextProperties.alignment,
        text = interpolatedPrimaryText,
        textCase = compoundTextProperties.textCase,
        maxLines = compoundTextProperties.maxLines,
        textColor = interpolatedPrimaryTextColor,
        backgroundColor = interpolatedPrimaryTextBackgroundColor,
        borderRadius = compoundTextProperties.borderRadius,
        fontSize = compoundTextProperties.fontSize,
        textFontWeight = compoundTextProperties.primaryTextFontWeight,
        clickable = compoundTextProperties.clickable,
        actions = compoundTextProperties.primaryTextActions,
        resourceData = resourceData,
        navController = navController,
        overflow = compoundTextProperties.overflow
      )
    }
    // Separate the primary and secondary text
    if (!interpolatedSeparator.isNullOrEmpty()) {
      Box(contentAlignment = Alignment.Center, modifier = modifier.padding(horizontal = 6.dp)) {
        Text(
          text = interpolatedSeparator,
          fontSize = compoundTextProperties.fontSize.sp,
          color = DefaultColor,
          textAlign = TextAlign.Center
        )
      }
    }
    if (!interpolatedSecondaryText.isNullOrEmpty()) {
      CompoundTextPart(
        modifier = modifier,
        viewAlignment = compoundTextProperties.alignment,
        text = interpolatedSecondaryText,
        textCase = compoundTextProperties.textCase,
        maxLines = compoundTextProperties.maxLines,
        textColor = interpolatedSecondaryTextColor,
        backgroundColor = interpolatedSecondaryTextBackgroundColor,
        borderRadius = compoundTextProperties.borderRadius,
        fontSize = compoundTextProperties.fontSize,
        textFontWeight = compoundTextProperties.secondaryTextFontWeight,
        clickable = compoundTextProperties.clickable,
        actions = compoundTextProperties.secondaryTextActions,
        navController = navController,
        resourceData = resourceData,
        overflow = compoundTextProperties.overflow
      )
    }
  }
}

@Composable
private fun CompoundTextPart(
  modifier: Modifier,
  viewAlignment: ViewAlignment,
  text: String,
  textCase: TextCase?,
  maxLines: Int,
  textColor: String?,
  colorOpacity: Float = 1f,
  backgroundColor: String?,
  borderRadius: Int,
  fontSize: Float,
  textFontWeight: TextFontWeight,
  clickable: String,
  actions: List<ActionConfig>,
  navController: NavController,
  resourceData: ResourceData,
  overflow: TextOverFlow?
) {
  Text(
    text =
      when (textCase) {
        TextCase.UPPER_CASE -> text.uppercase()
        TextCase.LOWER_CASE -> text.lowercase()
        TextCase.CAMEL_CASE -> text.camelCase()
        null -> text
      }.removeExtraWhiteSpaces(),
    color =
      textColor
        ?.interpolate(resourceData.computedValuesMap)
        ?.parseColor()
        ?.copy(alpha = colorOpacity)
        ?: DefaultColor.copy(alpha = colorOpacity),
    modifier =
      modifier
        .wrapContentWidth(Alignment.Start)
        .conditional(
          clickable.interpolate(resourceData.computedValuesMap).toBoolean(),
          { clickable { actions.handleClickEvent(navController, resourceData) } }
        )
        .clip(RoundedCornerShape(borderRadius.dp))
        .background(backgroundColor.parseColor())
        .padding(4.dp),
    fontSize = fontSize.sp,
    fontWeight = textFontWeight.fontWeight,
    textAlign =
      when (viewAlignment) {
        ViewAlignment.START -> TextAlign.Start
        ViewAlignment.END -> TextAlign.End
        ViewAlignment.CENTER -> TextAlign.Center
        else -> TextAlign.Start
      },
    maxLines = maxLines,
    overflow =
      when (overflow) {
        TextOverFlow.CLIP -> TextOverflow.Clip
        TextOverFlow.VISIBLE -> TextOverflow.Visible
        else -> TextOverflow.Ellipsis
      }
  )
}

@SuppressLint("UnrememberedMutableState")
@PreviewWithBackgroundExcludeGenerated
@Composable
private fun CompoundTextNoSecondaryTextPreview() {
  val navController = rememberNavController()
  Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
    CompoundText(
      compoundTextProperties =
        CompoundTextProperties(
          primaryText = "Full Name, Age",
          primaryTextColor = "#000000",
          primaryTextFontWeight = TextFontWeight.SEMI_BOLD,
        ),
      resourceData =
        ResourceData("id", ResourceType.Patient, mutableStateMapOf(), mutableStateMapOf()),
      navController = navController
    )
    CompoundText(
      compoundTextProperties =
        CompoundTextProperties(
          primaryText = "Sex",
          primaryTextColor = "#5A5A5A",
        ),
      resourceData =
        ResourceData("id", ResourceType.Patient, mutableStateMapOf(), mutableStateMapOf()),
      navController = navController
    )
  }
}

@SuppressLint("UnrememberedMutableState")
@PreviewWithBackgroundExcludeGenerated
@Composable
private fun CompoundTextWithSecondaryTextPreview() {
  val navController = rememberNavController()
  Row(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
    CompoundText(
      compoundTextProperties =
        CompoundTextProperties(primaryText = "Full Name, Sex, Age", primaryTextColor = "#000000"),
      resourceData =
        ResourceData("id", ResourceType.Patient, mutableStateMapOf(), mutableStateMapOf()),
      navController = navController
    )
    SpacerView(spacerProperties = SpacerProperties(viewType = ViewType.SPACER, width = 8f))
    CompoundText(
      compoundTextProperties =
        CompoundTextProperties(
          primaryText = "Stock status",
          primaryTextColor = "#5A5A5A",
          secondaryText = "Overdue",
          secondaryTextColor = "#000000",
          separator = ":",
          secondaryTextBackgroundColor = "#FFA500"
        ),
      resourceData =
        ResourceData("id", ResourceType.Patient, mutableStateMapOf(), mutableStateMapOf()),
      navController = navController
    )
  }
}
