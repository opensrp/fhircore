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

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.configuration.view.ButtonProperties
import org.smartregister.fhircore.engine.configuration.view.ButtonType
import org.smartregister.fhircore.engine.configuration.view.ColumnProperties
import org.smartregister.fhircore.engine.configuration.view.CompoundTextProperties
import org.smartregister.fhircore.engine.configuration.view.ServiceCardProperties
import org.smartregister.fhircore.engine.configuration.view.ViewProperties
import org.smartregister.fhircore.engine.domain.model.ResourceData
import org.smartregister.fhircore.engine.domain.model.ServiceMemberIcon
import org.smartregister.fhircore.engine.domain.model.ServiceStatus
import org.smartregister.fhircore.engine.domain.model.ViewType
import org.smartregister.fhircore.engine.ui.theme.DefaultColor
import org.smartregister.fhircore.engine.ui.theme.DividerColor
import org.smartregister.fhircore.engine.ui.theme.SuccessColor
import org.smartregister.fhircore.engine.util.annotation.PreviewWithBackgroundExcludeGenerated
import org.smartregister.fhircore.engine.util.extension.parseColor
import org.smartregister.fhircore.quest.util.extensions.conditional
import org.smartregister.fhircore.quest.util.extensions.handleClickEvent
import org.smartregister.p2p.utils.capitalize

const val DIVIDER_TEST_TAG = "dividerTestTag"

@Composable
fun ServiceCard(
  modifier: Modifier = Modifier,
  serviceCardProperties: ServiceCardProperties,
  resourceData: ResourceData,
  navController: NavController,
) {
  val serviceCardClickable = serviceCardProperties.clickable.toBoolean()
  Row(
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
    modifier = modifier.height(IntrinsicSize.Min),
  ) {
    // Show service card details in a column layout (occupies 75% of row width)
    // Display optional service member icons
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
      modifier =
        modifier
          .padding(top = 12.dp, bottom = 12.dp)
          .conditional(
            serviceCardProperties.serviceButton == null &&
              serviceCardProperties.services.isNullOrEmpty(),
            { fillMaxWidth() },
            { weight(if (serviceCardProperties.showVerticalDivider) 0.7f else 0.5f) },
          )
          .conditional(
            serviceCardClickable,
            {
              clickable {
                serviceCardProperties.actions.handleClickEvent(
                  navController = navController,
                  resourceData = resourceData,
                )
              }
            },
          ),
    ) {
      Column(
        modifier =
          modifier
            .wrapContentWidth(Alignment.Start)
            .weight(if (serviceCardProperties.showVerticalDivider) 0.7f else 1f),
        verticalArrangement = Arrangement.Center,
      ) {
        serviceCardProperties.details.forEach {
          CompoundText(
            compoundTextProperties = it,
            resourceData = resourceData,
            navController = navController,
          )
        }
      }
      if (serviceCardProperties.showVerticalDivider) {
        ServiceMemberIcons(
          modifier = modifier.wrapContentWidth(Alignment.End).weight(0.3f),
          serviceMemberIcons = serviceCardProperties.serviceMemberIcons,
        )
      }
    }

    // When divider is displayed member icons will not show
    if (serviceCardProperties.showVerticalDivider) {
      Divider(
        modifier = modifier.fillMaxHeight().width(1.dp).testTag(DIVIDER_TEST_TAG),
        thickness = 0.5.dp,
        color = DividerColor,
      )
    } else {
      ServiceMemberIcons(
        serviceMemberIcons =
          serviceCardProperties.serviceMemberIcons?.replace("\\s+".toRegex(), ""),
      )
    }

    // Show action button (occupies 25% of the row width)
    Box(
      modifier =
        modifier
          .weight(if (serviceCardProperties.showVerticalDivider) 0.3f else 0.4f)
          .padding(top = 12.dp, bottom = 12.dp),
      contentAlignment = Alignment.Center,
    ) {
      // Service card visibility can be determined dynamically e.g. only display when task is due
      if ((serviceCardProperties.serviceButton != null || serviceCardProperties.services != null)) {
        if (
          serviceCardProperties.serviceButton != null &&
            serviceCardProperties.serviceButton!!.visible.toBoolean()
        ) {
          if (serviceCardProperties.serviceButton!!.smallSized) {
            Column {
              ActionableButton(
                buttonProperties =
                  serviceCardProperties.serviceButton!!.copy(buttonType = ButtonType.TINY),
                navController = navController,
                resourceData = resourceData,
              )
            }
          } else {
            BigServiceButton(
              modifier = modifier,
              buttonProperties = serviceCardProperties.serviceButton!!,
              navController = navController,
              resourceData = resourceData,
            )
          }
        } else if (serviceCardProperties.services?.isNotEmpty() == true) {
          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            serviceCardProperties.services?.forEach { buttonProperties ->
              ActionableButton(
                buttonProperties = buttonProperties.copy(buttonType = ButtonType.TINY),
                navController = navController,
                resourceData = resourceData,
              )
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
  val iconsSplit = serviceMemberIcons?.split(",") ?: listOf()
  val twoMemberIcons = iconsSplit.map { it.capitalize().trim() }.take(2)
  if (twoMemberIcons.isNotEmpty()) {
    Row(modifier.padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
      twoMemberIcons.forEach {
        if (it.isNotEmpty() && ServiceMemberIcon.values().map { icon -> icon.name }.contains(it)) {
          Icon(
            painter = painterResource(id = ServiceMemberIcon.valueOf(it).icon),
            contentDescription = null,
            modifier = modifier.size(20.dp).padding(0.dp),
            tint = Color.Unspecified,
          )
        }
      }
      if (twoMemberIcons.size == 2 && iconsSplit.size > 2) {
        Box(
          contentAlignment = Alignment.Center,
          modifier = Modifier.clip(CircleShape).size(24.dp).background(DefaultColor.copy(0.1f)),
        ) {
          Text(
            modifier = Modifier.fillMaxWidth(),
            text = "+${iconsSplit.size - 2}",
            fontSize = 12.sp,
            color = Color.DarkGray,
            softWrap = false,
            maxLines = 1,
            textAlign = TextAlign.Center,
          )
        }
      }
    }
  }
}

@Composable
private fun BigServiceButton(
  modifier: Modifier = Modifier,
  buttonProperties: ButtonProperties,
  navController: NavController,
  resourceData: ResourceData,
) {
  val status = buttonProperties.status
  val isButtonEnabled = buttonProperties.enabled.toBoolean()
  val backgroundColor = buttonProperties.backgroundColor
  val statusColor = buttonProperties.statusColor(resourceData.computedValuesMap)
  val contentColor = remember { statusColor.copy(alpha = 0.85f) }
  val buttonClickable = buttonProperties.clickable.toBoolean()

  Column(
    modifier =
      modifier
        .clickable {
          if (isButtonEnabled && (status == ServiceStatus.DUE.name || buttonClickable)) {
            buttonProperties.actions.handleClickEvent(
              navController = navController,
              resourceData = resourceData,
            )
          }
        }
        .width(140.dp)
        .height(80.dp)
        .padding(8.dp)
        .clip(RoundedCornerShape(4.dp))
        .border(
          width = if (status == ServiceStatus.DUE.name) 1.dp else 0.dp,
          color = if (status == ServiceStatus.DUE.name) contentColor else Color.Unspecified,
          shape = RoundedCornerShape(4.dp),
        )
        .background(
          if (status == ServiceStatus.OVERDUE.name) {
            contentColor
          } else if (backgroundColor != Color.Unspecified.toString()) {
            backgroundColor.parseColor()
          } else {
            Color.Unspecified
          },
        ),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    if (status == ServiceStatus.COMPLETED.name) {
      Icon(
        modifier = modifier.size(16.dp),
        imageVector = Icons.Filled.Check,
        contentDescription = null,
        tint =
          when (status) {
            ServiceStatus.COMPLETED.name -> SuccessColor.copy(alpha = 0.9f)
            else -> statusColor.copy(alpha = 0.9f)
          },
      )
    }
    Text(
      text = buttonProperties.text ?: "",
      color = if (status == ServiceStatus.OVERDUE.name) Color.White else contentColor,
      textAlign = TextAlign.Center,
      fontSize = buttonProperties.fontSize.sp,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

@PreviewWithBackgroundExcludeGenerated
@Composable
private fun ServiceCardServiceOverduePreview() {
  val viewProperties =
    listOf<ViewProperties>(
      ColumnProperties(
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
                    secondaryTextColor = "#555AAA",
                  ),
                  CompoundTextProperties(
                    viewType = ViewType.COMPOUND_TEXT,
                    primaryText = "Last visited yesterday",
                    primaryTextColor = "#5A5A5A",
                  ),
                ),
              serviceMemberIcons = "CHILD",
              showVerticalDivider = true,
              serviceButton =
                ButtonProperties(
                  visible = "true",
                  status = ServiceStatus.OVERDUE.name,
                  text = "1",
                  buttonType = ButtonType.BIG,
                ),
            ),
          ),
      ),
    )

  Column(modifier = Modifier.padding(horizontal = 16.dp)) {
    ViewRenderer(
      viewProperties = viewProperties,
      resourceData = ResourceData("id", ResourceType.Patient, emptyMap()),
      navController = rememberNavController(),
    )
  }
}

@PreviewWithBackgroundExcludeGenerated
@Composable
private fun ServiceCardServiceOverdueWithBackgroundColorPreview() {
  val viewProperties =
    listOf<ViewProperties>(
      ColumnProperties(
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
                    secondaryTextColor = "#555AAA",
                  ),
                  CompoundTextProperties(
                    viewType = ViewType.COMPOUND_TEXT,
                    primaryText = "Last visited yesterday",
                    primaryTextColor = "#5A5A5A",
                  ),
                ),
              serviceMemberIcons = "CHILD",
              showVerticalDivider = true,
              serviceButton =
                ButtonProperties(
                  visible = "true",
                  status = "",
                  backgroundColor = "#000000",
                  text = "1",
                  buttonType = ButtonType.BIG,
                ),
            ),
          ),
      ),
    )

  Column(modifier = Modifier.padding(horizontal = 16.dp)) {
    ViewRenderer(
      viewProperties = viewProperties,
      resourceData = ResourceData("id", ResourceType.Patient, emptyMap()),
      navController = rememberNavController(),
    )
  }
}

@PreviewWithBackgroundExcludeGenerated
@Composable
private fun ServiceCardServiceOverdueWithNoBackgroundColorAndStatusPreview() {
  val viewProperties =
    listOf<ViewProperties>(
      ColumnProperties(
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
                    secondaryTextColor = "#555AAA",
                  ),
                  CompoundTextProperties(
                    viewType = ViewType.COMPOUND_TEXT,
                    primaryText = "Last visited yesterday",
                    primaryTextColor = "#5A5A5A",
                  ),
                ),
              serviceMemberIcons = "CHILD",
              showVerticalDivider = true,
              serviceButton =
                ButtonProperties(
                  visible = "true",
                  status = "",
                  backgroundColor = "",
                  text = "1",
                  buttonType = ButtonType.BIG,
                ),
            ),
          ),
      ),
    )

  Column(modifier = Modifier.padding(horizontal = 16.dp)) {
    ViewRenderer(
      viewProperties = viewProperties,
      resourceData = ResourceData("id", ResourceType.Patient, emptyMap()),
      navController = rememberNavController(),
    )
  }
}

@PreviewWithBackgroundExcludeGenerated
@Composable
private fun ServiceCardServiceDuePreview() {
  val viewProperties =
    listOf<ViewProperties>(
      ColumnProperties(
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
                    secondaryTextColor = "#555AAA",
                  ),
                  CompoundTextProperties(
                    viewType = ViewType.COMPOUND_TEXT,
                    primaryText = "Last visited yesterday",
                    primaryTextColor = "#5A5A5A",
                  ),
                ),
              serviceMemberIcons =
                "CHILD,PREGNANT_WOMAN,CHILD,CHILD,PREGNANT_WOMAN,CHILD,CHILD,CHILD,CHILD,CHILD,CHILD,CHILD,CHILD,CHILD,CHILD",
              showVerticalDivider = true,
              serviceButton =
                ButtonProperties(
                  visible = "true",
                  status = ServiceStatus.DUE.name,
                  text = "Issue Bed net",
                  buttonType = ButtonType.BIG,
                ),
            ),
          ),
      ),
    )

  Column(modifier = Modifier.padding(horizontal = 16.dp)) {
    ViewRenderer(
      viewProperties = viewProperties,
      resourceData = ResourceData("id", ResourceType.Patient, emptyMap()),
      navController = rememberNavController(),
    )
  }
}

@PreviewWithBackgroundExcludeGenerated
@Composable
private fun ServiceCardServiceUpcomingPreview() {
  val viewProperties =
    listOf<ViewProperties>(
      ColumnProperties(
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
                    secondaryTextColor = "#555AAA",
                  ),
                  CompoundTextProperties(
                    viewType = ViewType.COMPOUND_TEXT,
                    primaryText = "Last visited yesterday",
                    primaryTextColor = "#5A5A5A",
                  ),
                ),
              serviceMemberIcons = "CHILD,CHILD,CHILD,CHILD",
              showVerticalDivider = true,
              serviceButton =
                ButtonProperties(
                  visible = "true",
                  status = ServiceStatus.UPCOMING.name,
                  text = "Next visit 09-10-2022",
                  buttonType = ButtonType.BIG,
                ),
            ),
          ),
      ),
    )

  Column(modifier = Modifier.padding(horizontal = 16.dp)) {
    ViewRenderer(
      viewProperties = viewProperties,
      resourceData = ResourceData("id", ResourceType.Patient, emptyMap()),
      navController = rememberNavController(),
    )
  }
}

@PreviewWithBackgroundExcludeGenerated
@Composable
private fun ServiceCardServiceFamilyMemberPreview() {
  val viewProperties =
    listOf<ViewProperties>(
      ColumnProperties(
        viewType = ViewType.COLUMN,
        children =
          listOf(
            ServiceCardProperties(
              viewType = ViewType.SERVICE_CARD,
              details =
                listOf(
                  CompoundTextProperties(
                    viewType = ViewType.COMPOUND_TEXT,
                    primaryText = "John Njoroge Mwangi, F",
                    primaryTextColor = "#000000",
                  ),
                ),
              serviceMemberIcons = "CHILD",
              showVerticalDivider = false,
            ),
          ),
      ),
    )

  Column(modifier = Modifier.padding(horizontal = 16.dp)) {
    ViewRenderer(
      viewProperties = viewProperties,
      resourceData = ResourceData("id", ResourceType.Patient, emptyMap()),
      navController = rememberNavController(),
    )
  }
}

@PreviewWithBackgroundExcludeGenerated
@Composable
private fun ServiceCardServiceCompletedPreview() {
  val viewProperties =
    listOf<ViewProperties>(
      ColumnProperties(
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
                    secondaryTextColor = "#555AAA",
                  ),
                  CompoundTextProperties(
                    viewType = ViewType.COMPOUND_TEXT,
                    primaryText = "Last visited yesterday",
                    primaryTextColor = "#5A5A5A",
                  ),
                ),
              showVerticalDivider = true,
              serviceButton =
                ButtonProperties(
                  visible = "true",
                  status = ServiceStatus.COMPLETED.name,
                  text = "Fully Vaccinated against COVID 19 virus",
                  buttonType = ButtonType.BIG,
                ),
            ),
          ),
      ),
    )

  Column(modifier = Modifier.padding(horizontal = 16.dp)) {
    ViewRenderer(
      viewProperties = viewProperties,
      resourceData = ResourceData("id", ResourceType.Patient, emptyMap()),
      navController = rememberNavController(),
    )
  }
}

@PreviewWithBackgroundExcludeGenerated
@Composable
private fun ServiceCardANCServiceDuePreview() {
  val viewProperties =
    listOf<ViewProperties>(
      ColumnProperties(
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
                    secondaryTextColor = "#555AAA",
                  ),
                ),
              serviceMemberIcons = "CHILD",
              showVerticalDivider = false,
              serviceButton =
                ButtonProperties(
                  status = ServiceStatus.DUE.name,
                  text = "ANC Visit",
                  buttonType = ButtonType.TINY,
                ),
            ),
          ),
      ),
    )

  Column(modifier = Modifier.padding(horizontal = 16.dp)) {
    ViewRenderer(
      viewProperties = viewProperties,
      resourceData = ResourceData("id", ResourceType.Patient, emptyMap()),
      navController = rememberNavController(),
    )
  }
}

@PreviewWithBackgroundExcludeGenerated
@Composable
private fun ServiceCardANCServiceOverduePreview() {
  val viewProperties =
    listOf<ViewProperties>(
      ColumnProperties(
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
                    secondaryTextColor = "#555AAA",
                  ),
                ),
              showVerticalDivider = false,
              services =
                listOf(
                  ButtonProperties(
                    visible = "true",
                    status = ServiceStatus.COMPLETED.name,
                    text = "Pregnancy Outcome 1",
                    buttonType = ButtonType.TINY,
                  ),
                  ButtonProperties(
                    visible = "true",
                    status = ServiceStatus.OVERDUE.name,
                    text = "ANC Visit 2",
                    buttonType = ButtonType.TINY,
                  ),
                ),
            ),
          ),
      ),
    )

  Column(modifier = Modifier.padding(horizontal = 16.dp)) {
    ViewRenderer(
      viewProperties = viewProperties,
      resourceData = ResourceData("id", ResourceType.Patient, emptyMap()),
      navController = rememberNavController(),
    )
  }
}
