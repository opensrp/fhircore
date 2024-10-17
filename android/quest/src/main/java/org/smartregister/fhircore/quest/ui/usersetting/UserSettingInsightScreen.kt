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
import android.os.Build
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.primarySurface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.MutableSharedFlow
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.ui.theme.DividerColor
import org.smartregister.fhircore.engine.ui.theme.LoginDarkColor
import org.smartregister.fhircore.engine.util.extension.DEFAULT_FORMAT_SDF_DD_MM_YYYY
import org.smartregister.fhircore.engine.util.extension.formatDate

const val USER_INSIGHT_TOP_APP_BAR = "userInsightToAppBar"
const val INSIGHT_UNSYNCED_DATA = "insightUnsyncedData"

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
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
  dividerColor: Color = DividerColor,
  unsyncedResourcesFlow: MutableSharedFlow<List<Pair<String, Int>>>,
  navController: NavController,
  onRefreshRequest: () -> Unit,
  dateFormat: String = DEFAULT_FORMAT_SDF_DD_MM_YYYY,
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
  ) {
    LazyColumn(
      Modifier.fillMaxSize().background(Color.White),
      horizontalAlignment = Alignment.Start,
      contentPadding = PaddingValues(vertical = 24.dp, horizontal = 16.dp),
    ) {
      if (unsyncedResources.isNotEmpty()) {
        item {
          Text(
            text = stringResource(id = R.string.unsynced_resources),
            style = TextStyle(color = Color.Black, fontSize = 20.sp),
            fontWeight = FontWeight.Bold,
          )
          Spacer(modifier = Modifier.height(8.dp))
        }

        items(unsyncedResources) { unsynced ->
          UnsyncedDataView(
            first = unsynced.first,
            second = unsynced.second.toString(),
          )
        }

        item {
          Spacer(modifier = Modifier.height(16.dp))
          Divider(color = dividerColor)
          Spacer(modifier = Modifier.height(24.dp))
        }
      } else {
        item {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text(
              text = stringResource(id = R.string.all_resources_synced),
              style = TextStyle(color = Color.Black, fontSize = 20.sp),
              fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
              imageVector = Icons.Default.Check,
              contentDescription = null,
              tint = Color.Green,
              modifier = Modifier.size(32.dp),
            )
          }
        }
        item {
          Spacer(modifier = Modifier.height(16.dp))
          Divider(color = dividerColor)
          Spacer(modifier = Modifier.height(24.dp))
        }
      }
      item {
        if (fullName != null && team != null && locality != null) {
          val items =
            listOf(
              stringResource(R.string.user) to fullName,
              stringResource(R.string.team) to team,
              stringResource(R.string.locality) to locality,
            )

          InsightInfoView(
            title = stringResource(id = R.string.user_info),
            items = items,
          )
        }
      }
      item {
        val userNameItem =
          stringResource(id = R.string.username) to
            (if (userName.isNullOrEmpty()) "-" else userName!!)
        val organizationItem =
          stringResource(R.string.team_organization) to
            (if (organization.isNullOrEmpty()) "-" else organization.take(10))
        val careTeamItem =
          stringResource(R.string.care_team) to (if (careTeam.isNullOrEmpty()) "-" else careTeam)
        val locationItem =
          stringResource(R.string.location) to (if (location.isNullOrEmpty()) "-" else location)

        val items = listOf(userNameItem, organizationItem, careTeamItem, locationItem)
        InsightInfoView(
          title = stringResource(id = R.string.assignment_info),
          items = items,
        )
      }
      item {
        Spacer(modifier = Modifier.height(16.dp))
        Divider(color = dividerColor)
        Spacer(modifier = Modifier.height(24.dp))
      }

      item {
        val items =
          listOf(
            stringResource(R.string.app_versions) to appVersion,
            stringResource(R.string.app_version_code) to appVersionCode,
            stringResource(R.string.build_date) to buildDate,
          )
        InsightInfoView(
          title = stringResource(id = R.string.app_info),
          items = items,
        )
      }
      item {
        Spacer(modifier = Modifier.height(16.dp))
        Divider(color = dividerColor)
        Spacer(modifier = Modifier.height(24.dp))
      }

      item {
        val items =
          listOf(
            stringResource(R.string.manufacture) to
              (if (Build.MANUFACTURER.isNullOrEmpty()) "-" else Build.MANUFACTURER),
            stringResource(R.string.device) to
              (if (Build.DEVICE.isNullOrEmpty()) "-" else Build.DEVICE),
            stringResource(R.string.os_version) to
              (if (Build.VERSION.BASE_OS.isNullOrEmpty()) "-" else Build.VERSION.BASE_OS),
            stringResource(R.string.device_date) to
              (formatDate(Build.TIME, desireFormat = dateFormat).ifEmpty { "-" }),
          )
        InsightInfoView(
          title = stringResource(id = R.string.device_info),
          items = items,
        )
      }

      item {
        Column(
          Modifier.wrapContentWidth().wrapContentHeight().padding(4.dp),
        ) {
          Spacer(modifier = Modifier.height(16.dp))
          Surface(shape = RoundedCornerShape(0.dp)) {
            OutlinedButton(
              modifier = Modifier.fillMaxWidth(),
              onClick = onRefreshRequest,
              border = BorderStroke(0.7.dp, MaterialTheme.colors.primarySurface),
            ) {
              Text(
                text = stringResource(R.string.refresh).uppercase(),
                modifier = Modifier.padding(6.dp),
                style =
                  TextStyle(
                    color = MaterialTheme.colors.primarySurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
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
  Column(verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Top)) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Text(
        text = first,
        modifier = Modifier.testTag(INSIGHT_UNSYNCED_DATA),
        fontSize = 16.sp,
        color = colorResource(id = R.color.grayText),
        fontWeight = FontWeight.Medium,
      )
      Text(
        text = second,
        color = LoginDarkColor,
        fontWeight = FontWeight.Bold,
      )
    }
  }

  Spacer(modifier = Modifier.height(8.dp))
}

@Composable
fun InsightInfoView(
  title: String,
  items: List<Pair<String, String>>,
  headerTextStyle: TextStyle =
    TextStyle(color = Color.Gray, fontSize = 16.sp, fontWeight = FontWeight.Medium),
  contentTextStyle: TextStyle =
    TextStyle(color = LoginDarkColor, fontSize = 16.sp, fontWeight = FontWeight.Bold),
) {
  Text(
    text = title,
    style = TextStyle(color = Color.Black, fontSize = 20.sp, fontWeight = FontWeight.Bold),
  )
  Spacer(modifier = Modifier.height(16.dp))

  Column(verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Top)) {
    for ((header, content) in items) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(
          text = header,
          style = headerTextStyle,
        )
        Text(
          text = content,
          style = contentTextStyle,
          softWrap = true,
          overflow = TextOverflow.Ellipsis,
          maxLines = 1,
        )
      }
    }

    Spacer(modifier = Modifier.height(8.dp))
  }
}

fun formatTimestamp(timestamp: Long): String {
  val date = Date(timestamp * 1000)
  val sdf = SimpleDateFormat("MMM dd, yyyy h:mm a", Locale.getDefault())
  return sdf.format(date)
}

@Preview
@Composable
fun UserSettingInsightScreenPreview() {
  Column {
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
      dateFormat = DEFAULT_FORMAT_SDF_DD_MM_YYYY,
    )
  }
}
