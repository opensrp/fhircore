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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import org.smartregister.fhircore.quest.R

@Composable
fun UserSettingInsightScreen(
  unsyncedResources: List<Pair<String, Int>>,
  onDismissRequest: () -> Unit
) {
  Box(Modifier.clip(RectangleShape).fillMaxWidth().background(Color.White)) {
    Dialog(onDismissRequest = onDismissRequest) {
      Column(
        modifier = Modifier.fillMaxWidth().wrapContentHeight().background(Color.White),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Text(
          text = stringResource(id = R.string.unsynced_resources),
          modifier = Modifier.padding(11.dp),
          style = TextStyle(color = Color.Black, fontSize = 20.sp),
          fontWeight = FontWeight.Light
        )
        LazyColumn(modifier = Modifier.wrapContentHeight()) {
          items(unsyncedResources) { language ->
            Box(Modifier.fillMaxWidth().padding(15.dp)) {
              Text(
                text = language.first,
                modifier = Modifier.align(Alignment.CenterStart),
                fontWeight = FontWeight.Light
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
              border = BorderStroke(0.7.dp, MaterialTheme.colors.primarySurface)
            ) {
              Text(
                text = stringResource(R.string.dismiss),
                modifier = Modifier.padding(6.dp),
                style = TextStyle(color = MaterialTheme.colors.primarySurface, fontSize = 14.sp)
              )
            }
          }
        }
      }
    }
  }
}
