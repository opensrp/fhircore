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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Card
import androidx.compose.material.Chip
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.smartregister.fhircore.engine.ui.theme.BlueTextColor
import org.smartregister.fhircore.engine.ui.theme.LighterBlue

@Composable
fun InfoCard(viewModel: SettingsViewModel) {
  val data by viewModel.data.observeAsState()

  if (data != null) {
    val username = data!!.userName
    if (!username.isNullOrEmpty()) {
      Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        Box(
          modifier = Modifier.clip(CircleShape).background(color = LighterBlue).size(80.dp),
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
          modifier = Modifier.padding(vertical = 22.dp),
          fontWeight = FontWeight.Bold
        )
      }
    }
    Card(Modifier.padding(6.dp)) {
      Column(Modifier.padding(10.dp)) {
        generateData(data!!).forEach {
          Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
            Text(text = it.key, style = MaterialTheme.typography.h6)
            Box(modifier = Modifier.height(8.dp))
            it.value.map { FieldCard(it) }
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun FieldCard(fieldData: FieldData) {
  var show by remember { mutableStateOf(false) }

  Card(onClick = { show = !show }, modifier = Modifier.fillMaxWidth()) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(8.dp)) {
      Text(text = fieldData.value)

      if (show) {
        Chip(onClick = { /*TODO*/}) { Text(text = fieldData.id) }
      }
    }
  }
}

fun generateData(data: ProfileData): Map<String, List<FieldData>> {
  val defaultValue = FieldData("", "Not Defined")
  val map = mutableMapOf<String, List<FieldData>>()

  map["Location"] = data.locations.ifEmpty { listOf(defaultValue) }
  map["Organisation"] = data.organisations.ifEmpty { listOf(defaultValue) }
  map["CareTeam"] = data.careTeams.ifEmpty { listOf(defaultValue) }
  return map
}
