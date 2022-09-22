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

package org.smartregister.fhircore.engine.ui.usersetting

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Divider
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.domain.model.Language
import org.smartregister.fhircore.engine.ui.theme.BlueTextColor
import org.smartregister.fhircore.engine.ui.theme.DividerColor
import org.smartregister.fhircore.engine.ui.theme.LighterBlue

@Composable
fun UserSettingScreen(
  modifier: Modifier = Modifier,
  username: String?,
  allowSwitchingLanguages: Boolean,
  selectedLanguage: String,
  languages: List<Language>,
  onEvent: (UserSettingsEvent) -> Unit,
) {
  val context = LocalContext.current
  var expanded by remember { mutableStateOf(false) }

  Column(modifier = modifier.padding(vertical = 20.dp)) {
    if (!username.isNullOrEmpty()) {
      Column(modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        Box(
          modifier = modifier.clip(CircleShape).background(color = LighterBlue).size(80.dp),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = username.first().uppercase(),
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            color = BlueTextColor
          )
        }
        Text(
          text = username.capitalize(Locale.current),
          fontSize = 22.sp,
          modifier = modifier.padding(vertical = 22.dp),
          fontWeight = FontWeight.Bold
        )
      }
    }
    Divider(color = DividerColor)

    //TODO temporary disabled the sync functionality and will be enabled in future
    /*UserSettingRow(
      icon = Icons.Rounded.Sync,
      text = stringResource(id = R.string.sync),
      clickListener = { onEvent(UserSettingsEvent.SyncData) },
      modifier = modifier
    )*/

    // Language option
    if (allowSwitchingLanguages) {
      Row(
        modifier =
          modifier
            .fillMaxWidth()
            .clickable { expanded = true }
            .padding(vertical = 16.dp, horizontal = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Row(modifier = Modifier.align(Alignment.CenterVertically)) {
          Icon(
            painterResource(R.drawable.ic_language),
            stringResource(R.string.language),
            tint = BlueTextColor,
            modifier = Modifier.size(26.dp)
          )
          Spacer(modifier = modifier.width(20.dp))
          Text(text = stringResource(id = R.string.language), fontSize = 18.sp)
        }
        Box(contentAlignment = Alignment.CenterEnd) {
          Text(
            text = selectedLanguage,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = modifier.wrapContentWidth(Alignment.End)
          )
          DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = modifier.wrapContentWidth(Alignment.End)
          ) {
            for (language in languages) {
              DropdownMenuItem(
                onClick = { onEvent(UserSettingsEvent.SwitchLanguage(language, context)) }
              ) { Text(text = language.displayName, fontSize = 18.sp) }
            }
          }
        }
        Icon(
          imageVector = Icons.Rounded.ChevronRight,
          "",
          tint = Color.LightGray,
          modifier = modifier.wrapContentWidth(Alignment.End)
        )
      }
      Divider(color = DividerColor)
    }

    UserSettingRow(
      icon = Icons.Rounded.Logout,
      text = stringResource(id = R.string.logout),
      clickListener = { onEvent(UserSettingsEvent.Logout) },
      modifier = modifier
    )
  }
}

@Composable
fun UserSettingRow(
  icon: ImageVector,
  text: String,
  clickListener: () -> Unit,
  modifier: Modifier = Modifier
) {
  Row(
    modifier =
      modifier
        .fillMaxWidth()
        .clickable { clickListener() }
        .padding(vertical = 16.dp, horizontal = 20.dp),
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Row {
      Icon(imageVector = icon, "", tint = BlueTextColor)
      Spacer(modifier = modifier.width(20.dp))
      Text(text = text, fontSize = 18.sp)
    }
    Icon(
      imageVector = Icons.Rounded.ChevronRight,
      "",
      tint = Color.LightGray,
      modifier = modifier.wrapContentWidth(Alignment.End)
    )
  }
  Divider(color = DividerColor)
}
