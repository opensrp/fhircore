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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
const val NUMBER_OF_ICONS_DISPLAYED = 2

@Composable
fun ServiceCard(
  modifier: Modifier = Modifier,
  serviceCardProperties: ServiceCardProperties,
  resourceData: ResourceData,
  navController: NavController,
  decodeImage: ((String) -> Bitmap?)?,
) {
  val serviceMemberIconsTint = serviceCardProperties.serviceMemberIconsTint.parseColor()
  if (serviceCardProperties.showVerticalDivider) {
    Row(
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
      modifier =
        modifier
          .height(IntrinsicSize.Min)
          .fillMaxWidth()
          .conditional(
            serviceCardProperties.clickable.toBoolean(),
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
      // Render details 70% of screen
      RenderDetails(
        weight = 0.7f,
        details = serviceCardProperties.details,
        serviceMemberIcons = serviceCardProperties.serviceMemberIcons,
        serviceMemberIconsTint = serviceMemberIconsTint,
        navController = navController,
        resourceData = resourceData,
      )
      Divider(
        modifier = modifier.fillMaxHeight().width(1.dp).testTag(DIVIDER_TEST_TAG),
        thickness = 0.5.dp,
        color = DividerColor,
      )
      // Render action buttons 30% of screen
      RenderActionButtons(
        weight = 0.3f,
        serviceCardProperties = serviceCardProperties,
        navController = navController,
        resourceData = resourceData,
        decodeImage = decodeImage,
      )
    }
  } else {
    Row(
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
      modifier =
        modifier
          .fillMaxWidth()
          .padding(vertical = 16.dp)
          .conditional(
            serviceCardProperties.clickable.toBoolean(),
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
      // Render details 55% of screen
      RenderDetails(
        weight = 0.55f,
        details = serviceCardProperties.details,
        serviceMemberIcons = serviceCardProperties.serviceMemberIcons,
        serviceMemberIconsTint = serviceMemberIconsTint,
        navController = navController,
        resourceData = resourceData,
      )
      // Render action buttons 45% of screen
      RenderActionButtons(
        weight = 0.45f,
        serviceCardProperties = serviceCardProperties,
        navController = navController,
        resourceData = resourceData,
        decodeImage = decodeImage,
      )
    }
  }
}

@Composable
private fun RowScope.RenderDetails(
  weight: Float,
  details: List<CompoundTextProperties>,
  serviceMemberIcons: String?,
  serviceMemberIconsTint: Color,
  navController: NavController,
  resourceData: ResourceData,
) {
  val iconsSplit = serviceMemberIcons?.split(",")?.filter { it.isNotEmpty() } ?: listOf()
  val memberIcons = iconsSplit.map { it.capitalize().trim() }.take(NUMBER_OF_ICONS_DISPLAYED)
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier.weight(weight).padding(end = 10.dp).fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Column(
      verticalArrangement = Arrangement.Center,
      modifier =
        Modifier.padding(end = 8.dp).fillMaxWidth(if (iconsSplit.isNotEmpty()) 0.75f else 1f),
    ) {
      details.forEach {
        CompoundText(
          compoundTextProperties = it,
          resourceData = resourceData,
          navController = navController,
        )
      }
    }
    // Display N icons and counter if icons are more than N
    if (memberIcons.isNotEmpty()) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.wrapContentWidth(),
        horizontalArrangement = Arrangement.End,
      ) {
        memberIcons.forEach {
          if (it.isNotEmpty() && ServiceMemberIcon.entries.map { icon -> icon.name }.contains(it)) {
            Icon(
              painter = painterResource(id = ServiceMemberIcon.valueOf(it).icon),
              contentDescription = null,
              modifier = Modifier.size(18.dp).padding(0.dp),
              tint = serviceMemberIconsTint,
            )
          }
        }
        if (
          memberIcons.size == NUMBER_OF_ICONS_DISPLAYED &&
            iconsSplit.size > NUMBER_OF_ICONS_DISPLAYED
        ) {
          Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.clip(CircleShape).size(22.dp).background(DefaultColor.copy(0.1f)),
          ) {
            Text(
              text = "+${iconsSplit.size - NUMBER_OF_ICONS_DISPLAYED}",
              fontSize = 10.sp,
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
}

@Composable
private fun RowScope.RenderActionButtons(
  weight: Float,
  serviceCardProperties: ServiceCardProperties,
  navController: NavController,
  resourceData: ResourceData,
  decodeImage: ((String) -> Bitmap?)?,
) {
  Box(modifier = Modifier.weight(weight).padding(start = 6.dp)) {
    if (serviceCardProperties.serviceButton != null || serviceCardProperties.services != null) {
      if (
        serviceCardProperties.serviceButton != null &&
          serviceCardProperties.serviceButton!!.visible.toBoolean()
      ) {
        when (serviceCardProperties.serviceButton!!.buttonType) {
          ButtonType.TINY,
          ButtonType.MEDIUM, -> {
            Column(
              horizontalAlignment = Alignment.End,
            ) {
              ActionableButton(
                buttonProperties = serviceCardProperties.serviceButton!!,
                resourceData = resourceData,
                navController = navController,
                decodeImage = decodeImage,
              )
            }
          }
          else -> {
            Box(contentAlignment = Alignment.Center) {
              BigServiceButton(
                modifier = Modifier,
                buttonProperties = serviceCardProperties.serviceButton!!,
                navController = navController,
                resourceData = resourceData,
              )
            }
          }
        }
      } else if (serviceCardProperties.services?.isNotEmpty() == true) {
        Box(contentAlignment = Alignment.Center) {
          Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            serviceCardProperties.services?.forEach { buttonProperties ->
              ActionableButton(
                buttonProperties = buttonProperties,
                resourceData = resourceData,
                navController = navController,
                decodeImage = decodeImage,
              )
            }
          }
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
  val contentColor = remember { statusColor.copy(alpha = 0.8f) }
  val buttonClickable = buttonProperties.clickable.toBoolean()

  Column(
    modifier =
      modifier
        .width(136.dp)
        .height(80.dp)
        .padding(top = 8.dp, end = 8.dp, bottom = 8.dp)
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
        )
        .clickable {
          if (isButtonEnabled && (status == ServiceStatus.DUE.name || buttonClickable)) {
            buttonProperties.actions.handleClickEvent(
              navController = navController,
              resourceData = resourceData,
            )
          }
        },
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    if (status == ServiceStatus.COMPLETED.name) {
      Icon(
        modifier = modifier.size(buttonProperties.statusIconSize.dp),
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
      fontSize = (buttonProperties.fontSize - 2).sp,
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
      decodeImage = null,
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
      decodeImage = null,
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
      decodeImage = null,
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
                    primaryText = "Important Due household service from Past",
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
      decodeImage = null,
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
      decodeImage = null,
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
                    primaryText = "John Njoroge Mwangi, M",
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
      decodeImage = null,
    )
  }
}

@PreviewWithBackgroundExcludeGenerated
@Composable
private fun ServiceCardServiceWithTinyServiceButtonPreview() {
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
                    primaryText = "Nelson Mandela, M",
                    primaryTextColor = "#000000",
                  ),
                  CompoundTextProperties(
                    viewType = ViewType.COMPOUND_TEXT,
                    primaryText = "Last visited yesterday",
                    primaryTextColor = "#5A5A5A",
                  ),
                ),
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
      decodeImage = null,
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
      decodeImage = null,
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
              showVerticalDivider = true,
              serviceButton =
                ButtonProperties(
                  status = ServiceStatus.DUE.name,
                  text = "ANC Visit",
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
      decodeImage = null,
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
                    primaryText = "A very long name. Lorem Ipsum Blah blah!",
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
              serviceMemberIcons = "PREGNANT_WOMAN",
              services =
                listOf(
                  ButtonProperties(
                    visible = "true",
                    status = ServiceStatus.COMPLETED.name,
                    text = "Pregnancy Outcome",
                    buttonType = ButtonType.MEDIUM,
                  ),
                  ButtonProperties(
                    visible = "true",
                    status = ServiceStatus.OVERDUE.name,
                    text = "ANC Visit 2",
                    buttonType = ButtonType.MEDIUM,
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
      decodeImage = null,
    )
  }
}
