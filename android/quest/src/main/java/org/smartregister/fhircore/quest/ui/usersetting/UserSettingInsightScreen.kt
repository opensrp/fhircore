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

import android.annotation.SuppressLint
import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.flow.MutableSharedFlow
import org.smartregister.fhircore.engine.ui.theme.DividerColor
import org.smartregister.fhircore.engine.ui.theme.LoginDarkColor
import org.smartregister.fhircore.quest.R

const val USER_INSIGHT_TOP_APP_BAR = "userInsightToAppBar"
const val INSIGHT_UNSYNCED_DATA = "insightUnsynceData"

@Composable
fun UserSettingInsightScreen(
  fullName: String?,
  team: String?,
  locality: String?,
  userName: String?,
  organization: String?,
  careTeam: String?,
  location: String?,
  appVersionCode: String,
  appVersion: String,
  buildDate: String,
  unsyncedResourcesFlow: MutableSharedFlow<List<Pair<String, Int>>>,
  navController: NavController,
  onRefreshRequest: () -> Unit,
) {
  val unsyncedResources = unsyncedResourcesFlow.collectAsState(initial = listOf()).value

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text(text = stringResource(R.string.insights)) },
        navigationIcon = {
          IconButton(onClick = { navController.popBackStack() }) {
            Icon(
              imageVector = Icons.Filled.ArrowBack,
              modifier = Modifier.testTag(USER_INSIGHT_TOP_APP_BAR),
              contentDescription = null,
            )
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
          text = stringResource(id = R.string.unsynced_resources),
          style = TextStyle(color = Color.Black, fontSize = 18.sp),
          fontWeight = FontWeight.Bold,
        )
      }
      if (unsyncedResources.isNotEmpty()) {
        items(unsyncedResources) { unsynced ->
          UnsyncedDataView(
            first = unsynced.first,
            second = unsynced.second.toString(),
          )
          Divider(color = DividerColor)
          Spacer(modifier = Modifier.padding(8.dp))
        }
      }
      item {
        UserInfoView(
          title = stringResource(id = R.string.user_info),
          name = fullName ?: "",
          team = team ?: "",
          locality = locality ?: "",
        )
      }
      item {
        AppInfoView(
          title = stringResource(id = R.string.app_info),
          userName = userName ?: "",
          organization = organization ?: "",
          careTeam = careTeam ?: "",
          location = location ?: "",
        )
      }

      item {
        AssignmentInfoView(
          title = stringResource(id = R.string.assignment_info),
          appVersion = appVersion,
          appVersionCode = appVersionCode,
          buildDate = buildDate,
        )
      }

      item {
        DeviceInfoView(
          title = stringResource(id = R.string.device_info),
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
fun UnsyncedDataView(
  first: String,
  second: String,
) {
  Box(
    Modifier.fillMaxWidth().padding(15.dp),
  ) {
    Text(
      text = first,
      modifier = Modifier.align(Alignment.CenterStart).testTag(INSIGHT_UNSYNCED_DATA),
      color = LoginDarkColor,
      fontWeight = FontWeight.Normal,
    )
    Text(
      modifier = Modifier.align(Alignment.CenterEnd),
      text = second,
      color = LoginDarkColor,
      fontWeight = FontWeight.Bold,
    )
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
        text = stringResource(R.string.user),
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
        text = stringResource(R.string.team),
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
        text = stringResource(R.string.locality),
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
        text = stringResource(id = R.string.username),
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
        text = stringResource(R.string.team_organization),
        fontSize = 16.sp,
        color = LoginDarkColor,
        fontWeight = FontWeight.Normal,
      )
      Text(
        text = organization.take(10),
        fontSize = 16.sp,
        color = LoginDarkColor,
        fontWeight = FontWeight.Bold,
        softWrap = true,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
    Row(
      modifier = Modifier.fillMaxWidth().padding(8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Text(
        text = stringResource(R.string.care_team),
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
        text = stringResource(R.string.location),
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
        text = stringResource(R.string.app_versions),
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
        text = stringResource(R.string.app_version_code),
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
        text = stringResource(R.string.build_date),
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
@SuppressLint("HadwareIds")
fun DeviceInfoView(
  title: String,
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
        text = stringResource(R.string.manufacture),
        fontSize = 16.sp,
        color = LoginDarkColor,
        fontWeight = FontWeight.Normal,
      )
      Text(
        text = Build.MANUFACTURER,
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
        text = stringResource(R.string.device),
        fontSize = 16.sp,
        color = LoginDarkColor,
        fontWeight = FontWeight.Normal,
      )
      Text(
        text = Build.DEVICE,
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
        text = stringResource(R.string.os_version),
        fontSize = 16.sp,
        color = LoginDarkColor,
        fontWeight = FontWeight.Normal,
      )
      Text(
        text = Build.VERSION.RELEASE,
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
        text = stringResource(R.string.model),
        fontSize = 16.sp,
        color = LoginDarkColor,
        fontWeight = FontWeight.Normal,
      )
      Text(
        text = Build.MODEL,
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
      fullName = "Tembo",
      team = "Team_tembo",
      locality = "Ps Dev-a",
      userName = "user_name",
      organization = "team_organization",
      careTeam = "care_team",
      location = "location",
      appVersionCode = "v2.3.4",
      appVersion = "119",
      buildDate = "29 Jan 2023",
      unsyncedResourcesFlow = MutableSharedFlow(),
      navController = rememberNavController(),
      onRefreshRequest = {},
    )
  }
}
