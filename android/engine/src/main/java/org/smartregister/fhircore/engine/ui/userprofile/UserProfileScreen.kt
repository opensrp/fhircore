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

package org.smartregister.fhircore.engine.ui.userprofile

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
import androidx.compose.material.AlertDialog
import androidx.compose.material.Divider
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import org.smartregister.fhircore.engine.BuildConfig
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.ui.components.register.LoaderDialog
import org.smartregister.fhircore.engine.ui.theme.BlueTextColor
import org.smartregister.fhircore.engine.ui.theme.DividerColor
import org.smartregister.fhircore.engine.ui.theme.LighterBlue

const val RESET_DATABASE_DIALOG = "resetDatabaseDialog"

@Composable
fun UserProfileScreen(
  modifier: Modifier = Modifier,
  userProfileViewModel: UserProfileViewModel = hiltViewModel()
) {

  val username by remember { mutableStateOf(userProfileViewModel.retrieveUsername()) }
  var expanded by remember { mutableStateOf(false) }
  val resetDB by userProfileViewModel.onDatabaseReset.observeAsState(false)
  val showProgressBar by userProfileViewModel.showProgressBar.observeAsState(false)

  Column(modifier = modifier.padding(vertical = 20.dp)) {
    if (!username.isNullOrEmpty()) {
      Column(modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        Box(
          modifier = modifier.clip(CircleShape).background(color = LighterBlue).size(80.dp),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = username!!.first().uppercase(),
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            color = BlueTextColor
          )
        }
        Text(
          text = username!!.capitalize(Locale.current),
          fontSize = 22.sp,
          modifier = modifier.padding(vertical = 22.dp),
          fontWeight = FontWeight.Bold
        )
      }
    }
    Divider(color = DividerColor)
    UserProfileRow(
      icon = Icons.Rounded.Sync,
      text = stringResource(id = R.string.sync),
      clickListener = userProfileViewModel::runSync,
      modifier = modifier
    )

    // Language option
    if (userProfileViewModel.allowSwitchingLanguages()) {
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
            painterResource(R.drawable.ic_outline_language_black),
            stringResource(R.string.language),
            tint = BlueTextColor,
            modifier = Modifier.size(26.dp)
          )
          Spacer(modifier = modifier.width(20.dp))
          Text(text = stringResource(id = R.string.language), fontSize = 18.sp)
        }
        Box(contentAlignment = Alignment.CenterEnd) {
          Text(
            text = userProfileViewModel.loadSelectedLanguage(),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = modifier.wrapContentWidth(Alignment.End)
          )
          DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = modifier.wrapContentWidth(Alignment.End)
          ) {
            for (language in userProfileViewModel.languages) {
              DropdownMenuItem(onClick = { userProfileViewModel.setLanguage(language) }) {
                Text(text = language.displayName, fontSize = 18.sp)
              }
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

    if (showProgressBar) {
      LoaderDialog(modifier = modifier, stringResource(id = R.string.resetting_app))
    }

    if (resetDB) {
      ConfirmClearDatabaseDialog(
        permanentResetDatabase = {
          userProfileViewModel.showProgressBarFlag(true)
          userProfileViewModel.resetDatabase()
        },
        onDismissDialog = { userProfileViewModel.resetDatabaseFlag(false) }
      )
    }

    if (BuildConfig.DEBUG) {
      UserProfileRow(
        icon = Icons.Rounded.DeleteForever,
        text = stringResource(id = R.string.clear_database),
        clickListener = { userProfileViewModel.resetDatabaseFlag(true) },
        modifier = modifier
      )
    }

    UserProfileRow(
      icon = Icons.Rounded.Logout,
      text = stringResource(id = R.string.logout),
      clickListener = userProfileViewModel::logoutUser,
      modifier = modifier
    )
  }
}

@Composable
fun UserProfileRow(
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

@Composable
fun ConfirmClearDatabaseDialog(
  permanentResetDatabase: () -> Unit,
  onDismissDialog: () -> Unit,
  modifier: Modifier = Modifier
) {
  AlertDialog(
    onDismissRequest = onDismissDialog,
    title = {
      Text(
        text = stringResource(R.string.clear_database_title),
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp
      )
    },
    text = { Text(text = stringResource(R.string.clear_database_message), fontSize = 16.sp) },
    buttons = {
      Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 20.dp),
        horizontalArrangement = Arrangement.End
      ) {
        Text(
          text = stringResource(R.string.cancel),
          modifier = modifier.padding(horizontal = 10.dp).clickable { onDismissDialog() }
        )
        Text(
          color = MaterialTheme.colors.primary,
          text = stringResource(R.string.clear_database).uppercase(),
          modifier =
            modifier.padding(horizontal = 10.dp).clickable {
              permanentResetDatabase()
              onDismissDialog()
            }
        )
      }
    },
    modifier = Modifier.testTag(RESET_DATABASE_DIALOG)
  )
}
