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

package org.smartregister.fhircore.engine.ui.components.register

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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.domain.model.RegisterRowData
import org.smartregister.fhircore.engine.ui.components.Separator
import org.smartregister.fhircore.engine.ui.theme.DefaultColor
import org.smartregister.fhircore.engine.ui.theme.DividerColor
import org.smartregister.fhircore.engine.ui.theme.InfoColor
import org.smartregister.fhircore.engine.ui.theme.OverdueColor

@Composable
fun RegisterListRow(modifier: Modifier = Modifier, registerRowData: RegisterRowData) {
  Row(
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
    modifier = modifier.fillMaxWidth().height(IntrinsicSize.Min)
  ) {
    Column(modifier = modifier.weight(0.7f).padding(16.dp)) {
      if (registerRowData.serviceAsButton) {
        Row(
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
          modifier = modifier.fillMaxWidth()
        ) {
          Column(modifier = modifier.wrapContentWidth(Alignment.Start)) {
            Text(text = registerRowData.title)
            RegisterListStatus(registerRowData, modifier)
          }
          ServiceButton(
            registerRowData = registerRowData,
            modifier = modifier.wrapContentWidth(Alignment.End)
          )
        }
      } else {
        Text(text = registerRowData.title)
        Row(
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
          modifier = modifier.fillMaxWidth()
        ) {
          Column(modifier = modifier.wrapContentWidth(Alignment.Start).weight(0.7f)) {
            if (!registerRowData.subtitle.isNullOrEmpty()) {
              Text(
                text = registerRowData.subtitle,
                color = DefaultColor,
                modifier = modifier.padding(top = 4.dp)
              )
            }
            RegisterListStatus(registerRowData, modifier)
          }
          ServiceMemberIcons(
            registerRowData,
            modifier = modifier.wrapContentWidth(Alignment.End).weight(0.3f)
          )
        }
      }
    }
    if (!registerRowData.serviceText.isNullOrEmpty()) {
      Divider(
        modifier =
          modifier
            .fillMaxHeight()
            .padding(horizontal = 4.dp)
            .width(1.dp)
            .background(color = DividerColor)
      )
      ServiceActionSection(registerRowData, modifier = modifier.weight(0.3f))
    }
  }
}

@Composable
private fun ServiceButton(registerRowData: RegisterRowData, modifier: Modifier) {
  val contentColor = remember { registerRowData.serviceForegroundColor.copy(alpha = 0.85f) }
  Row(
    modifier =
      modifier.background(color = registerRowData.serviceForegroundColor.copy(alpha = 0.2f)),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Icon(imageVector = Icons.Filled.Add, contentDescription = null, tint = contentColor)
    Text(
      text = registerRowData.serviceText ?: "",
      color = contentColor,
      fontSize = 16.sp,
      fontWeight = FontWeight.Bold,
      modifier =
        modifier
          .clip(RoundedCornerShape(2.8.dp))
          .wrapContentWidth()
          .padding(4.8.dp)
          .clickable { /*TODO Provide the given service*/}
    )
  }
}

@Composable
private fun RegisterListStatus(registerRowData: RegisterRowData, modifier: Modifier) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = modifier.padding(top = 4.dp),
  ) {
    if (!registerRowData.status.isNullOrEmpty()) {
      Text(
        text = registerRowData.status,
        color = DefaultColor,
        modifier = modifier.wrapContentWidth(Alignment.Start)
      )
    }
    if (!registerRowData.otherStatus.isNullOrEmpty()) {
      Separator()
      Text(
        text = registerRowData.otherStatus,
        color = DefaultColor,
        modifier = modifier.wrapContentWidth(Alignment.Start)
      )
    }
  }
}

@Composable
private fun ServiceActionSection(registerRowData: RegisterRowData, modifier: Modifier) {
  if (registerRowData.serviceText != null && !registerRowData.serviceAsButton) {
    Box(
      contentAlignment = Alignment.Center,
      modifier = modifier.padding(8.dp).clip(RoundedCornerShape(4.dp))
    ) {
      Column(
        modifier = modifier.background(registerRowData.serviceBackgroundColor).fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        if (registerRowData.serviceTextIcon != null)
          Icon(
            painter = painterResource(id = registerRowData.serviceTextIcon),
            contentDescription = null,
            tint = registerRowData.serviceForegroundColor
          )
        Text(
          text = registerRowData.serviceText,
          color = registerRowData.serviceForegroundColor,
          textAlign = TextAlign.Center
        )
      }
    }
  }
}

@Composable
private fun ServiceMemberIcons(registerRowData: RegisterRowData, modifier: Modifier) {
  // Count member icons only show and display counter of the rest
  if (!registerRowData.serviceMemberIcons.isNullOrEmpty() && !registerRowData.serviceAsButton) {
    Row(modifier.padding(start = 4.dp)) {
      registerRowData.serviceMemberIcons.take(2).forEach {
        Icon(
          painter = painterResource(id = it),
          contentDescription = null,
          modifier = modifier.size(20.dp),
          tint = Color.Unspecified
        )
      }
      if (registerRowData.serviceMemberIcons.size > 2) {
        Box(
          contentAlignment = Alignment.Center,
          modifier = modifier.clip(CircleShape).size(20.dp).background(DefaultColor.copy(0.1f))
        ) { Text(text = "+${registerRowData.serviceMemberIcons.size - 2}", fontSize = 10.sp) }
      }
    }
  }
}

@Composable
@Preview(showBackground = true)
fun RegisterListRowForFamilyRegisterPreview() {
  RegisterListRow(
    registerRowData =
      RegisterRowData(
        id = "1234",
        title = "John Doe, Male, 40y",
        subtitle = "#90129",
        status = "Last visited on Thursday",
        serviceText = "ANC visit",
        serviceBackgroundColor = OverdueColor,
        serviceForegroundColor = Color.White,
        serviceMemberIcons =
          listOf(R.drawable.ic_pregnant, R.drawable.ic_pregnant, R.drawable.ic_pregnant),
        serviceAsButton = false
      )
  )
}

@Composable
@Preview(showBackground = true)
fun RegisterListRowForQuestRegisterPreview() {
  RegisterListRow(
    registerRowData = RegisterRowData(id = "1234", title = "John Doe, 40y", subtitle = "Male")
  )
}

@Composable
@Preview(showBackground = true)
fun RegisterListRowForRdtRegisterPreview() {
  RegisterListRow(
    registerRowData =
      RegisterRowData(
        id = "1234",
        title = "Jackie Johnson, Female, 40y",
        status = "Last test",
        otherStatus = "04 Feb 2022"
      )
  )
}

@Composable
@Preview(showBackground = true)
fun RegisterListRowForAncRegisterPreview() {
  RegisterListRow(
    registerRowData =
      RegisterRowData(
        id = "121299",
        title = "Alberta Tuft, Female, 26Y",
        status = "ID Number: 1929102",
        otherStatus = "Kimulu village",
        serviceAsButton = true,
        serviceText = "ANC visit",
        serviceForegroundColor = InfoColor
      )
  )
}

@Composable
@Preview(showBackground = true)
fun RegisterListRowForEirRegisterPreview() {
  RegisterListRow(
    registerRowData =
      RegisterRowData(
        id = "1212299",
        title = "Mohamed Ali, Male, 26Y",
        status = "Last test",
        otherStatus = "04 Feb 2022",
        serviceText = "Overdue",
        serviceForegroundColor = Color.White,
        serviceBackgroundColor = OverdueColor
      )
  )
}
