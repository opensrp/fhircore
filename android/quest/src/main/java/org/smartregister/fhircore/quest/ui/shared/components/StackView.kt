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

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.configuration.view.StackViewProperties
import org.smartregister.fhircore.engine.configuration.view.ViewAlignment
import org.smartregister.fhircore.engine.domain.model.ResourceData
import org.smartregister.fhircore.engine.util.annotation.PreviewWithBackgroundExcludeGenerated

const val STACK_VIEW_TEST_TAG = "stackViewTestTag"

@Composable
fun StackView(
  modifier: Modifier,
  stackViewProperties: StackViewProperties,
  resourceData: ResourceData,
  navController: NavController,
  decodeImage: ((String) -> Bitmap?)?,
) {
  Box(
    modifier.size(stackViewProperties.size.dp).testTag(STACK_VIEW_TEST_TAG),
    contentAlignment = castViewAlignment(stackViewProperties.alignment),
  ) {
    stackViewProperties.children.forEach { child ->
      GenerateView(
        modifier = generateModifier(viewProperties = child),
        properties = child.interpolate(resourceData.computedValuesMap),
        resourceData = resourceData,
        navController = navController,
        decodeImage = decodeImage,
      )
    }
  }
}

fun castViewAlignment(
  viewAlignment: ViewAlignment,
): Alignment {
  return when (viewAlignment) {
    ViewAlignment.TOPSTART -> Alignment.TopStart
    ViewAlignment.TOPEND -> Alignment.TopEnd
    ViewAlignment.CENTER -> Alignment.Center
    ViewAlignment.BOTTOMSTART -> Alignment.BottomStart
    ViewAlignment.BOTTOMEND -> Alignment.BottomEnd
    else -> {
      Alignment.Center
    }
  }
}

@PreviewWithBackgroundExcludeGenerated
@Composable
private fun PreviewStack() {
  StackView(
    stackViewProperties =
      StackViewProperties(
        opacity = 0.2f,
        size = 250,
        backgroundColor = "successColor",
      ),
    modifier = Modifier,
    navController = rememberNavController(),
    resourceData =
      ResourceData(
        baseResourceId = "baseId",
        baseResourceType = ResourceType.Patient,
        computedValuesMap = emptyMap(),
      ),
    decodeImage = null,
  )
}
