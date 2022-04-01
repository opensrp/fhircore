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

package org.smartregister.fhircore.anc.ui.details.child.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.smartregister.fhircore.anc.R
import org.smartregister.fhircore.anc.ui.details.child.model.ChildProfileRowItem
import org.smartregister.fhircore.engine.ui.theme.OverdueColor

val StatusTextColor = Color.Gray.copy(alpha = 0.9f)
val InfoColor = Color.Blue.copy(alpha = 0.5f)

@Composable
fun ChildProfileTaskRow(
  childProfileRowItem: ChildProfileRowItem,
  onRowClick: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier.fillMaxWidth().padding(16.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Column {
      TitleRow(childProfileRowItem = childProfileRowItem, modifier = modifier)
      Spacer(modifier = modifier.height(8.dp))
      SubtitleRow(childProfileRowItem = childProfileRowItem, modifier = modifier)
    }
    ActionButton(childProfileRowItem, onRowClick)
  }
}

@Composable
private fun ActionButton(childProfileRowItem: ChildProfileRowItem, onRowClick: (String) -> Unit) {
  if (childProfileRowItem.actionButtonColor != null && childProfileRowItem.actionButtonText != null
  ) {
    OutlinedButton(
      onClick = { onRowClick.invoke(childProfileRowItem.id) },
      colors =
        ButtonDefaults.buttonColors(
          backgroundColor = childProfileRowItem.actionButtonColor.copy(alpha = 0.2f),
          contentColor = childProfileRowItem.actionButtonColor,
        )
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = Icons.Filled.Add, contentDescription = null)
        Text(text = childProfileRowItem.actionButtonText)
      }
    }
  }
}

@Composable
private fun TitleRow(childProfileRowItem: ChildProfileRowItem, modifier: Modifier) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    Text(
      text = childProfileRowItem.title,
      fontWeight = FontWeight.SemiBold,
      modifier = modifier.padding(end = 8.dp)
    )
    if (childProfileRowItem.titleIcon != null)
      Image(painter = painterResource(childProfileRowItem.titleIcon), contentDescription = null)
  }
}

@Composable
private fun SubtitleRow(childProfileRowItem: ChildProfileRowItem, modifier: Modifier) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    Text(
      text = childProfileRowItem.subtitle,
      color = StatusTextColor,
      fontSize = 12.sp,
      modifier = modifier.padding(end = 8.dp)
    )
    if (childProfileRowItem.subtitleStatus != null)
      Text(
        text = childProfileRowItem.subtitleStatus,
        color = childProfileRowItem.subtitleStatusColor ?: StatusTextColor,
        modifier =
          modifier
            .clip(RoundedCornerShape(6.dp))
            .background(
              if (childProfileRowItem.showDot) Color.Transparent
              else
                childProfileRowItem.subtitleStatusColor?.copy(alpha = 0.2f)
                  ?: StatusTextColor.copy(alpha = 0.2f)
            )
            .padding(4.dp)
      )
  }
}

@Composable
@Preview(showBackground = true)
fun ProfileActionableItemForTasksPreview() {
  Column {
    ChildProfileTaskRow(
      ChildProfileRowItem(
        id = "test-id-1",
        title = "Child Routine visit task",
        titleIcon = R.drawable.ic_pregnant,
        subtitle = "12-02-2022",
        actionButtonColor = InfoColor,
        actionButtonText = "Child visit"
      ),
      {}
    )
    Divider()
    ChildProfileTaskRow(
      ChildProfileRowItem(
        id = "test-id-2",
        title = "Child Routine visit task",
        titleIcon = R.drawable.ic_pregnant,
        subtitle = "12-8-2020",
        actionButtonColor = OverdueColor,
        actionButtonText = "Child visit"
      ),
      {}
    )
  }
}
