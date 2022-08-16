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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.fhir.logicalId
import org.hl7.fhir.r4.model.Patient
import org.smartregister.fhircore.engine.configuration.view.ButtonProperties
import org.smartregister.fhircore.engine.configuration.view.CompoundTextProperties
import org.smartregister.fhircore.engine.configuration.view.ServiceCardProperties
import org.smartregister.fhircore.engine.configuration.view.ViewGroupProperties
import org.smartregister.fhircore.engine.configuration.view.ViewProperties
import org.smartregister.fhircore.engine.configuration.workflow.ActionTrigger
import org.smartregister.fhircore.engine.domain.model.ResourceData
import org.smartregister.fhircore.engine.domain.model.ServiceMemberIcon
import org.smartregister.fhircore.engine.domain.model.ServiceStatus
import org.smartregister.fhircore.engine.domain.model.ViewType
import org.smartregister.fhircore.engine.ui.components.ActionableButton
import org.smartregister.fhircore.engine.ui.components.statusColor
import org.smartregister.fhircore.engine.ui.theme.DefaultColor
import org.smartregister.fhircore.engine.ui.theme.DividerColor
import org.smartregister.fhircore.engine.util.extension.interpolate
import org.smartregister.fhircore.quest.ui.shared.models.ViewComponentEvent
import org.smartregister.p2p.utils.capitalize

@Composable
fun ServiceCard(
  modifier: Modifier = Modifier,
  serviceCardProperties: ServiceCardProperties,
  resourceData: ResourceData,
  onViewComponentClick: (ViewComponentEvent) -> Unit
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
            // Ensure the service card has a click action
            val profileId =
              serviceCardProperties.actions.find {
                it.trigger == ActionTrigger.ON_CLICK && !it.id.isNullOrEmpty()
              }
            profileId?.let {
              onViewComponentClick(
                ViewComponentEvent.ServiceCardClick(
                  profileId = it.id!!,
                  resourceId = resourceData.baseResource.logicalId
                )
              )
            }
          }
          .padding(top = 24.dp, bottom = 24.dp)
          .weight(0.65f)
    ) {
      Column(modifier = modifier.wrapContentWidth(Alignment.Start).weight(0.7f)) {
        serviceCardProperties.details.forEach {
          CompoundText(
            compoundTextProperties = it,
            computedValuesMap = resourceData.computedValuesMap
          )
        }
      }
      ServiceMemberIcons(
        modifier = modifier.wrapContentWidth(Alignment.End).weight(0.3f),
        serviceMemberIcons =
          serviceCardProperties.serviceMemberIcons?.interpolate(resourceData.computedValuesMap)
      )
    }

    // Display a vertical divider to separate service card details from action button
    if (serviceCardProperties.showVerticalDivider) {
      Divider(
        modifier = modifier.fillMaxHeight().width(1.dp),
        thickness = 1.dp,
        color = DividerColor
      )
    }

    // Show action button (occupies 35% of the row width)
    Box(
      modifier = modifier.weight(0.35f).padding(top = 24.dp, bottom = 24.dp),
      contentAlignment = Alignment.Center
    ) {
      // Service card visibility can be determined dynamically e.g. only display when task is due
      if ((serviceCardProperties.serviceButton != null || serviceCardProperties.services != null)) {
        if (
          serviceCardProperties.serviceButton != null &&
            serviceCardProperties.serviceButton!!.visible == true
        ) {
          if (serviceCardProperties.serviceButton!!.smallSized) {
            ActionableButton(
              modifier = modifier,
              buttonProperties = serviceCardProperties.serviceButton!!,
              onAction = {}
            )
          } else {
            BigServiceButton(
              modifier = modifier,
              buttonProperties = serviceCardProperties.serviceButton!!,
              computedValuesMap = resourceData.computedValuesMap,
            )
          }
        } else if (serviceCardProperties.services?.isNotEmpty() == true) {
          Column {
            serviceCardProperties.services?.forEach { buttonProperties ->
              ActionableButton(buttonProperties = buttonProperties, onAction = {})
              Spacer(modifier = modifier.height(8.dp))
            }
          }
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
private fun BigServiceButton(
  modifier: Modifier = Modifier,
  buttonProperties: ButtonProperties,
  computedValuesMap: Map<String, Any>
) {
  val statusColor = buttonProperties.statusColor()
  val contentColor = remember { statusColor.copy(alpha = 0.85f) }
  val extractedStatus = remember { ServiceStatus.valueOf(buttonProperties.status) }

  Column(
    modifier =
      modifier
        .fillMaxSize()
        .padding(4.dp)
        .clip(RoundedCornerShape(4.dp))
        .background(
          if (extractedStatus == ServiceStatus.OVERDUE) contentColor else Color.Unspecified
        ),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Column(
      verticalArrangement = Arrangement.Center,
      modifier = modifier.fillMaxSize().padding(8.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      if (extractedStatus == ServiceStatus.COMPLETED)
        Icon(imageVector = Icons.Filled.Check, contentDescription = null, tint = contentColor)
      Text(
        text = buttonProperties.text?.interpolate(computedValuesMap) ?: "",
        color = if (extractedStatus == ServiceStatus.OVERDUE) Color.White else contentColor,
        textAlign = TextAlign.Center,
        fontSize = buttonProperties.fontSize.sp
      )
    }
  }
}

@Preview(showBackground = true)
@Composable
private fun ServiceCardServiceOverduePreview() {
  val viewProperties =
    listOf<ViewProperties>(
      ViewGroupProperties(
        viewType = ViewType.COLUMN,
        children =
          listOf(
            ServiceCardProperties(
              viewType = ViewType.SERVICE_CARD,
              details =
                listOf(
                  CompoundTextProperties(
                    viewType = ViewType.COMPOUND_TEXT,
                    primaryText = "Overdue household service",
                    primaryTextColor = "#000000",
                  ),
                  CompoundTextProperties(
                    viewType = ViewType.COMPOUND_TEXT,
                    primaryText = "Town/Village",
                    primaryTextColor = "#5A5A5A",
                    secondaryText = "HH No.",
                    secondaryTextColor = "#555AAA"
                  ),
                  CompoundTextProperties(
                    viewType = ViewType.COMPOUND_TEXT,
                    primaryText = "Last visited yesterday",
                    primaryTextColor = "#5A5A5A",
                  )
                ),
              serviceMemberIcons = "CHILD",
              showVerticalDivider = true,
              serviceButton =
                ButtonProperties(
                  visible = true,
                  status = ServiceStatus.OVERDUE.name,
                  text = "1",
                  smallSized = false
                )
            )
          )
      )
    )

  Column(modifier = Modifier.padding(horizontal = 16.dp)) {
    ViewRenderer(
      viewProperties = viewProperties,
      resourceData = ResourceData(Patient(), emptyMap(), emptyMap()),
      onViewComponentClick = {},
    )
  }
}

@Preview(showBackground = true)
@Composable
private fun ServiceCardServiceDuePreview() {
  val viewProperties =
    listOf<ViewProperties>(
      ViewGroupProperties(
        viewType = ViewType.COLUMN,
        children =
          listOf(
            ServiceCardProperties(
              viewType = ViewType.SERVICE_CARD,
              details =
                listOf(
                  CompoundTextProperties(
                    viewType = ViewType.COMPOUND_TEXT,
                    primaryText = "Due household services",
                    primaryTextColor = "#000000",
                  ),
                  CompoundTextProperties(
                    viewType = ViewType.COMPOUND_TEXT,
                    primaryText = "Town/Village",
                    primaryTextColor = "#5A5A5A",
                    secondaryText = "HH No.",
                    secondaryTextColor = "#555AAA"
                  ),
                  CompoundTextProperties(
                    viewType = ViewType.COMPOUND_TEXT,
                    primaryText = "Last visited yesterday",
                    primaryTextColor = "#5A5A5A",
                  )
                ),
              serviceMemberIcons = "CHILD,PREGNANT_WOMAN,CHILD,CHILD",
              showVerticalDivider = true,
              serviceButton =
                ButtonProperties(
                  visible = true,
                  status = ServiceStatus.DUE.name,
                  text = "Issue Bed net",
                  smallSized = false
                )
            )
          )
      )
    )

  Column(modifier = Modifier.padding(horizontal = 16.dp)) {
    ViewRenderer(
      viewProperties = viewProperties,
      resourceData = ResourceData(Patient(), emptyMap(), emptyMap()),
      onViewComponentClick = {},
    )
  }
}

@Preview(showBackground = true)
@Composable
private fun ServiceCardServiceUpcomingPreview() {
  val viewProperties =
    listOf<ViewProperties>(
      ViewGroupProperties(
        viewType = ViewType.COLUMN,
        children =
          listOf(
            ServiceCardProperties(
              viewType = ViewType.SERVICE_CARD,
              details =
                listOf(
                  CompoundTextProperties(
                    viewType = ViewType.COMPOUND_TEXT,
                    primaryText = "Upcoming household service",
                    primaryTextColor = "#000000",
                  ),
                  CompoundTextProperties(
                    viewType = ViewType.COMPOUND_TEXT,
                    primaryText = "Town/Village",
                    primaryTextColor = "#5A5A5A",
                    secondaryText = "HH No.",
                    secondaryTextColor = "#555AAA"
                  ),
                  CompoundTextProperties(
                    viewType = ViewType.COMPOUND_TEXT,
                    primaryText = "Last visited yesterday",
                    primaryTextColor = "#5A5A5A",
                  )
                ),
              serviceMemberIcons = "CHILD,CHILD,CHILD,CHILD",
              showVerticalDivider = true,
              serviceButton =
                ButtonProperties(
                  visible = true,
                  status = ServiceStatus.UPCOMING.name,
                  text = "Next visit 09-10-2022",
                  smallSized = false
                )
            )
          )
      )
    )

  Column(modifier = Modifier.padding(horizontal = 16.dp)) {
    ViewRenderer(
      viewProperties = viewProperties,
      resourceData = ResourceData(Patient(), emptyMap(), emptyMap()),
      onViewComponentClick = {},
    )
  }
}

@Preview(showBackground = true)
@Composable
private fun ServiceCardServiceCompletedPreview() {
  val viewProperties =
    listOf<ViewProperties>(
      ViewGroupProperties(
        viewType = ViewType.COLUMN,
        children =
          listOf(
            ServiceCardProperties(
              viewType = ViewType.SERVICE_CARD,
              details =
                listOf(
                  CompoundTextProperties(
                    viewType = ViewType.COMPOUND_TEXT,
                    primaryText = "Fully vaccinated household",
                    primaryTextColor = "#000000",
                  ),
                  CompoundTextProperties(
                    viewType = ViewType.COMPOUND_TEXT,
                    primaryText = "Town/Village",
                    primaryTextColor = "#5A5A5A",
                    secondaryText = "HH No.",
                    secondaryTextColor = "#555AAA"
                  ),
                  CompoundTextProperties(
                    viewType = ViewType.COMPOUND_TEXT,
                    primaryText = "Last visited yesterday",
                    primaryTextColor = "#5A5A5A",
                  )
                ),
              showVerticalDivider = true,
              serviceButton =
                ButtonProperties(
                  visible = true,
                  status = ServiceStatus.COMPLETED.name,
                  text = "Fully Vaccinated",
                  smallSized = false
                )
            )
          )
      )
    )

  Column(modifier = Modifier.padding(horizontal = 16.dp)) {
    ViewRenderer(
      viewProperties = viewProperties,
      resourceData = ResourceData(Patient(), emptyMap(), emptyMap()),
      onViewComponentClick = {},
    )
  }
}

@Preview(showBackground = true)
@Composable
private fun ServiceCardANCServiceDuePreview() {
  val viewProperties =
    listOf<ViewProperties>(
      ViewGroupProperties(
        viewType = ViewType.COLUMN,
        children =
          listOf(
            ServiceCardProperties(
              viewType = ViewType.SERVICE_CARD,
              details =
                listOf(
                  CompoundTextProperties(
                    viewType = ViewType.COMPOUND_TEXT,
                    primaryText = "ANC service due",
                    primaryTextColor = "#000000",
                  ),
                  CompoundTextProperties(
                    viewType = ViewType.COMPOUND_TEXT,
                    primaryText = "EDD",
                    primaryTextColor = "#5A5A5A",
                    secondaryText = "29-10-2022",
                    secondaryTextColor = "#555AAA"
                  )
                ),
              showVerticalDivider = false,
              serviceButton =
                ButtonProperties(
                  visible = true,
                  status = ServiceStatus.DUE.name,
                  text = "ANC Visit",
                  smallSized = true
                )
            )
          )
      )
    )

  Column(modifier = Modifier.padding(horizontal = 16.dp)) {
    ViewRenderer(
      viewProperties = viewProperties,
      resourceData = ResourceData(Patient(), emptyMap(), emptyMap()),
      onViewComponentClick = {},
    )
  }
}

@Preview(showBackground = true)
@Composable
private fun ServiceCardANCServiceOverduePreview() {
  val viewProperties =
    listOf<ViewProperties>(
      ViewGroupProperties(
        viewType = ViewType.COLUMN,
        children =
          listOf(
            ServiceCardProperties(
              viewType = ViewType.SERVICE_CARD,
              details =
                listOf(
                  CompoundTextProperties(
                    viewType = ViewType.COMPOUND_TEXT,
                    primaryText = "ANC service overdue",
                    primaryTextColor = "#000000",
                  ),
                  CompoundTextProperties(
                    viewType = ViewType.COMPOUND_TEXT,
                    primaryText = "EDD",
                    primaryTextColor = "#5A5A5A",
                    secondaryText = "29-10-2022",
                    secondaryTextColor = "#555AAA"
                  )
                ),
              showVerticalDivider = false,
              services =
                listOf(
                  ButtonProperties(
                    visible = true,
                    status = ServiceStatus.COMPLETED.name,
                    text = "Pregnancy Outcome 1",
                    smallSized = true
                  ),
                  ButtonProperties(
                    visible = true,
                    status = ServiceStatus.OVERDUE.name,
                    text = "ANC Visit 2",
                    smallSized = true
                  )
                )
            )
          )
      )
    )

  Column(modifier = Modifier.padding(horizontal = 16.dp)) {
    ViewRenderer(
      viewProperties = viewProperties,
      resourceData = ResourceData(Patient(), emptyMap(), emptyMap()),
      onViewComponentClick = {},
    )
  }
}
