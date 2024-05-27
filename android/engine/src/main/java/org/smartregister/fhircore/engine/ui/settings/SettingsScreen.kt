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

package org.smartregister.fhircore.engine.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Scaffold
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material.icons.rounded.Report
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.coroutines.launch
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.ui.settings.views.DevMenu
import org.smartregister.fhircore.engine.ui.settings.views.ReportBottomSheet
import org.smartregister.fhircore.engine.ui.settings.views.UserProfileRow
import org.smartregister.fhircore.engine.ui.settings.views.ViewResourceReport
import org.smartregister.fhircore.engine.ui.theme.DividerColor
import org.smartregister.fhircore.engine.util.SharedPreferenceKey.LAST_PURGE_KEY

const val SYNC_TIMESTAMP_OUTPUT_FORMAT = "hh:mm aa, MMM d"

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun SettingsScreen(
  modifier: Modifier = Modifier,
  navController: NavController? = null,
  settingsViewModel: SettingsViewModel = hiltViewModel(),
  devViewModel: DevViewModel = hiltViewModel(),
) {
  val context = LocalContext.current
  val devMenuSheetState = rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden)
  val reportsSheetState = rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden)
  val viewResSheetState = rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden)
  val scope = rememberCoroutineScope()

  ModalBottomSheetLayout(
    sheetState = viewResSheetState,
    sheetContent = { ViewResourceReport(devViewModel) },
  ) {
    ModalBottomSheetLayout(
      sheetState = reportsSheetState,
      sheetContent = {
        ReportBottomSheet(devViewModel) {
          scope.launch {
            devMenuSheetState.hide()
            viewResSheetState.show()
          }
        }
      },
    ) {
      ModalBottomSheetLayout(
        sheetState = devMenuSheetState,
        sheetContent = { DevMenu(viewModel = devViewModel) },
      ) {
        Scaffold(
          topBar = {
            TopAppBar(
              title = {},
              navigationIcon = {
                IconButton(onClick = { navController?.popBackStack() }) {
                  Icon(Icons.Default.ArrowBack, "")
                }
              },
            )
          },
        ) { paddingValues ->
          Column(
            modifier =
              modifier
                .padding(paddingValues)
                .padding(vertical = 20.dp)
                .verticalScroll(rememberScrollState()),
          ) {
            InfoCard(profileData = settingsViewModel.profileData)
            Divider(color = DividerColor)
            UserProfileRow(
              icon = Icons.Rounded.Download,
              text = stringResource(R.string.re_fetch_practitioner),
              clickListener = settingsViewModel::fetchPractitionerDetails,
              modifier = modifier,
            )
            UserProfileRow(
              icon = Icons.Rounded.Sync,
              text = stringResource(id = R.string.sync),
              clickListener = settingsViewModel::runSync,
              modifier = modifier,
            )
            UserProfileRow(
              icon = Icons.Rounded.Report,
              text = stringResource(R.string.reports),
              clickListener = { scope.launch { reportsSheetState.show() } },
              modifier = modifier,
            )
            UserProfileRow(
              icon = Icons.Rounded.BugReport,
              text = stringResource(R.string.dev_menu),
              clickListener = { scope.launch { devMenuSheetState.show() } },
              modifier = modifier,
            )
            UserProfileRow(
              icon = Icons.Rounded.Logout,
              text = stringResource(id = R.string.logout),
              clickListener = { settingsViewModel.logoutUser(context) },
              modifier = modifier,
            )

            val timestamp = settingsViewModel.sharedPreferences.read(LAST_PURGE_KEY.name, 0L)
            val simpleDateFormat =
              SimpleDateFormat(SYNC_TIMESTAMP_OUTPUT_FORMAT, Locale.getDefault())
            val text = context.resources.getString(R.string.last_purge)
            val dateFormat = simpleDateFormat.format(timestamp)

            if (timestamp > 0L) {
              UserProfileRow(
                icon = Icons.Rounded.CleaningServices,
                text = "$text: $dateFormat",
                clickable = false,
                modifier = modifier,
              )
            }
          }
        }
      }
    }
  }
}
