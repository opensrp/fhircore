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

package org.smartregister.fhircore.quest.ui.patient.register.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.accompanist.flowlayout.FlowColumn
import com.google.accompanist.flowlayout.FlowRow
import org.hl7.fhir.r4.model.Patient
import org.smartregister.fhircore.engine.configuration.register.RegisterCardConfig
import org.smartregister.fhircore.engine.configuration.view.CompoundTextProperties
import org.smartregister.fhircore.engine.configuration.view.RegisterCardViewProperties
import org.smartregister.fhircore.engine.configuration.view.ViewGroupProperties
import org.smartregister.fhircore.engine.domain.model.ResourceData
import org.smartregister.fhircore.engine.domain.model.ViewType
import org.smartregister.fhircore.engine.ui.components.Separator
import org.smartregister.fhircore.engine.util.extension.parseColor
import org.smartregister.fhircore.quest.ui.shared.models.RegisterCardData

/**
 * A register card is a configurable view component that renders views for every of the rows of the
 * register. A register card consumes the data provided via the [ResourceData] class. The views are
 * configured via the [RegisterCardConfig]. The card also has an [onCardClick] listener that
 * responds to the click events of the register.
 *
 * The [RegisterCardConfig] defines rules that should be pre-computed before
 */
@Composable
fun RegisterCard(
  modifier: Modifier = Modifier,
  registerCardViewProperties: List<RegisterCardViewProperties>,
  registerCardData: RegisterCardData,
  onCardClick: (String) -> Unit
) {
  registerCardViewProperties.forEach { viewProperties ->
    // Render views recursively
    if (viewProperties is ViewGroupProperties) {
      if (viewProperties.children.isEmpty()) return
      when (viewProperties.viewType) {
        ViewType.COLUMN, ViewType.ROW ->
          RenderViewGroup(viewProperties, modifier, registerCardData, onCardClick)
        else -> return
      }
    } else {
      RenderChildView(
        modifier = modifier,
        registerCardViewProperties = viewProperties,
        registerCardData = registerCardData
      )
    }
  }
}

@Composable
private fun RenderViewGroup(
  viewProperties: ViewGroupProperties,
  modifier: Modifier,
  registerCardData: RegisterCardData,
  onCardClick: (String) -> Unit
) {
  viewProperties.children.forEach { childViewProperty ->
    if (childViewProperty is ViewGroupProperties) {
      if (childViewProperty.viewType == ViewType.COLUMN) {
        FlowColumn {
          RegisterCard(
            modifier = modifier,
            registerCardViewProperties = childViewProperty.children,
            registerCardData = registerCardData,
            onCardClick = onCardClick
          )
        }
      } else if (childViewProperty.viewType == ViewType.ROW) {
        FlowRow {
          RegisterCard(
            modifier = modifier,
            registerCardViewProperties = childViewProperty.children,
            registerCardData = registerCardData,
            onCardClick = onCardClick
          )
        }
      }
    }
    RenderChildView(
      modifier = modifier,
      registerCardViewProperties = childViewProperty,
      registerCardData = registerCardData
    )
  }
}

@Composable
private fun RenderChildView(
  modifier: Modifier = Modifier,
  registerCardViewProperties: RegisterCardViewProperties,
  registerCardData: RegisterCardData
) {
  when (registerCardViewProperties) {
    is CompoundTextProperties ->
      CompoundText(
        compoundTextProperties = registerCardViewProperties,
        registerCardData = registerCardData,
        modifier = modifier
      )
  }
}

@Composable
fun CompoundText(
  modifier: Modifier = Modifier,
  compoundTextProperties: CompoundTextProperties,
  registerCardData: RegisterCardData
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = modifier.padding(bottom = 8.dp),
  ) {
    if (compoundTextProperties.primaryText != null) {
      Text(
        text = compoundTextProperties.primaryText!!,
        color = compoundTextProperties.primaryTextColor.parseColor(),
        modifier = modifier.wrapContentWidth(Alignment.Start)
      )
    }
    if (compoundTextProperties.secondaryText != null) {
      // Separate the primary and secondary text
      Separator(separator = compoundTextProperties.separator ?: "-")

      Text(
        text = compoundTextProperties.secondaryText!!,
        color = compoundTextProperties.secondaryTextColor.parseColor(),
        modifier = modifier.wrapContentWidth(Alignment.Start).padding(end = 8.dp)
      )
    }
  }
}

@Preview(showBackground = true)
@Composable
private fun CompoundTextPreview() {
  Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
    CompoundText(
      compoundTextProperties =
        CompoundTextProperties(
          primaryText = "Angela Merkel, 67, F",
          primaryTextColor = "#000000",
        ),
      registerCardData =
        RegisterCardData(
          resourceData = ResourceData(baseResource = Patient()),
          computedRegisterCardData = emptyMap()
        )
    )
    CompoundText(
      compoundTextProperties =
        CompoundTextProperties(
          primaryText = "Coughlin HH",
          primaryTextColor = "#5A5A5A",
          secondaryText = "002",
          separator = "-",
          secondaryTextColor = "#5A5A5A"
        ),
      registerCardData =
        RegisterCardData(
          resourceData = ResourceData(baseResource = Patient()),
          computedRegisterCardData = emptyMap()
        )
    )
  }
}

@Preview(showBackground = true)
@Composable
private fun RegisterCardPreview() {
  val registerCardViewProperties =
    listOf<RegisterCardViewProperties>(
      ViewGroupProperties(
        viewType = ViewType.COLUMN,
        children =
          listOf(
            ViewGroupProperties(
              viewType = ViewType.COLUMN,
              children =
                listOf(
                  CompoundTextProperties(
                    viewType = ViewType.COMPOUND_TEXT,
                    primaryText = "Column #1",
                    primaryTextColor = "#000000"
                  ),
                  CompoundTextProperties(
                    viewType = ViewType.COMPOUND_TEXT,
                    primaryText = "Column #2",
                    primaryTextColor = "#000000"
                  ),
                  CompoundTextProperties(
                    viewType = ViewType.COMPOUND_TEXT,
                    primaryText = "Column #3",
                    primaryTextColor = "#000000"
                  )
                )
            ),
            ViewGroupProperties(
              viewType = ViewType.ROW,
              children =
                listOf(
                  CompoundTextProperties(
                    viewType = ViewType.COMPOUND_TEXT,
                    primaryText = "TB",
                    primaryTextColor = "#5A5A5A",
                    secondaryText = "Row 1",
                    secondaryTextColor = "#1DB11B"
                  ),
                  CompoundTextProperties(
                    viewType = ViewType.COMPOUND_TEXT,
                    primaryText = "HIV",
                    primaryTextColor = "#5A5A5A",
                    secondaryText = "Row 2",
                    secondaryTextColor = "#FF333F"
                  )
                )
            )
          )
      )
    )

  Column {
    RegisterCard(
      registerCardViewProperties = registerCardViewProperties,
      registerCardData = RegisterCardData(ResourceData(Patient()), emptyMap()),
      onCardClick = {}
    )
  }
}
