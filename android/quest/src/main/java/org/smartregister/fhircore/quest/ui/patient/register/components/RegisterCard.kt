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
import androidx.compose.foundation.layout.fillMaxSize
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
import com.google.android.fhir.logicalId
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
import org.smartregister.fhircore.engine.util.extension.interpolate
import org.smartregister.fhircore.engine.util.extension.parseColor
import org.smartregister.p2p.utils.capitalize

/**
 * A register card is a configurable view component that renders views for every of the rows of the
 * register. A register card consumes the data provided via the [ResourceData] class. The views are
 * configured via the [RegisterCardConfig]. The card also has an [onCardClick] listener that
 * responds to the click events.
 *
 * The [ResourceData.computedValuesMap] provides a map of computed rules values that can now be used
 * in the view.
 *
 * Note that by default the register card is not rendered in a view group like a Column/Row. This is
 * to allow us to call the function recursively for nested view group layout. Therefore when using
 * the this layout, provide a parent layout (usually Row/Column) so that the views can be rendered
 * appropriately otherwise the generated view group will be rendered one on top of the other. See
 * the Previews samples in this files.
 */
@Composable
fun RegisterCard(
  modifier: Modifier = Modifier,
  registerCardViewProperties: List<RegisterCardViewProperties>,
  resourceData: ResourceData,
  onCardClick: (String) -> Unit
) {
  registerCardViewProperties.forEach { viewProperties ->
    // Render views recursively
    if (viewProperties is ViewGroupProperties) {
      if (viewProperties.children.isEmpty()) return
      when (viewProperties.viewType) {
        ViewType.COLUMN, ViewType.ROW ->
          RenderViewGroup(viewProperties, modifier, resourceData, onCardClick)
        else -> return
      }
    } else {
      RenderChildView(
        modifier = modifier,
        registerCardViewProperties = viewProperties,
        resourceData = resourceData,
        onCardClick = onCardClick
      )
    }
  }
}

@Composable
private fun RenderViewGroup(
  viewProperties: ViewGroupProperties,
  modifier: Modifier,
  resourceData: ResourceData,
  onCardClick: (String) -> Unit
) {
  viewProperties.children.forEach { childViewProperty ->
    if (childViewProperty is ViewGroupProperties) {
      if (childViewProperty.viewType == ViewType.COLUMN) {
        FlowColumn {
          RegisterCard(
            modifier = modifier,
            registerCardViewProperties = childViewProperty.children,
            resourceData = resourceData,
            onCardClick = onCardClick
          )
        }
      } else if (childViewProperty.viewType == ViewType.ROW) {
        FlowRow {
          RegisterCard(
            modifier = modifier,
            registerCardViewProperties = childViewProperty.children,
            resourceData = resourceData,
            onCardClick = onCardClick
          )
        }
      }
    }
    RenderChildView(
      modifier = modifier,
      registerCardViewProperties = childViewProperty,
      resourceData = resourceData,
      onCardClick = onCardClick
    )
  }
}

@Composable
private fun RenderChildView(
  modifier: Modifier = Modifier,
  registerCardViewProperties: RegisterCardViewProperties,
  resourceData: ResourceData,
  onCardClick: (String) -> Unit
) {
  when (registerCardViewProperties) {
    is CompoundTextProperties ->
      CompoundText(
        modifier = modifier,
        compoundTextProperties = registerCardViewProperties,
        computedValuesMap = resourceData.computedValuesMap,
      )
    is ServiceCardProperties ->
      ServiceCard(
        serviceCardProperties = registerCardViewProperties,
        resourceData = resourceData,
        onCardClick = onCardClick
      )
  }
}

@Composable
fun CompoundText(
  modifier: Modifier = Modifier,
  compoundTextProperties: CompoundTextProperties,
  computedValuesMap: Map<String, Any>
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = modifier.padding(bottom = 8.dp),
  ) {
    if (compoundTextProperties.primaryText != null) {
      Text(
        text = compoundTextProperties.primaryText!!.interpolate(computedValuesMap),
        color = compoundTextProperties.primaryTextColor.parseColor(),
        modifier = modifier.wrapContentWidth(Alignment.Start)
      )
    }
    if (compoundTextProperties.secondaryText != null) {
      // Separate the primary and secondary text
      Separator(separator = compoundTextProperties.separator ?: "-")

      Text(
        text = compoundTextProperties.secondaryText!!.interpolate(computedValuesMap),
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
  resourceData: ResourceData,
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
          .clickable { onCardClick(resourceData.baseResource.logicalId) }
          .padding(top = 24.dp, bottom = 24.dp)
          .weight(0.75f)
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
        serviceCardProperties.serviceMemberIcons
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

    // Show action button (occupies 25% of the row width)
    Box(
      modifier = modifier.weight(0.25f).padding(top = 24.dp, bottom = 24.dp),
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
            serviceButton = serviceCardProperties.serviceButton!!,
            computedValuesMap = resourceData.computedValuesMap
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
      text = serviceButton.text ?: "",
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
private fun BigServiceButton(
  modifier: Modifier = Modifier,
  serviceButton: ServiceButton,
  computedValuesMap: Map<String, Any>
) {
  val statusColor = serviceButton.statusColor()
  val contentColor = remember { statusColor.copy(alpha = 0.85f) }
  Column(
    modifier =
      modifier
        .fillMaxSize()
        .padding(8.dp)
        .clip(RoundedCornerShape(4.dp))
        .background(
          if (serviceButton.status == ServiceStatus.OVERDUE) contentColor else Color.Unspecified
        ),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    if (serviceButton.status == ServiceStatus.COMPLETED)
      Icon(imageVector = Icons.Filled.Check, contentDescription = null, tint = contentColor)
    Text(
      text = serviceButton.text?.interpolate(computedValuesMap) ?: "",
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

@Preview(showBackground = true)
@Composable
private fun CompoundTextNoSecondaryTextPreview() {
  Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
    CompoundText(
      compoundTextProperties =
        CompoundTextProperties(
          primaryText = "Full Name, Age",
          primaryTextColor = "#000000",
        ),
      computedValuesMap = emptyMap()
    )
    CompoundText(
      compoundTextProperties =
        CompoundTextProperties(
          primaryText = "Sex",
          primaryTextColor = "#5A5A5A",
        ),
      computedValuesMap = emptyMap()
    )
  }
}

@Preview(showBackground = true)
@Composable
private fun CompoundTextWithSecondaryTextPreview() {
  Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
    CompoundText(
      compoundTextProperties =
        CompoundTextProperties(
          primaryText = "Full Name, Sex, Age",
          primaryTextColor = "#000000",
        ),
      computedValuesMap = emptyMap()
    )
    CompoundText(
      compoundTextProperties =
        CompoundTextProperties(
          primaryText = "Last visited",
          primaryTextColor = "#5A5A5A",
          secondaryText = "G6PD status",
          separator = "-",
          secondaryTextColor = "#5A5A5A"
        ),
      computedValuesMap = emptyMap()
    )
  }
}

@Preview(showBackground = true)
@Composable
private fun RegisterCardServiceOverduePreview() {
  val registerCardViewProperties =
    listOf<RegisterCardViewProperties>(
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
                ServiceButton(
                  visible = true,
                  status = ServiceStatus.OVERDUE,
                  text = "1",
                  smallSized = false
                )
            )
          )
      )
    )

  Column(modifier = Modifier.padding(horizontal = 16.dp)) {
    RegisterCard(
      registerCardViewProperties = registerCardViewProperties,
      resourceData = ResourceData(Patient(), emptyMap(), emptyMap()),
      onCardClick = {}
    )
  }
}

@Preview(showBackground = true)
@Composable
private fun RegisterCardServiceDuePreview() {
  val registerCardViewProperties =
    listOf<RegisterCardViewProperties>(
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
                ServiceButton(
                  visible = true,
                  status = ServiceStatus.DUE,
                  text = "Issue Bed net",
                  smallSized = false
                )
            )
          )
      )
    )

  Column(modifier = Modifier.padding(horizontal = 16.dp)) {
    RegisterCard(
      registerCardViewProperties = registerCardViewProperties,
      resourceData = ResourceData(Patient(), emptyMap(), emptyMap()),
      onCardClick = {}
    )
  }
}

@Preview(showBackground = true)
@Composable
private fun RegisterCardServiceUpcomingPreview() {
  val registerCardViewProperties =
    listOf<RegisterCardViewProperties>(
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
                ServiceButton(
                  visible = true,
                  status = ServiceStatus.UPCOMING,
                  text = "Next visit 09-10-2022",
                  smallSized = false
                )
            )
          )
      )
    )

  Column(modifier = Modifier.padding(horizontal = 16.dp)) {
    RegisterCard(
      registerCardViewProperties = registerCardViewProperties,
      resourceData = ResourceData(Patient(), emptyMap(), emptyMap()),
      onCardClick = {}
    )
  }
}

@Preview(showBackground = true)
@Composable
private fun RegisterCardServiceCompletedPreview() {
  val registerCardViewProperties =
    listOf<RegisterCardViewProperties>(
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
                ServiceButton(
                  visible = true,
                  status = ServiceStatus.COMPLETED,
                  text = "Fully Vaccinated",
                  smallSized = false
                )
            )
          )
      )
    )

  Column(modifier = Modifier.padding(horizontal = 16.dp)) {
    RegisterCard(
      registerCardViewProperties = registerCardViewProperties,
      resourceData = ResourceData(Patient(), emptyMap(), emptyMap()),
      onCardClick = {}
    )
  }
}

@Preview(showBackground = true)
@Composable
private fun RegisterCardANCServiceDuePreview() {
  val registerCardViewProperties =
    listOf<RegisterCardViewProperties>(
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
                ServiceButton(
                  visible = true,
                  status = ServiceStatus.DUE,
                  text = "ANC Visit",
                  smallSized = true
                )
            )
          )
      )
    )

  Column(modifier = Modifier.padding(horizontal = 16.dp)) {
    RegisterCard(
      registerCardViewProperties = registerCardViewProperties,
      resourceData = ResourceData(Patient(), emptyMap(), emptyMap()),
      onCardClick = {}
    )
  }
}

@Preview(showBackground = true)
@Composable
private fun RegisterCardANCServiceOverduePreview() {
  val registerCardViewProperties =
    listOf<RegisterCardViewProperties>(
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
              serviceButton =
                ServiceButton(
                  visible = true,
                  status = ServiceStatus.OVERDUE,
                  text = "ANC Visit",
                  smallSized = true
                )
            )
          )
      )
    )

  Column(modifier = Modifier.padding(horizontal = 16.dp)) {
    RegisterCard(
      registerCardViewProperties = registerCardViewProperties,
      resourceData = ResourceData(Patient(), emptyMap(), emptyMap()),
      onCardClick = {}
    )
  }
}
