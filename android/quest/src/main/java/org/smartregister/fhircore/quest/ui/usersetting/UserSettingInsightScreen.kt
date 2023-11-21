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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.primarySurface
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import org.smartregister.fhircore.engine.ui.theme.DividerColor
import org.smartregister.fhircore.engine.ui.theme.LoginDarkColor
import org.smartregister.fhircore.quest.R

@Composable
fun UserSettingInsightScreen(
    unsyncedResources: List<Pair<String, Int>>,
    syncedResources: List<Pair<String, Int>>,
    mainNavController: NavController,
) {
  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text(text = stringResource(R.string.insights)) },
        navigationIcon = {
          IconButton(onClick = { mainNavController.popBackStack()}) {
            Icon(Icons.Filled.ArrowBack, null)
          }
        },
        contentColor = Color.White,
        backgroundColor = MaterialTheme.colors.primary,
      )
    },
    backgroundColor = Color.White,
  ) { paddingValues->

        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.White),
            horizontalAlignment = Alignment.Start,

            ) {

            item{
                Text(
                    text = if (unsyncedResources.isNullOrEmpty()) {
                        "stringResource(id = R.string.synced_statistics)"
                    } else stringResource(id = R.string.unsynced_resources),
                    modifier = Modifier.padding(11.dp),
                    style = TextStyle(color = Color.Black, fontSize = 18.sp),
                    fontWeight = FontWeight.Bold,
                )
            }

            items(unsyncedResources){ unsynced->
                Row(
                    Modifier
                        .fillMaxWidth(),

                        horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
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
                    Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = synced.first,
                        fontWeight = FontWeight.Light,
                    )
                    Text(
                        text = synced.second.toString(),
                    )
                }

            }
            item {
                Divider(color = DividerColor)

                Spacer(modifier = Modifier.padding(8.dp))
            }

    /*   InfoView(title = stringResource(id = R.string.user_info), data = unsyncedResources)
       InfoView(title = stringResource(id = R.string.app_info), data = unsyncedResources)
       InfoView(
           title = stringResource(id = R.string.assignment_info),
           data = unsyncedResources
       )
       InfoView(title = stringResource(id = R.string.device_info), data = unsyncedResources)*/
     item {
       Column(
         Modifier
           .wrapContentWidth()
           .wrapContentHeight()
           .padding(4.dp)
       ) {
         Surface(shape = RoundedCornerShape(0.dp)) {
           OutlinedButton(
             modifier = Modifier.width(300.dp),
             onClick = {},
             border = BorderStroke(0.7.dp, MaterialTheme.colors.primarySurface),
           ) {
             Text(
               text = "stringResource(R.string.refresh)",
               modifier = Modifier.padding(6.dp),
               style = TextStyle(
                 color = MaterialTheme.colors.primarySurface,
                 fontSize = 14.sp
               ),
             )
           }
         }
       }
     }
  }
}


@Composable
fun InfoView(
  title: String,
  data: List<Pair<String, Int>>,
  modifier: Modifier = Modifier,
  textColor: Color = LoginDarkColor,
) {

  LazyColumn(
  ) {
    item {
      Text(
        text = title,
        fontSize = 18.sp,
        color = textColor,
        fontWeight = FontWeight.Bold
      )
      Spacer(modifier = modifier.height(4.dp))
    }
    items(data) { info ->
      Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Text(
          text = info.first,
          fontSize = 16.sp,
          color = textColor,
          fontWeight = FontWeight.Normal
        )
        Text(
          text = info.second.toString(),
          fontSize = 16.sp,
          color = textColor,
          fontWeight = FontWeight.Normal
        )
      }
    }


  }
  Divider(color = DividerColor)
}
  /*Box(Modifier.clip(RectangleShape).fillMaxWidth().background(Color.White)) {
    Dialog(onDismissRequest = onDismissRequest) {
      Column(
        modifier = Modifier.fillMaxWidth().wrapContentHeight().background(Color.White),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Text(
          text = stringResource(id = R.string.unsynced_resources),
          modifier = Modifier.padding(11.dp),
          style = TextStyle(color = Color.Black, fontSize = 20.sp),
          fontWeight = FontWeight.Light,
        )
        LazyColumn(modifier = Modifier.wrapContentHeight()) {
          items(unsyncedResources) { language ->
            Box(Modifier.fillMaxWidth().padding(15.dp)) {
              Text(
                text = language.first,
                modifier = Modifier.align(Alignment.CenterStart),
                fontWeight = FontWeight.Light,
              )
              Text(
                text = language.second.toString(),
                modifier = Modifier.align(Alignment.CenterEnd),
              )
            }
            Spacer(modifier = Modifier.padding(1.dp))
          }
        }
        Column(Modifier.wrapContentWidth().wrapContentHeight().padding(4.dp)) {
          Surface(shape = RoundedCornerShape(0.dp)) {
            OutlinedButton(
              onClick = onDismissRequest,
              border = BorderStroke(0.7.dp, MaterialTheme.colors.primarySurface),
            ) {
              Text(
                text = stringResource(R.string.dismiss),
                modifier = Modifier.padding(6.dp),
                style = TextStyle(color = MaterialTheme.colors.primarySurface, fontSize = 14.sp),
              )
            }
          }
        }
      }
    }
  }*/
}

@Preview
@Composable
fun UserSettingInsightScreenPreview() {
  Column() {
    UserSettingInsightScreen(
        unsyncedResources = listOf(Pair("", 1)),
        syncedResources = listOf(Pair("", 1)),
        mainNavController = rememberNavController(),
    )
  }
}
