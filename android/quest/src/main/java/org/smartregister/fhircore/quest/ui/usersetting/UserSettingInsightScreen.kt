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
import org.smartregister.fhircore.engine.ui.theme.DividerColor
import org.smartregister.fhircore.engine.ui.theme.LoginDarkColor
import org.smartregister.fhircore.quest.R

const val USER_INSIGHT_TOP_APP_BAR = "userInsightToAppBar"
const val INSIGHT_UNSYNCED_DATA = "insightUnsynceData"

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
  ) {
    LazyColumn(
      Modifier.fillMaxSize().background(Color.White),
      horizontalAlignment = Alignment.Start,
      contentPadding = PaddingValues(vertical = 24.dp, horizontal = 16.dp),
    ) {
      item {
        Text(
          text = stringResource(id = R.string.unsynced_resources),
          style = TextStyle(color = Color.Black, fontSize = 20.sp),
          fontWeight = FontWeight.Bold,
        )
        if (unsyncedResources.isEmpty()) {
          Spacer(modifier = Modifier.height(24.dp))
          Divider(color = DividerColor)
        }
      }
      if (unsyncedResources.isNotEmpty()) {
        items(unsyncedResources) { unsynced ->
          UnsyncedDataView(
            first = unsynced.first,
            second = unsynced.second.toString(),
          )
          Spacer(modifier = Modifier.height(24.dp))
          Divider(color = DividerColor)
        }
      }
      item {
        InsightInfoView(
          title = stringResource(id = R.string.user_info),
          headingOne = stringResource(R.string.user),
          contentOne = fullName,
          headingTwo = stringResource(R.string.team),
          contentTwo = team,
          headingThree = stringResource(R.string.locality),
          contentThree = locality,
          headingFour = null,
          contentFour = null,
        )
      }
      item {
        InsightInfoView(
          title = stringResource(id = R.string.app_info),
          headingOne = stringResource(id = R.string.username),
          contentOne = userName,
          headingTwo = stringResource(R.string.team_organization),
          contentTwo = organization?.take(10),
          headingThree = stringResource(R.string.care_team),
          contentThree = careTeam,
          headingFour = stringResource(R.string.location),
          contentFour = location,
        )
      }

      item {
        InsightInfoView(
          title = stringResource(id = R.string.assignment_info),
          headingOne = stringResource(R.string.app_versions),
          contentOne = appVersion,
          headingTwo = stringResource(R.string.app_version_code),
          contentTwo = appVersionCode,
          headingThree = stringResource(R.string.build_date),
          contentThree = buildDate,
          headingFour = null,
          contentFour = null,
        )
      }

      item {
        InsightInfoView(
          title = stringResource(id = R.string.device_info),
          headingOne = stringResource(R.string.manufacture),
          contentOne = Build.MANUFACTURER,
          headingTwo = stringResource(R.string.device),
          contentTwo = Build.DEVICE,
          headingThree = stringResource(R.string.os_version),
          contentThree = Build.VERSION.BASE_OS,
          headingFour = stringResource(R.string.device_date),
          contentFour = formatTimestamp(Build.TIME),
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
  Box(
    Modifier.fillMaxWidth().padding(16.dp),
  ) {
    Text(
      text = first,
      modifier = Modifier.align(Alignment.CenterStart).testTag(INSIGHT_UNSYNCED_DATA),
      fontSize = 16.sp,
      color = colorResource(id = R.color.grayText),
      fontWeight = FontWeight.Medium,
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
fun InsightInfoView(
  title: String,
  headingOne: String?,
  contentOne: String?,
  headingTwo: String?,
  contentTwo: String?,
  headingThree: String?,
  contentThree: String?,
  headingFour: String?,
  contentFour: String?,
) {
  Spacer(modifier = Modifier.height(24.dp))

  Text(
    text = title,
    style = TextStyle(color = Color.Black, fontSize = 20.sp),
    fontWeight = FontWeight.Bold,
  )
  Spacer(modifier = Modifier.height(16.dp))

  Column(verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Top)) {
    if (headingOne != null && contentOne != null) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(
          text = headingOne,
          fontSize = 16.sp,
          color = colorResource(id = R.color.grayText),
          fontWeight = FontWeight.Medium,
        )
        Text(
          text = contentOne,
          fontSize = 16.sp,
          color = LoginDarkColor,
          fontWeight = FontWeight.Bold,
        )
      }
    }
    if (headingTwo != null && contentTwo != null) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(
          text = headingTwo,
          fontSize = 16.sp,
          color = colorResource(id = R.color.grayText),
          fontWeight = FontWeight.Medium,
        )
        Text(
          text = contentTwo,
          fontSize = 16.sp,
          color = LoginDarkColor,
          fontWeight = FontWeight.Bold,
          softWrap = true,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
    if (headingThree != null && contentThree != null) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(
          text = headingThree,
          fontSize = 16.sp,
          color = colorResource(id = R.color.grayText),
          fontWeight = FontWeight.Medium,
        )
        Text(
          text = contentThree,
          fontSize = 16.sp,
          color = LoginDarkColor,
          fontWeight = FontWeight.Bold,
        )
      }
    }

    if (headingFour != null && contentFour != null) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(
          text = headingFour,
          fontSize = 16.sp,
          color = colorResource(id = R.color.grayText),
          fontWeight = FontWeight.Medium,
        )
        Text(
          text = contentFour,
          fontSize = 16.sp,
          color = LoginDarkColor,
          fontWeight = FontWeight.Bold,
        )
      }
    }
    Spacer(modifier = Modifier.height(16.dp))
    Divider(color = DividerColor)
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
    )
  }
}
