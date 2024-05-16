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

package org.smartregister.fhircore.engine.ui.settings.views

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.smartregister.fhircore.engine.ui.theme.BlueTextColor
import org.smartregister.fhircore.engine.ui.theme.DividerColor

@Composable
fun UserProfileRow(
  text: String,
  modifier: Modifier = Modifier,
  icon: ImageVector? = null,
  iconAlt: (@Composable () -> Unit)? = null,
  clickListener: (() -> Unit) = {},
  clickable: Boolean = true,
) {
  Row(
    modifier =
      modifier
        .fillMaxWidth()
        .clickable(clickable) { clickListener() }
        .padding(vertical = 16.dp, horizontal = 20.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Row {
      if (icon != null) {
        Icon(imageVector = icon, "", tint = BlueTextColor)
        Spacer(modifier = modifier.width(20.dp))
      }
      if (iconAlt != null) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.Center,
        ) {
          iconAlt()
        }
      }
      Text(text = text, fontSize = 18.sp)
    }
    if (clickable) {
      Icon(
        imageVector = Icons.Rounded.ChevronRight,
        "",
        tint = Color.LightGray,
        modifier = modifier.wrapContentWidth(Alignment.End),
      )
    }
  }
  Divider(color = DividerColor)
}

@Composable
fun SectionTitle(text: String) {
  Column(modifier = Modifier.padding(bottom = 6.dp)) {
    Text(text = text, style = MaterialTheme.typography.h6)
  }
}
