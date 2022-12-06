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

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.accompanist.flowlayout.FlowColumn
import com.google.accompanist.flowlayout.FlowRow
import org.smartregister.fhircore.engine.configuration.view.ButtonProperties
import org.smartregister.fhircore.engine.configuration.view.CardViewProperties
import org.smartregister.fhircore.engine.configuration.view.ColumnProperties
import org.smartregister.fhircore.engine.configuration.view.CompoundTextProperties
import org.smartregister.fhircore.engine.configuration.view.ListProperties
import org.smartregister.fhircore.engine.configuration.view.PersonalDataProperties
import org.smartregister.fhircore.engine.configuration.view.RowProperties
import org.smartregister.fhircore.engine.configuration.view.ServiceCardProperties
import org.smartregister.fhircore.engine.configuration.view.SpacerProperties
import org.smartregister.fhircore.engine.configuration.view.ViewAlignment
import org.smartregister.fhircore.engine.configuration.view.ViewProperties
import org.smartregister.fhircore.engine.domain.model.ResourceData
import org.smartregister.fhircore.engine.domain.model.ViewType
import org.smartregister.fhircore.engine.util.extension.parseColor
import org.smartregister.fhircore.quest.util.extensions.conditional

@Composable
fun GenerateView(
  modifier: Modifier = Modifier,
  properties: ViewProperties,
  resourceData: ResourceData,
  navController: NavController
) {
  when (properties.viewType) {
    ViewType.COMPOUND_TEXT -> {
      CompoundText(
        modifier = modifier,
        compoundTextProperties = properties as CompoundTextProperties,
        resourceData = resourceData,
        navController = navController
      )
    }
    ViewType.BUTTON -> {
      ActionableButton(
        modifier = modifier,
        buttonProperties = properties as ButtonProperties,
        navController = navController,
        resourceData = resourceData
      )
    }
    ViewType.COLUMN -> {
      val children = (properties as ColumnProperties).children
      if (properties.wrapContent) {
        FlowColumn(modifier = modifier.padding(properties.padding.dp)) {
          properties.children.forEach { properties ->
            GenerateView(
              modifier = generateModifier(properties),
              properties = properties,
              resourceData = resourceData,
              navController = navController
            )
          }
        }
      } else {
        val isWeighted = remember { children.any { it.weight > 0 } }
        Column(
          horizontalAlignment =
            when (properties.alignment) {
              ViewAlignment.START -> Alignment.Start
              ViewAlignment.END -> Alignment.End
              ViewAlignment.CENTER -> Alignment.CenterHorizontally
              ViewAlignment.NONE -> Alignment.Start
            },
          modifier = modifier.padding(properties.padding.dp),
          verticalArrangement =
            if (isWeighted) Arrangement.spacedBy(properties.spacedBy.dp)
            else properties.arrangement?.position ?: Arrangement.Top
        ) {
          for (child in children) {
            GenerateView(
              modifier = generateModifier(child),
              properties = child,
              resourceData = resourceData,
              navController = navController
            )
          }
        }
      }
    }
    ViewType.ROW -> {
      val children = (properties as RowProperties).children
      if (properties.wrapContent) {
        FlowRow(modifier = modifier.padding(properties.padding.dp)) {
          properties.children.forEach { properties ->
            GenerateView(
              modifier = generateModifier(properties),
              properties = properties,
              resourceData = resourceData,
              navController = navController
            )
          }
        }
      } else {
        val isWeighted = remember { children.any { it.weight > 0 } }
        Row(
          verticalAlignment =
            when (properties.alignment) {
              ViewAlignment.START -> Alignment.Top
              ViewAlignment.END -> Alignment.Bottom
              ViewAlignment.CENTER -> Alignment.CenterVertically
              ViewAlignment.NONE -> Alignment.CenterVertically
            },
          modifier = modifier.padding(properties.padding.dp),
          horizontalArrangement =
            if (isWeighted) Arrangement.spacedBy(properties.spacedBy.dp)
            else properties.arrangement?.position ?: Arrangement.Start
        ) {
          children.any { it.weight > 0 }
          for (child in children) {
            GenerateView(
              modifier = generateModifier(child),
              properties = child,
              resourceData = resourceData,
              navController = navController
            )
          }
        }
      }
    }
    ViewType.SERVICE_CARD ->
      ServiceCard(
        modifier = modifier,
        serviceCardProperties = properties as ServiceCardProperties,
        resourceData = resourceData,
        navController = navController
      )
    ViewType.CARD ->
      CardView(
        modifier = modifier,
        viewProperties = properties as CardViewProperties,
        resourceData = resourceData,
        navController = navController
      )
    ViewType.PERSONAL_DATA ->
      PersonalDataView(
        modifier = modifier,
        personalDataCardProperties = properties as PersonalDataProperties,
        resourceData = resourceData,
        navController = navController
      )
    ViewType.SPACER ->
      SpacerView(modifier = modifier, spacerProperties = properties as SpacerProperties)
    ViewType.LIST ->
      List(
        modifier = modifier,
        viewProperties = properties as ListProperties,
        resourceData = resourceData,
        navController = navController,
      )
  }
}

@SuppressLint("ComposableModifierFactory", "ModifierFactoryExtensionFunction")
@Composable
fun RowScope.generateModifier(viewProperties: ViewProperties): Modifier {
  var modifier = if (viewProperties.weight > 0) Modifier.weight(viewProperties.weight) else Modifier

  modifier = modifier.applyCommonProperties(viewProperties)

  return when (viewProperties.alignment) {
    ViewAlignment.START -> modifier.align(Alignment.Top)
    ViewAlignment.END -> modifier.align(Alignment.Bottom)
    ViewAlignment.CENTER -> modifier.align(Alignment.CenterVertically)
    else -> modifier
  }
}

@SuppressLint("ComposableModifierFactory", "ModifierFactoryExtensionFunction")
@Composable
fun ColumnScope.generateModifier(viewProperties: ViewProperties): Modifier {
  var modifier =
    if (viewProperties.weight > 0) {
      Modifier.weight(viewProperties.weight)
    } else Modifier

  modifier = modifier.applyCommonProperties(viewProperties)

  return when (viewProperties.alignment) {
    ViewAlignment.START -> modifier.align(Alignment.Start)
    ViewAlignment.END -> modifier.align(Alignment.End)
    ViewAlignment.CENTER -> modifier.align(Alignment.CenterHorizontally)
    else -> modifier
  }
}

@SuppressLint("ComposableModifierFactory", "ModifierFactoryExtensionFunction")
@Composable
fun generateModifier(viewProperties: ViewProperties): Modifier =
  Modifier.applyCommonProperties(viewProperties)

@SuppressLint("ComposableModifierFactory", "ModifierFactoryExtensionFunction")
@Composable
private fun Modifier.applyCommonProperties(viewProperties: ViewProperties): Modifier =
  this.conditional(viewProperties.fillMaxWidth, { fillMaxWidth() })
    .conditional(viewProperties.fillMaxHeight, { fillMaxHeight() })
    .background(viewProperties.backgroundColor.parseColor())
    .clip(RoundedCornerShape(viewProperties.borderRadius.dp))
