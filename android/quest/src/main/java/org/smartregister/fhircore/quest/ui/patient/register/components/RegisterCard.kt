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

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.flowlayout.FlowColumn
import com.google.accompanist.flowlayout.FlowRow
import org.hl7.fhir.r4.model.Patient
import org.smartregister.fhircore.engine.configuration.register.RegisterCardConfig
import org.smartregister.fhircore.engine.configuration.view.CompoundTextProperties
import org.smartregister.fhircore.engine.configuration.view.RegisterCardViewProperties
import org.smartregister.fhircore.engine.configuration.view.ServiceButton
import org.smartregister.fhircore.engine.configuration.view.ServiceCardProperties
import org.smartregister.fhircore.engine.configuration.view.ViewGroupProperties
import org.smartregister.fhircore.engine.domain.model.ResourceData
import org.smartregister.fhircore.engine.domain.model.ServiceMemberIcon
import org.smartregister.fhircore.engine.domain.model.ServiceStatus
import org.smartregister.fhircore.engine.domain.model.ViewType
import org.smartregister.fhircore.engine.ui.components.Separator
import org.smartregister.fhircore.engine.ui.theme.DangerColor
import org.smartregister.fhircore.engine.ui.theme.DefaultColor
import org.smartregister.fhircore.engine.ui.theme.DividerColor
import org.smartregister.fhircore.engine.ui.theme.InfoColor
import org.smartregister.fhircore.engine.ui.theme.SuccessColor
import org.smartregister.fhircore.engine.util.extension.parseColor
import org.smartregister.fhircore.quest.ui.shared.models.RegisterCardData
import org.smartregister.p2p.utils.capitalize

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
        registerCardData = registerCardData,
        onCardClick = onCardClick
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
      registerCardData = registerCardData,
      onCardClick = onCardClick
    )
  }
}

@Composable
private fun RenderChildView(
  modifier: Modifier = Modifier,
  registerCardViewProperties: RegisterCardViewProperties,
  registerCardData: RegisterCardData,
  onCardClick: (String) -> Unit
) {
  when (registerCardViewProperties) {
    is CompoundTextProperties ->
      CompoundText(
        compoundTextProperties = registerCardViewProperties,
        registerCardData = registerCardData,
        modifier = modifier
      )
    is ServiceCardProperties ->
      ServiceCard(
        serviceCardProperties = registerCardViewProperties,
        registerCardData = registerCardData,
        onCardClick = onCardClick
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

@Composable
fun ServiceCard(
  modifier: Modifier = Modifier,
  serviceCardProperties: ServiceCardProperties,
  registerCardData: RegisterCardData,
  onCardClick: (String) -> Unit
) {
  Row(
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
    modifier = modifier.height(IntrinsicSize.Min)
  ) {
    // Show service card details in a column layout (occupies 75% of row width)
    // Display optional service member icons
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
      modifier =
        modifier
          .clickable {
            /** TODO call card click listener */
          }
          .padding(start = 16.dp, top = 24.dp, bottom = 24.dp)
          .weight(0.75f)
    ) {
      Column {
        serviceCardProperties.details.forEach {
          CompoundText(compoundTextProperties = it, registerCardData = registerCardData)
        }
      }
      ServiceMemberIcons(modifier = modifier, serviceCardProperties.serviceMemberIcons)
    }

    // Display a vertical divider to separate service card details from action button
    if (serviceCardProperties.showVerticalDivider) {
      Divider(
        modifier = modifier.fillMaxHeight().width(1.dp),
        thickness = 1.dp,
        color = DividerColor
      )
    }

    // Show action button (occupies 25% of the row width)
    Box(
      modifier = modifier.weight(0.25f).padding(end = 16.dp, top = 24.dp, bottom = 24.dp),
      contentAlignment = Alignment.Center
    ) {
      // Service card visibility can be determined dynamically e.g. only display when task is due
      if (serviceCardProperties.serviceButton != null &&
          serviceCardProperties.serviceButton!!.visible == true
      ) {
        if (serviceCardProperties.serviceButton!!.smallSized) {
          SmallServiceButton(
            modifier = modifier,
            serviceButton = serviceCardProperties.serviceButton!!
          )
        } else {
          BigServiceButton(
            modifier = modifier,
            serviceButton = serviceCardProperties.serviceButton!!
          )
        }
      }
    }
  }
}

@Composable
private fun ServiceMemberIcons(modifier: Modifier = Modifier, serviceMemberIcons: String?) {
  // Count member icons only show and display counter of the rest
  val iconsSplit = remember { serviceMemberIcons?.split(",") } ?: listOf()
  val twoMemberIcons = remember { iconsSplit.onEach { it.capitalize().trim() }.take(2) }
  if (twoMemberIcons.isNotEmpty()) {
    Row(modifier.padding(horizontal = 8.dp)) {
      twoMemberIcons.forEach {
        if (it.isNotEmpty() && ServiceMemberIcon.values().map { icon -> icon.name }.contains(it))
          Icon(
            painter = painterResource(id = ServiceMemberIcon.valueOf(it).icon),
            contentDescription = null,
            modifier = modifier.size(20.dp).padding(0.dp),
            tint = Color.Unspecified
          )
      }
      if (twoMemberIcons.size == 2 && iconsSplit.size > 2) {
        Box(
          contentAlignment = Alignment.Center,
          modifier = modifier.clip(CircleShape).size(24.dp).background(DefaultColor.copy(0.1f))
        ) { Text(text = "+${iconsSplit.size - 2}", fontSize = 12.sp, color = Color.DarkGray) }
      }
    }
  }
}

@Composable
private fun SmallServiceButton(modifier: Modifier = Modifier, serviceButton: ServiceButton) {
  val statusColor = serviceButton.statusColor()
  val contentColor = remember { statusColor.copy(alpha = 0.85f) }
  Row(
    modifier =
      modifier
        .padding(horizontal = 8.dp)
        .clip(RoundedCornerShape(8.dp))
        .clickable { /*TODO Provide the given service*/}
        .background(color = statusColor.copy(alpha = 0.1f)),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Icon(
      imageVector = Icons.Filled.Add,
      contentDescription = null,
      tint = contentColor,
      modifier = modifier.size(16.dp).padding(horizontal = 1.dp)
    )
    Text(
      text = serviceButton.label ?: "",
      color = contentColor,
      fontSize = 12.sp,
      fontWeight = FontWeight.Bold,
      modifier = modifier.padding(4.dp).wrapContentHeight(Alignment.CenterVertically),
      overflow = TextOverflow.Visible,
      maxLines = 1
    )
  }
}

@Composable
private fun BigServiceButton(modifier: Modifier = Modifier, serviceButton: ServiceButton) {
  val statusColor = serviceButton.statusColor()
  val contentColor = remember { statusColor.copy(alpha = 0.85f) }
  Column(
    modifier =
      modifier.background(
        if (serviceButton.status == ServiceStatus.OVERDUE) contentColor else Color.Unspecified
      ),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    if (serviceButton.status == ServiceStatus.COMPLETED)
      Icon(imageVector = Icons.Filled.Check, contentDescription = null, tint = contentColor)
    Text(
      text = serviceButton.text ?: "",
      color = if (serviceButton.status == ServiceStatus.OVERDUE) Color.White else contentColor,
      textAlign = TextAlign.Center
    )
  }
}

@Composable
private fun ServiceButton.statusColor(): Color = remember {
  // Status color is determined from the service status
  when (this.status) {
    ServiceStatus.DUE -> InfoColor
    ServiceStatus.OVERDUE -> DangerColor
    ServiceStatus.UPCOMING -> DefaultColor
    ServiceStatus.COMPLETED -> SuccessColor
  }
}

@Composable @Preview(showBackground = true) fun ServiceCardPreview() {}

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
