/*
 * Copyright 2021-2023 Ona Systems, Inc
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

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.primarySurface
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import org.smartregister.fhircore.engine.ui.theme.DividerColor
import org.smartregister.fhircore.engine.ui.theme.LoginDarkColor
import org.smartregister.fhircore.quest.R

@Composable
fun UserSettingInsightScreen(
  unsyncedResources: List<Pair<String, Int>>,
  syncedResources: List<Pair<String, Int>>,
  navController: NavController,
  onRefreshRequest: () -> Unit,
) {
  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text(text = stringResource(R.string.insights)) },
        navigationIcon = {
          IconButton(onClick = { navController.popBackStack() }) {
            Icon(Icons.Filled.ArrowBack, null)
          }
        },
        contentColor = Color.White,
        backgroundColor = MaterialTheme.colors.primary,
      )
    },
    backgroundColor = Color.White,
  ) { paddingValues ->
    LazyColumn(
      Modifier.fillMaxSize().padding(paddingValues).background(Color.White),
      horizontalAlignment = Alignment.Start,
      contentPadding = PaddingValues(16.dp),
    ) {
      item {
        Text(
          text =
            if (unsyncedResources.isNullOrEmpty()) {
              stringResource(id = R.string.synced_statistics)
            } else {
              stringResource(id = R.string.unsynced_resources)
            },
          style = TextStyle(color = Color.Black, fontSize = 18.sp),
          fontWeight = FontWeight.Bold,
        )
      }

      items(unsyncedResources) { unsynced ->
        Row(
          Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = unsynced.first,
            fontWeight = FontWeight.Light,
          )
          Text(
            text = unsynced.second.toString(),
          )
        }

        Spacer(modifier = Modifier.padding(4.dp))
      }
      items(syncedResources) { synced ->
        Row(
          Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = synced.first,
            fontWeight = FontWeight.Light,
          )
          Text(
            text = synced.second.toString(),
          )
        }
        Divider(color = DividerColor)
        Spacer(modifier = Modifier.padding(8.dp))
      }
      item {
        UserInfoView(
          title = stringResource(id = R.string.user_info),
          name = "Tembo",
          team = "Tembo",
          locality = "Ps Dev-a",
        )
      }
      item {
        AppInfoView(
          title = stringResource(id = R.string.app_info),
          userName = "User_name",
          organization = "team_organization",
          careTeam = "care_team",
          location = "location",
        )
      }

      item {
        AssignmentInfoView(
          title = stringResource(id = R.string.assignment_info),
          appVersion = "v2.3.4",
          appVersionCode = "119",
          databaseVersion = "31",
          buildDate = "29 Jan 2023",
        )
      }

      item {
        DeviceInfoView(
          title = stringResource(id = R.string.device_info),
          manufacture = "Sumsung",
          device = "Sm-Tmo",
          osVersion = "R",
          date = "Jan 29 2023 3:02:19 PM",
        )
      }
      item {
        Column(
          Modifier.wrapContentWidth().wrapContentHeight().padding(4.dp),
        ) {
          Surface(shape = RoundedCornerShape(0.dp)) {
            OutlinedButton(
              modifier = Modifier.fillMaxWidth(),
              onClick = onRefreshRequest,
              border = BorderStroke(0.7.dp, MaterialTheme.colors.primarySurface),
            ) {
              Text(
                text = stringResource(R.string.refresh),
                modifier = Modifier.padding(6.dp),
                style =
                  TextStyle(
                    color = MaterialTheme.colors.primarySurface,
                    fontSize = 14.sp,
                  ),
              )
            }
          }
        }
      }
    }
  }
}

@Composable
fun UserInfoView(
  title: String,
  name: String,
  team: String,
  locality: String,
) {
  Column {
    Text(
      text = title,
      style = TextStyle(color = Color.Black, fontSize = 18.sp),
      fontWeight = FontWeight.Bold,
    )
    Row(
      modifier = Modifier.fillMaxWidth().padding(8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Text(
        text = "User",
        fontSize = 16.sp,
        color = LoginDarkColor,
        fontWeight = FontWeight.Normal,
      )
      Text(
        text = name,
        fontSize = 16.sp,
        color = LoginDarkColor,
        fontWeight = FontWeight.Bold,
      )
    }
    Row(
      modifier = Modifier.fillMaxWidth().padding(8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Text(
        text = "Team",
        fontSize = 16.sp,
        color = LoginDarkColor,
        fontWeight = FontWeight.Normal,
      )
      Text(
        text = team,
        fontSize = 16.sp,
        color = LoginDarkColor,
        fontWeight = FontWeight.Bold,
      )
    }
    Row(
      modifier = Modifier.fillMaxWidth().padding(8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Text(
        text = "Locality",
        fontSize = 16.sp,
        color = LoginDarkColor,
        fontWeight = FontWeight.Normal,
      )
      Text(
        text = locality,
        fontSize = 16.sp,
        color = LoginDarkColor,
        fontWeight = FontWeight.Bold,
      )
    }
    Divider(thickness = 1.dp, color = LoginDarkColor.copy(alpha = 0.67f))
    Spacer(modifier = Modifier.height(4.dp))
  }
}

@Composable
fun AppInfoView(
  title: String,
  userName: String,
  organization: String,
  careTeam: String,
  location: String,
) {
  Column {
    Text(
      text = title,
      style = TextStyle(color = Color.Black, fontSize = 18.sp),
      fontWeight = FontWeight.Bold,
    )
    Row(
      modifier = Modifier.fillMaxWidth().padding(8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Text(
        text = "Username",
        fontSize = 16.sp,
        color = LoginDarkColor,
        fontWeight = FontWeight.Normal,
      )
      Text(
        text = userName,
        fontSize = 16.sp,
        color = LoginDarkColor,
        fontWeight = FontWeight.Bold,
      )
    }
    Row(
      modifier = Modifier.fillMaxWidth().padding(8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Text(
        text = "Team(Organization)",
        fontSize = 16.sp,
        color = LoginDarkColor,
        fontWeight = FontWeight.Normal,
      )
      Text(
        text = organization,
        fontSize = 16.sp,
        color = LoginDarkColor,
        fontWeight = FontWeight.Bold,
      )
    }
    Row(
      modifier = Modifier.fillMaxWidth().padding(8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Text(
        text = "Care Team",
        fontSize = 16.sp,
        color = LoginDarkColor,
        fontWeight = FontWeight.Normal,
      )
      Text(
        text = careTeam,
        fontSize = 16.sp,
        color = LoginDarkColor,
        fontWeight = FontWeight.Bold,
      )
    }

    Row(
      modifier = Modifier.fillMaxWidth().padding(8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Text(
        text = "Location",
        fontSize = 16.sp,
        color = LoginDarkColor,
        fontWeight = FontWeight.Normal,
      )
      Text(
        text = location,
        fontSize = 16.sp,
        color = LoginDarkColor,
        fontWeight = FontWeight.Bold,
      )
    }

    Divider(thickness = 1.dp, color = LoginDarkColor.copy(alpha = 0.67f))
    Spacer(modifier = Modifier.height(4.dp))
  }
}

@Composable
fun AssignmentInfoView(
  title: String,
  appVersion: String,
  appVersionCode: String,
  databaseVersion: String,
  buildDate: String,
) {
  Column {
    Text(
      text = title,
      style = TextStyle(color = Color.Black, fontSize = 18.sp),
      fontWeight = FontWeight.Bold,
    )
    Row(
      modifier = Modifier.fillMaxWidth().padding(8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Text(
        text = "App Version",
        fontSize = 16.sp,
        color = LoginDarkColor,
        fontWeight = FontWeight.Normal,
      )
      Text(
        text = appVersion,
        fontSize = 16.sp,
        color = LoginDarkColor,
        fontWeight = FontWeight.Bold,
      )
    }
    Row(
      modifier = Modifier.fillMaxWidth().padding(8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Text(
        text = "App Version Code",
        fontSize = 16.sp,
        color = LoginDarkColor,
        fontWeight = FontWeight.Normal,
      )
      Text(
        text = appVersionCode,
        fontSize = 16.sp,
        color = LoginDarkColor,
        fontWeight = FontWeight.Bold,
      )
    }
    Row(
      modifier = Modifier.fillMaxWidth().padding(8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Text(
        text = "Database Version",
        fontSize = 16.sp,
        color = LoginDarkColor,
        fontWeight = FontWeight.Normal,
      )
      Text(
        text = databaseVersion,
        fontSize = 16.sp,
        color = LoginDarkColor,
        fontWeight = FontWeight.Bold,
      )
    }

    Row(
      modifier = Modifier.fillMaxWidth().padding(8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Text(
        text = "Build Date",
        fontSize = 16.sp,
        color = LoginDarkColor,
        fontWeight = FontWeight.Normal,
      )
      Text(
        text = buildDate,
        fontSize = 16.sp,
        color = LoginDarkColor,
        fontWeight = FontWeight.Bold,
      )
    }

    Divider(thickness = 1.dp, color = LoginDarkColor.copy(alpha = 0.67f))
    Spacer(modifier = Modifier.height(4.dp))
  }
}

@Composable
fun DeviceInfoView(
  title: String,
  manufacture: String,
  device: String,
  osVersion: String,
  date: String,
) {
  Column {
    Text(
      text = title,
      style = TextStyle(color = Color.Black, fontSize = 18.sp),
      fontWeight = FontWeight.Bold,
    )
    Row(
      modifier = Modifier.fillMaxWidth().padding(8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Text(
        text = "Manufacture",
        fontSize = 16.sp,
        color = LoginDarkColor,
        fontWeight = FontWeight.Normal,
      )
      Text(
        text = manufacture,
        fontSize = 16.sp,
        color = LoginDarkColor,
        fontWeight = FontWeight.Bold,
      )
    }
    Row(
      modifier = Modifier.fillMaxWidth().padding(8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Text(
        text = "Device",
        fontSize = 16.sp,
        color = LoginDarkColor,
        fontWeight = FontWeight.Normal,
      )
      Text(
        text = device,
        fontSize = 16.sp,
        color = LoginDarkColor,
        fontWeight = FontWeight.Bold,
      )
    }
    Row(
      modifier = Modifier.fillMaxWidth().padding(8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Text(
        text = "OS Version",
        fontSize = 16.sp,
        color = LoginDarkColor,
        fontWeight = FontWeight.Normal,
      )
      Text(
        text = osVersion,
        fontSize = 16.sp,
        color = LoginDarkColor,
        fontWeight = FontWeight.Bold,
      )
    }

    Row(
      modifier = Modifier.fillMaxWidth().padding(8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Text(
        text = "Date",
        fontSize = 16.sp,
        color = LoginDarkColor,
        fontWeight = FontWeight.Normal,
      )
      Text(
        text = date,
        fontSize = 16.sp,
        color = LoginDarkColor,
        fontWeight = FontWeight.Bold,
      )
    }

    Spacer(modifier = Modifier.height(4.dp))
  }
}

@Preview
@Composable
fun UserSettingInsightScreenPreview() {
  Column() {
    UserSettingInsightScreen(
      unsyncedResources = listOf(Pair("", 1)),
      syncedResources = listOf(Pair("", 1)),
      navController = rememberNavController(),
      onRefreshRequest = {},
    )
  }
}
