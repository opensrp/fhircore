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

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.AlertDialog
import androidx.compose.material.Divider
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material.icons.rounded.Share
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.domain.model.Language
import org.smartregister.fhircore.engine.ui.components.register.LoaderDialog
import org.smartregister.fhircore.engine.ui.theme.BlueTextColor
import org.smartregister.fhircore.engine.ui.theme.DividerColor
import org.smartregister.fhircore.engine.ui.theme.LighterBlue
import org.smartregister.fhircore.engine.ui.theme.LoginDarkColor
import org.smartregister.fhircore.engine.util.extension.appVersion

const val RESET_DATABASE_DIALOG = "resetDatabaseDialog"
const val USER_SETTING_ROW_LOGOUT = "userSettingRowLogout"
const val USER_SETTING_ROW_RESET_DATA = "userSettingRowResetData"
const val USER_SETTING_ROW_P2P = "userSettingRowP2P"
const val USER_SETTING_ROW_LANGUAGE = "userSettingRowLanguage"

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun UserSettingScreen(
  modifier: Modifier = Modifier,
  username: String?,
  allowSwitchingLanguages: Boolean,
  selectedLanguage: String,
  languages: List<Language>,
  showDatabaseResetConfirmation: Boolean,
  progressBarState: Pair<Boolean, Int>,
  isDebugVariant: Boolean = false,
  onEvent: (UserSettingsEvent) -> Unit,
  mainNavController: NavController,
  appVersionPair: Pair<Int, String>? = null,
  allowP2PSync: Boolean,
  lastSyncTime: String?
) {
  val context = LocalContext.current
  val (showProgressBar, messageResource) = progressBarState
  var expanded by remember { mutableStateOf(false) }
  val (versionCode, versionName) = remember { appVersionPair ?: context.appVersion() }
  val contentColor = colorResource(id = R.color.grayText)
  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text(text = stringResource(R.string.settings)) },
        navigationIcon = {
          IconButton(onClick = { mainNavController.popBackStack() }) {
            Icon(Icons.Filled.ArrowBack, null)
          }
        },
        contentColor = Color.White,
        backgroundColor = MaterialTheme.colors.primary
      )
    }
  ) {
    Column(modifier = modifier.background(Color.White)) {
      if (!username.isNullOrEmpty()) {
        Column(
          modifier = modifier.background(Color.White).padding(vertical = 24.dp).fillMaxWidth(),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
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
            modifier = modifier.padding(vertical = 12.dp),
            fontWeight = FontWeight.Bold
          )
        }
      }

      Divider(color = DividerColor)
      Column(modifier = modifier.background(color = colorResource(id = R.color.backgroundGray))) {
        Spacer(modifier = modifier.padding(top = 16.dp).padding(bottom = 16.dp))
        Row {
          Text(
            modifier =
              modifier
                .padding(top = 4.dp)
                .padding(bottom = 8.dp)
                .padding(start = 20.dp)
                .fillMaxWidth(),
            text = stringResource(R.string.settings).uppercase(),
            fontSize = 18.sp,
            color = contentColor,
            fontWeight = FontWeight.Medium
          )
        }
      }

      Divider(color = DividerColor)

      UserSettingRow(
        icon = Icons.Rounded.Sync,
        text = stringResource(id = R.string.sync),
        clickListener = { onEvent(UserSettingsEvent.SyncData) },
        modifier = modifier
      )

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
          Spacer(modifier = modifier.weight(1f))
          Box(contentAlignment = Alignment.CenterEnd) {
            Text(
              text = selectedLanguage,
              fontSize = 18.sp,
              fontWeight = FontWeight.Medium,
              color = contentColor,
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

      if (showProgressBar) {
        LoaderDialog(modifier = modifier, stringResource(messageResource))
      }

      if (showDatabaseResetConfirmation) {
        ConfirmClearDatabaseDialog(
          permanentResetDatabase = {
            onEvent(UserSettingsEvent.ShowLoaderView(true, R.string.clear_database))
            onEvent(UserSettingsEvent.ResetDatabaseFlag(true))
          },
          onDismissDialog = {
            onEvent(UserSettingsEvent.ShowResetDatabaseConfirmationDialog(false))
          }
        )
      }

      if (isDebugVariant) {
        UserSettingRow(
          icon = Icons.Rounded.DeleteForever,
          text = stringResource(id = R.string.clear_database),
          clickListener = { onEvent(UserSettingsEvent.ShowResetDatabaseConfirmationDialog(true)) },
          modifier = modifier.testTag(USER_SETTING_ROW_RESET_DATA)
        )
      }

      if (allowP2PSync) {
        UserSettingRow(
          icon = Icons.Rounded.Share,
          text = stringResource(id = R.string.transfer_data),
          clickListener = { onEvent(UserSettingsEvent.SwitchToP2PScreen(context)) },
          modifier = modifier.testTag(USER_SETTING_ROW_P2P),
          canSwitchToScreen = true
        )
      }

      UserSettingRow(
        icon = Icons.Rounded.Logout,
        text = stringResource(id = R.string.logout),
        clickListener = { onEvent(UserSettingsEvent.Logout) },
        modifier = modifier.testTag(USER_SETTING_ROW_LOGOUT),
        iconTint = colorResource(id = R.color.colorError),
        textColor = colorResource(id = R.color.colorError)
      )

      Column(
        modifier =
          modifier.background(color = colorResource(id = R.color.backgroundGray)).fillMaxWidth()
      ) {
        Spacer(modifier = Modifier.weight(1f))

        Image(
          painterResource(R.drawable.logo_fhir_core),
          "content description",
          modifier = modifier.requiredHeight(40.dp).align(Alignment.CenterHorizontally),
          contentScale = ContentScale.Fit
        )

        Text(
          color = contentColor,
          fontSize = 16.sp,
          text = stringResource(id = R.string.app_version, versionCode, versionName),
          modifier = modifier.padding(top = 12.dp).align(Alignment.CenterHorizontally)
        )

        Text(
          color = contentColor,
          fontSize = 16.sp,
          text = lastSyncTime ?: "",
          modifier =
            modifier.padding(bottom = 12.dp, top = 2.dp).align(Alignment.CenterHorizontally)
        )
      }
    }
  }
}

@Composable
fun UserSettingRow(
  icon: ImageVector,
  text: String,
  clickListener: () -> Unit,
  modifier: Modifier = Modifier,
  canSwitchToScreen: Boolean = false,
  iconTint: Color = BlueTextColor,
  textColor: Color = LoginDarkColor
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
      Icon(imageVector = icon, "", tint = iconTint)
      Spacer(modifier = modifier.width(20.dp))
      Text(text = text, fontSize = 18.sp, color = textColor)
    }
    if (canSwitchToScreen) {
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

@Composable
@Preview(showBackground = true)
fun UserSettingPreview() {
  UserSettingScreen(
    username = "Jam",
    allowSwitchingLanguages = true,
    selectedLanguage = java.util.Locale.ENGLISH.toLanguageTag(),
    languages = listOf(Language("en", "English"), Language("sw", "Swahili")),
    showDatabaseResetConfirmation = false,
    progressBarState = Pair(false, R.string.resetting_app),
    isDebugVariant = true,
    onEvent = {},
    mainNavController = rememberNavController(),
    appVersionPair = Pair(1, "1.0.1"),
    allowP2PSync = true,
    lastSyncTime = "05:30 PM, Mar 3"
  )
}
