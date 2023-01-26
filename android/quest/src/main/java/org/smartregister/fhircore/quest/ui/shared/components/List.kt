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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import org.smartregister.fhircore.engine.configuration.view.ListProperties
import org.smartregister.fhircore.engine.domain.model.ResourceData
import org.smartregister.fhircore.engine.ui.theme.DefaultColor
import org.smartregister.fhircore.engine.ui.theme.DividerColor
import org.smartregister.fhircore.engine.util.extension.interpolate
import org.smartregister.fhircore.engine.util.extension.parseColor
import org.smartregister.fhircore.quest.R

@Composable
fun List(
  modifier: Modifier,
  viewProperties: ListProperties,
  resourceData: ResourceData,
  navController: NavController,
) {
  val currentListResourceData = resourceData.listResourceDataMap[viewProperties.id]

  Column(
    modifier =
      modifier
        .background(
          viewProperties.backgroundColor?.interpolate(resourceData.computedValuesMap).parseColor()
        )
        .padding(
          horizontal = viewProperties.padding.dp,
          vertical = viewProperties.padding.div(4).dp
        )
  ) {
    if (currentListResourceData.isNullOrEmpty()) {
      Box(contentAlignment = Alignment.Center, modifier = modifier.fillMaxSize()) {
        Text(
          text = viewProperties.emptyList?.message ?: stringResource(id = R.string.no_visits),
          modifier = modifier.padding(8.dp).align(Alignment.Center),
          color = DefaultColor,
          fontStyle = FontStyle.Italic
        )
      }
    } else {
      currentListResourceData.forEachIndexed { index, listResourceData ->
        Column {
          Spacer(modifier = modifier.height(5.dp))
          Box {
            ViewRenderer(
              viewProperties = viewProperties.registerCard.views,
              resourceData = listResourceData,
              navController = navController,
            )
          }
          Spacer(modifier = modifier.height(5.dp))
          if (index < currentListResourceData.lastIndex && viewProperties.showDivider)
            Divider(color = DividerColor, thickness = 0.5.dp)
        }
      }
    }
  }
}
