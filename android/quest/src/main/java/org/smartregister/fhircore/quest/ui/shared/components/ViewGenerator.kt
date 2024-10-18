/*
 * Copyright 2021-2024 Ona Systems, Inc
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
import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowColumn
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import org.smartregister.fhircore.engine.configuration.view.ButtonProperties
import org.smartregister.fhircore.engine.configuration.view.CardViewProperties
import org.smartregister.fhircore.engine.configuration.view.ColumnProperties
import org.smartregister.fhircore.engine.configuration.view.CompoundTextProperties
import org.smartregister.fhircore.engine.configuration.view.DividerProperties
import org.smartregister.fhircore.engine.configuration.view.ImageProperties
import org.smartregister.fhircore.engine.configuration.view.ListProperties
import org.smartregister.fhircore.engine.configuration.view.PersonalDataProperties
import org.smartregister.fhircore.engine.configuration.view.RowProperties
import org.smartregister.fhircore.engine.configuration.view.ServiceCardProperties
import org.smartregister.fhircore.engine.configuration.view.SpacerProperties
import org.smartregister.fhircore.engine.configuration.view.StackViewProperties
import org.smartregister.fhircore.engine.configuration.view.ViewAlignment
import org.smartregister.fhircore.engine.configuration.view.ViewProperties
import org.smartregister.fhircore.engine.domain.model.ResourceData
import org.smartregister.fhircore.engine.domain.model.ViewType
import org.smartregister.fhircore.engine.ui.theme.DividerColor
import org.smartregister.fhircore.engine.util.extension.parseColor
import org.smartregister.fhircore.quest.util.extensions.conditional
import org.smartregister.fhircore.quest.util.extensions.handleClickEvent

const val COLUMN_DIVIDER_TEST_TAG = "horizontalDividerTestTag"

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GenerateView(
  modifier: Modifier = Modifier,
  properties: ViewProperties,
  resourceData: ResourceData,
  navController: NavController,
  decodeImage: ((String) -> Bitmap?)?,
) {
  if (properties.visible.toBoolean()) {
    when (properties.viewType) {
      ViewType.COMPOUND_TEXT ->
        CompoundText(
          modifier = modifier,
          compoundTextProperties = properties as CompoundTextProperties,
          resourceData = resourceData,
          navController = navController,
        )
      ViewType.BUTTON ->
        ActionableButton(
          modifier = modifier,
          buttonProperties = properties as ButtonProperties,
          resourceData = resourceData,
          navController = navController,
          decodeImage = decodeImage,
        )
      ViewType.COLUMN -> {
        val children = (properties as ColumnProperties).children
        if (properties.wrapContent) {
          FlowColumn(modifier = modifier.padding(properties.padding.dp)) {
            properties.children.forEach { properties ->
              GenerateView(
                modifier =
                  generateModifier(properties)
                    .conditional(
                      properties.clickable.toBoolean(),
                      {
                        clickable {
                          (properties as RowProperties)
                            .actions
                            .handleClickEvent(navController, resourceData)
                        }
                      },
                    ),
                properties = properties,
                resourceData = resourceData,
                navController = navController,
                decodeImage = decodeImage,
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
                else -> {
                  Alignment.Start
                }
              },
            modifier =
              modifier
                .padding(properties.padding.dp)
                .conditional(
                  properties.clickable.toBoolean(),
                  {
                    clickable { properties.actions.handleClickEvent(navController, resourceData) }
                  },
                ),
            verticalArrangement =
              if (isWeighted) {
                Arrangement.spacedBy(properties.spacedBy.dp)
              } else {
                properties.arrangement?.position ?: Arrangement.Top
              },
          ) {
            children.forEachIndexed { index, child ->
              GenerateView(
                modifier = generateModifier(child),
                properties = child.interpolate(resourceData.computedValuesMap),
                resourceData = resourceData,
                navController = navController,
                decodeImage = decodeImage,
              )
              if (properties.showDivider.toBoolean() && index < children.lastIndex) {
                Divider(
                  color = DividerColor,
                  thickness = 0.5.dp,
                  modifier = Modifier.testTag(COLUMN_DIVIDER_TEST_TAG),
                )
              }
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
                modifier =
                  generateModifier(properties)
                    .conditional(
                      properties.clickable.toBoolean(),
                      {
                        clickable {
                          (properties as RowProperties)
                            .actions
                            .handleClickEvent(navController, resourceData)
                        }
                      },
                    ),
                properties = properties.interpolate(resourceData.computedValuesMap),
                resourceData = resourceData,
                navController = navController,
                decodeImage = decodeImage,
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
                else -> {
                  Alignment.CenterVertically
                }
              },
            modifier =
              modifier
                .padding(properties.padding.dp)
                .conditional(
                  properties.clickable.toBoolean(),
                  {
                    clickable { properties.actions.handleClickEvent(navController, resourceData) }
                  },
                ),
            horizontalArrangement =
              if (isWeighted) {
                Arrangement.spacedBy(properties.spacedBy.dp)
              } else {
                properties.arrangement?.position ?: Arrangement.Start
              },
          ) {
            for (child in children) {
              GenerateView(
                modifier = generateModifier(child),
                properties = child.interpolate(resourceData.computedValuesMap),
                resourceData = resourceData,
                navController = navController,
                decodeImage = decodeImage,
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
          navController = navController,
          decodeImage = decodeImage,
        )
      ViewType.CARD ->
        CardView(
          modifier = modifier,
          viewProperties = properties as CardViewProperties,
          resourceData = resourceData,
          navController = navController,
          decodeImage = decodeImage,
        )
      ViewType.PERSONAL_DATA ->
        PersonalDataView(
          modifier = modifier,
          personalDataCardProperties = properties as PersonalDataProperties,
          resourceData = resourceData,
          navController = navController,
        )
      ViewType.SPACER ->
        SpacerView(modifier = modifier, spacerProperties = properties as SpacerProperties)
      ViewType.BORDER ->
        DividerView(modifier = modifier, dividerProperties = properties as DividerProperties)
      ViewType.LIST ->
        List(
          modifier = modifier,
          viewProperties = properties as ListProperties,
          resourceData = resourceData,
          navController = navController,
          decodeImage = decodeImage,
        )
      ViewType.IMAGE ->
        Image(
          modifier = modifier,
          imageProperties = properties as ImageProperties,
          resourceData = resourceData,
          navController = navController,
          decodeImage = decodeImage,
        )
      ViewType.STACK ->
        StackView(
          modifier = modifier,
          stackViewProperties = properties as StackViewProperties,
          resourceData = resourceData,
          navController = navController,
          decodeImage = decodeImage,
        )
    }
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
    } else {
      Modifier
    }

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
    .background(
      viewProperties.backgroundColor.parseColor().let { baseColor ->
        if (viewProperties.opacity != null) {
          baseColor.copy(alpha = viewProperties.opacity!!.toFloat())
        } else {
          baseColor
        }
      },
    )
    .clip(RoundedCornerShape(viewProperties.borderRadius.dp))
