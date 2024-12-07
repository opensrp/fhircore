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

package org.smartregister.fhircore.quest.ui.usersetting

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.AlertDialog
import androidx.compose.material.CircularProgressIndicator
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.flow.MutableStateFlow
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.domain.model.Language
import org.smartregister.fhircore.engine.ui.components.register.LoaderDialog
import org.smartregister.fhircore.engine.ui.theme.BlueTextColor
import org.smartregister.fhircore.engine.ui.theme.DividerColor
import org.smartregister.fhircore.engine.ui.theme.GreyTextColor
import org.smartregister.fhircore.engine.ui.theme.LighterBlue
import org.smartregister.fhircore.engine.ui.theme.LoginDarkColor
import org.smartregister.fhircore.engine.util.annotation.PreviewWithBackgroundExcludeGenerated
import org.smartregister.fhircore.engine.util.extension.appVersion

const val RESET_DATABASE_DIALOG = "resetDatabaseDialog"
const val USER_SETTING_ROW_LOGOUT = "userSettingRowLogout"
const val USER_SETTING_ROW_RESET_DATA = "userSettingRowResetData"
const val USER_SETTING_ROW_P2P = "userSettingRowP2P"
const val USER_SETTING_ROW_INSIGHTS = "userSettingRowInsights"
const val USER_SETTING_ROW_CONTACT_HELP = "userSettingRowContactHelp"
const val USER_SETTING_ROW_OFFLINE_MAP = "userSettingRowOfflineMap"
const val USER_SETTING_ROW_SYNC = "userSettingRowSync"
const val OPENSRP_LOGO_TEST_TAG = "opensrpLogoTestTag"

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun UserSettingScreen(
  appTitle: String?,
  modifier: Modifier = Modifier,
  username: String?,
  practitionerLocation: String?,
  fullname: String?,
  selectedLanguage: String,
  languages: List<Language>,
  progressBarState: Pair<Boolean, Int>,
  isDebugVariant: Boolean = false,
  onEvent: (UserSettingsEvent) -> Unit,
  mainNavController: NavController,
  appVersionPair: Pair<Int, String>? = null,
  dataMigrationVersion: String,
  lastSyncTime: String?,
  showProgressIndicatorFlow: MutableStateFlow<Boolean>,
  enableManualSync: Boolean,
  allowSwitchingLanguages: Boolean,
  showDatabaseResetConfirmation: Boolean,
  enableAppInsights: Boolean,
  showOfflineMaps: Boolean = false,
  allowP2PSync: Boolean = false,
  enableHelpContacts: Boolean = false,
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
            Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
          }
        },
        contentColor = Color.White,
        backgroundColor = MaterialTheme.colors.primary,
      )
    },
    backgroundColor = colorResource(id = R.color.backgroundGray),
  ) {
    Column(
      modifier =
        Modifier.background(color = colorResource(id = R.color.backgroundGray))
          .verticalScroll(rememberScrollState()),
    ) {
      if (!username.isNullOrEmpty()) {
        Column(
          modifier = modifier.background(Color.White).padding(vertical = 24.dp).fillMaxWidth(),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          Box(
            modifier = modifier.clip(CircleShape).background(color = LighterBlue).size(80.dp),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = username.first().uppercase(),
              textAlign = TextAlign.Center,
              fontWeight = FontWeight.Bold,
              fontSize = 28.sp,
              color = BlueTextColor,
            )
          }
          Text(
            text = appTitle ?: "",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
          )
          Text(
            text = fullname?.capitalize(Locale.current) ?: "",
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal,
          )
          Text(
            text = "@${username.capitalize(Locale.current)}",
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal,
          )
          Text(
            text = practitionerLocation?.capitalize(Locale.current) ?: "",
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal,
          )
        }
      }

      Divider(color = DividerColor)
      Column(modifier = modifier.background(color = colorResource(id = R.color.backgroundGray))) {
        Spacer(
          modifier = modifier.padding(top = 16.dp).padding(bottom = 16.dp),
        )
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
            fontWeight = FontWeight.Medium,
          )
        }
      }
      Divider(color = DividerColor)

      if (enableManualSync) {
        UserSettingRow(
          icon = Icons.Rounded.Sync,
          text = stringResource(id = R.string.manual_sync),
          clickListener = { onEvent(UserSettingsEvent.SyncData(context)) },
          modifier = modifier.testTag(USER_SETTING_ROW_SYNC),
        )
      }

      if (showOfflineMaps) {
        UserSettingRow(
          icon = Icons.Rounded.Map,
          text = stringResource(id = R.string.offline_map),
          clickListener = { onEvent(UserSettingsEvent.OnLaunchOfflineMap(true, context)) },
          modifier = modifier.testTag(USER_SETTING_ROW_OFFLINE_MAP),
          canSwitchToScreen = true,
        )
      }

      // Language option
      if (allowSwitchingLanguages) {
        Row(
          modifier =
            modifier
              .background(Color.White)
              .fillMaxWidth()
              .clickable { expanded = true }
              .padding(vertical = 16.dp, horizontal = 20.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Row(modifier = Modifier.align(Alignment.CenterVertically)) {
            Icon(
              painterResource(R.drawable.ic_language),
              stringResource(R.string.language),
              tint = GreyTextColor,
              modifier = Modifier.size(26.dp),
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
              modifier = modifier.wrapContentWidth(Alignment.End),
            )
            DropdownMenu(
              expanded = expanded,
              onDismissRequest = { expanded = false },
              modifier = modifier.wrapContentWidth(Alignment.End),
            ) {
              for (language in languages) {
                DropdownMenuItem(
                  onClick = {
                    onEvent(
                      UserSettingsEvent.SwitchLanguage(
                        language,
                        context,
                      ),
                    )
                  },
                ) {
                  Text(text = language.displayName, fontSize = 18.sp)
                }
              }
            }
          }
          Icon(
            imageVector = Icons.Rounded.ChevronRight,
            "",
            tint = Color.LightGray,
            modifier = modifier.wrapContentWidth(Alignment.End),
          )
        }
        Divider(color = DividerColor)
      }

      if (showProgressBar) {
        LoaderDialog(modifier = modifier, dialogMessage = stringResource(messageResource))
      }

      if (allowP2PSync) {
        UserSettingRow(
          icon = Icons.Rounded.Share,
          text = stringResource(id = R.string.transfer_data),
          clickListener = { onEvent(UserSettingsEvent.SwitchToP2PScreen(context)) },
          modifier = modifier.testTag(USER_SETTING_ROW_P2P),
          canSwitchToScreen = true,
        )
      }

      if (showDatabaseResetConfirmation) {
        ConfirmClearDatabaseDialog(
          permanentResetDatabase = {
            onEvent(UserSettingsEvent.ShowLoaderView(true, R.string.clear_database))
            onEvent(UserSettingsEvent.ResetDatabaseFlag(true, context))
          },
          onDismissDialog = {
            onEvent(UserSettingsEvent.ShowResetDatabaseConfirmationDialog(false))
          },
        )
      }

      if (isDebugVariant) {
        UserSettingRow(
          icon = Icons.Rounded.DeleteForever,
          text = stringResource(id = R.string.clear_database),
          clickListener = { onEvent(UserSettingsEvent.ShowResetDatabaseConfirmationDialog(true)) },
          modifier = modifier.testTag(USER_SETTING_ROW_RESET_DATA),
        )
      }

      if (enableAppInsights) {
        UserSettingRow(
          icon = Icons.Rounded.Insights,
          text = stringResource(id = R.string.insights),
          clickListener = {
            onEvent(UserSettingsEvent.ShowInsightsScreen(navController = mainNavController))
          },
          modifier = modifier.testTag(USER_SETTING_ROW_INSIGHTS),
          showProgressIndicator = showProgressIndicatorFlow.collectAsState().value,
          canSwitchToScreen = true,
        )
      }

      if (enableHelpContacts) {
        UserSettingRow(
          icon = Icons.Rounded.Phone,
          text = stringResource(id = R.string.contact_help),
          clickListener = { onEvent(UserSettingsEvent.ShowContactView(true, context)) },
          modifier = modifier.testTag(USER_SETTING_ROW_CONTACT_HELP),
          canSwitchToScreen = true,
        )
      }

      UserSettingRow(
        icon = Icons.AutoMirrored.Rounded.Logout,
        text = stringResource(id = R.string.logout),
        clickListener = { onEvent(UserSettingsEvent.Logout(context)) },
        modifier = modifier.testTag(USER_SETTING_ROW_LOGOUT),
        iconTint = colorResource(id = R.color.colorError),
        textColor = colorResource(id = R.color.colorError),
      )

      Column(
        modifier =
          modifier
            .background(color = colorResource(id = R.color.backgroundGray))
            .fillMaxWidth()
            .padding(top = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Spacer(modifier = Modifier.weight(1f))

        Image(
          painterResource(R.drawable.ic_opensrplogo),
          "content description",
          colorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }),
          modifier =
            modifier
              .padding(top = 8.dp)
              .requiredHeight(32.dp)
              .align(Alignment.CenterHorizontally)
              .testTag(OPENSRP_LOGO_TEST_TAG),
          contentScale = ContentScale.Fit,
        )

        Text(
          color = contentColor,
          fontSize = 16.sp,
          text = stringResource(id = R.string.app_version, versionCode, versionName),
          modifier = modifier.padding(top = 8.dp).align(Alignment.CenterHorizontally),
        )

        if (dataMigrationVersion.toInt() > 0) {
          Text(
            color = contentColor,
            fontSize = 16.sp,
            text = stringResource(id = R.string.data_migration_version, dataMigrationVersion),
            modifier = modifier.padding(top = 2.dp).align(Alignment.CenterHorizontally),
          )
        }

        Text(
          color = contentColor,
          fontSize = 12.sp,
          text = stringResource(id = R.string.last_sync, lastSyncTime ?: ""),
          modifier =
            modifier.padding(bottom = 12.dp, top = 2.dp).align(Alignment.CenterHorizontally),
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
  iconTint: Color = GreyTextColor,
  textColor: Color = LoginDarkColor,
  showProgressIndicator: Boolean = false,
) {
  Row(
    modifier =
      modifier
        .fillMaxWidth()
        .background(color = colorResource(id = R.color.white))
        .clickable { clickListener() }
        .padding(vertical = 16.dp, horizontal = 20.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
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
    if (showProgressIndicator) {
      CircularProgressIndicator(
        modifier =
          modifier.size(18.dp).testTag(CIRCULAR_PROGRESS_INDICATOR).wrapContentWidth(Alignment.End),
        strokeWidth = 1.6.dp,
      )
    }
  }
  Divider(color = DividerColor)
}

@Composable
fun ConfirmClearDatabaseDialog(
  permanentResetDatabase: () -> Unit,
  onDismissDialog: () -> Unit,
  modifier: Modifier = Modifier,
) {
  AlertDialog(
    onDismissRequest = onDismissDialog,
    title = {
      Text(
        text = stringResource(R.string.clear_database_title),
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
      )
    },
    text = { Text(text = stringResource(R.string.clear_database_message), fontSize = 16.sp) },
    buttons = {
      Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 20.dp),
        horizontalArrangement = Arrangement.End,
      ) {
        Text(
          text = stringResource(R.string.cancel),
          modifier = modifier.padding(horizontal = 10.dp).clickable { onDismissDialog() },
        )
        Text(
          color = MaterialTheme.colors.primary,
          text = stringResource(R.string.clear_database).uppercase(),
          modifier =
            modifier.padding(horizontal = 10.dp).clickable {
              permanentResetDatabase()
              onDismissDialog()
            },
        )
      }
    },
    modifier = Modifier.testTag(RESET_DATABASE_DIALOG),
  )
}

@Composable
@PreviewWithBackgroundExcludeGenerated
fun UserSettingPreview() {
  UserSettingScreen(
    appTitle = "Quest",
    username = "Jam",
    fullname = "Jam Kenya",
    practitionerLocation = "Gateway Remote Location",
    selectedLanguage = java.util.Locale.ENGLISH.toLanguageTag(),
    languages = listOf(Language("en", "English"), Language("sw", "Swahili")),
    progressBarState = Pair(false, R.string.resetting_app),
    isDebugVariant = true,
    onEvent = {},
    mainNavController = rememberNavController(),
    appVersionPair = Pair(1, "1.0.1"),
    dataMigrationVersion = "0",
    lastSyncTime = "05:30 PM, Mar 3",
    showProgressIndicatorFlow = MutableStateFlow(false),
    enableManualSync = true,
    allowSwitchingLanguages = true,
    showDatabaseResetConfirmation = false,
    enableAppInsights = true,
    showOfflineMaps = true,
    allowP2PSync = true,
    enableHelpContacts = true,
  )
}
