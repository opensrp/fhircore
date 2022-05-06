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
import org.smartregister.fhircore.engine.ui.components.Separator
import org.smartregister.fhircore.engine.ui.theme.DefaultColor
import org.smartregister.fhircore.engine.ui.theme.DividerColor
import org.smartregister.fhircore.engine.ui.theme.InfoColor
import org.smartregister.fhircore.engine.ui.theme.OverdueColor
import org.smartregister.fhircore.quest.R
import org.smartregister.fhircore.quest.ui.shared.models.RegisterViewData
import org.smartregister.fhircore.quest.ui.shared.models.ServiceMember

@Composable
fun RegisterListRow(
  modifier: Modifier = Modifier,
  registerViewData: RegisterViewData,
  onRowClick: (String) -> Unit
) {
  Row(
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
    modifier = modifier.fillMaxWidth().height(IntrinsicSize.Min)
  ) {
    Column(
      modifier =
        modifier
          .clickable { onRowClick(registerViewData.logicalId) }
          .weight(0.75f)
          .padding(horizontal = 16.dp, vertical = 28.dp)
    ) {
      if (registerViewData.serviceButtonActionable) {
        Row(
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
          modifier = modifier.fillMaxWidth()
        ) {
          Column(modifier = modifier.wrapContentWidth(Alignment.Start)) {
            Text(text = registerViewData.title)
            RegisterListStatus(registerViewData, modifier)
          }
        }
      } else {
        Text(text = registerViewData.title)
        Row(
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
          modifier = modifier.fillMaxWidth()
        ) {
          Column(modifier = modifier.wrapContentWidth(Alignment.Start).weight(0.7f)) {
            if (!registerViewData.subtitle.isNullOrEmpty()) {
              Text(
                text = registerViewData.subtitle,
                color = DefaultColor,
                modifier = modifier.padding(top = 4.dp)
              )
            }
            RegisterListStatus(registerViewData, modifier)
          }
          ServiceMemberIcons(
            registerViewData,
            modifier = modifier.wrapContentWidth(Alignment.End).weight(0.3f)
          )
        }
      }
    }
    if (registerViewData.showDivider) {
      Divider(
        modifier =
          modifier
            .fillMaxHeight()
            .padding(horizontal = 4.dp)
            .width(1.dp)
            .background(color = DividerColor)
      )
    }
    Box(modifier = modifier.weight(0.25f), contentAlignment = Alignment.Center) {
      if (registerViewData.showServiceButton) {
        if (registerViewData.serviceButtonActionable) {
          ServiceButton(
            registerViewData = registerViewData,
            modifier =
              modifier
                .wrapContentHeight(Alignment.CenterVertically)
                .wrapContentWidth(Alignment.CenterHorizontally)
          )
        } else {
          ServiceActionSection(
            registerViewData = registerViewData,
            modifier =
              modifier.fillMaxHeight(0.6f).fillMaxWidth(0.9f).clip(RoundedCornerShape(4.dp))
          )
        }
      }
    }
  }
}

@Composable
private fun ServiceButton(registerViewData: RegisterViewData, modifier: Modifier) {
  val contentColor = remember { registerViewData.serviceButtonForegroundColor.copy(alpha = 0.85f) }
  Row(
    modifier =
      modifier
        .padding(horizontal = 8.dp)
        .clip(RoundedCornerShape(8.dp))
        .clickable { /*TODO Provide the given service*/}
        .background(color = registerViewData.serviceButtonForegroundColor.copy(alpha = 0.1f)),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Icon(
      imageVector = Icons.Filled.Add,
      contentDescription = null,
      tint = contentColor,
      modifier = modifier.size(16.dp).padding(horizontal = 1.dp)
    )
    Text(
      text = registerViewData.serviceText ?: "",
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
private fun RegisterListStatus(registerViewData: RegisterViewData, modifier: Modifier) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = modifier.padding(top = 4.dp),
  ) {
    if (!registerViewData.status.isNullOrEmpty()) {
      Text(
        text = registerViewData.status,
        color = DefaultColor,
        modifier = modifier.wrapContentWidth(Alignment.Start)
      )
    }
    if (!registerViewData.otherStatus.isNullOrEmpty()) {
      Separator()
      Text(
        text = registerViewData.otherStatus,
        color = DefaultColor,
        modifier = modifier.wrapContentWidth(Alignment.Start)
      )
    }
  }
}

@Composable
private fun ServiceActionSection(registerViewData: RegisterViewData, modifier: Modifier) {
  if (registerViewData.serviceText != null && !registerViewData.serviceButtonActionable) {
    if (registerViewData.borderedServiceButton) {
      Box(modifier = modifier.background(registerViewData.serviceButtonBorderColor))
    }
    Column(
      modifier =
        modifier
          .padding(if (registerViewData.borderedServiceButton) 1.4.dp else 0.dp)
          .background(registerViewData.serviceButtonBackgroundColor),
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      if (registerViewData.serviceTextIcon != null)
        Icon(
          painter = painterResource(id = registerViewData.serviceTextIcon),
          contentDescription = null,
          tint = registerViewData.serviceButtonForegroundColor
        )
      Text(
        text = registerViewData.serviceText,
        color = registerViewData.serviceButtonForegroundColor,
        textAlign = TextAlign.Center
      )
    }
  }
}

@Composable
private fun ServiceMemberIcons(registerViewData: RegisterViewData, modifier: Modifier) {
  // Count member icons only show and display counter of the rest
  val twoMemberIcons = remember { registerViewData.serviceMembers.take(2) }
  if (!registerViewData.serviceButtonActionable) {
    Row(modifier.padding(start = 4.dp)) {
      twoMemberIcons.forEach {
        if (it.icon != null)
          Icon(
            painter = painterResource(id = it.icon),
            contentDescription = null,
            modifier = modifier.size(20.dp).padding(0.dp),
            tint = Color.Unspecified
          )
      }
      if (twoMemberIcons.size == 2 && registerViewData.serviceMembers.size > 2) {
        Box(
          contentAlignment = Alignment.Center,
          modifier = modifier.clip(CircleShape).size(24.dp).background(DefaultColor.copy(0.1f))
        ) {
          Text(
            text = "+${registerViewData.serviceMembers.size - 2}",
            fontSize = 12.sp,
            color = Color.DarkGray
          )
        }
      }
    }
  }
}

@Composable
@Preview(showBackground = true)
fun RegisterListRowForFamilyRegisterOverduePreview() {
  RegisterListRow(
    registerViewData =
      RegisterViewData(
        logicalId = "1234",
        title = "John Doe, Male, 40y",
        subtitle = "#90129",
        status = "Last visited on Thursday",
        serviceText = "2",
        serviceButtonBackgroundColor = OverdueColor,
        serviceButtonForegroundColor = Color.White,
        serviceButtonActionable = false,
        showDivider = true,
        serviceMembers =
          listOf(
            ServiceMember(R.drawable.ic_pregnant, "1920192"),
            ServiceMember(R.drawable.ic_pregnant, "1920190"),
            ServiceMember(R.drawable.ic_pregnant, "1920191"),
            ServiceMember(R.drawable.ic_pregnant, "1920194")
          )
      ),
    onRowClick = {}
  )
}

@Composable
@Preview(showBackground = true)
fun RegisterListRowForFamilyRegisterDuePreview() {
  RegisterListRow(
    registerViewData =
      RegisterViewData(
        logicalId = "1234",
        title = "Ekuro Eoukot, Male, 67y",
        subtitle = "#90129",
        status = "Last visited on 03-03-2022",
        serviceText = "1",
        serviceButtonActionable = false,
        borderedServiceButton = true,
        serviceButtonForegroundColor = InfoColor,
        serviceButtonBorderColor = InfoColor,
        showDivider = true,
      ),
    onRowClick = {}
  )
}

@Composable
@Preview(showBackground = true)
fun RegisterListRowForQuestRegisterPreview() {
  RegisterListRow(
    registerViewData =
      RegisterViewData(logicalId = "1234", title = "John Doe, 40y", subtitle = "Male"),
    onRowClick = {}
  )
}

@Composable
@Preview(showBackground = true)
fun RegisterListRowForRdtRegisterPreview() {
  RegisterListRow(
    registerViewData =
      RegisterViewData(
        logicalId = "1234",
        title = "Jackie Johnson, Female, 40y",
        status = "Last test",
        otherStatus = "04 Feb 2022"
      ),
    onRowClick = {}
  )
}

@Composable
@Preview(showBackground = true)
fun RegisterListRowForAncRegisterPreview() {
  RegisterListRow(
    registerViewData =
      RegisterViewData(
        logicalId = "121299",
        title = "Alberta Tuft, Female, 26Y",
        status = "ID Number: 1929102",
        otherStatus = "Kimulu village",
        serviceButtonActionable = true,
        serviceText = "ANC visit",
        serviceButtonForegroundColor = InfoColor
      ),
    onRowClick = {}
  )
}

@Composable
@Preview(showBackground = true)
fun RegisterListRowForEirRegisterPreview() {
  RegisterListRow(
    registerViewData =
      RegisterViewData(
        logicalId = "1212299",
        title = "Mohamed Ali, Male, 26Y",
        status = "Last test",
        otherStatus = "04 Feb 2022",
        serviceText = "Overdue",
        serviceButtonForegroundColor = Color.White,
        serviceButtonBackgroundColor = OverdueColor,
        showDivider = true
      ),
    onRowClick = {}
  )
}
