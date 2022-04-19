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

package org.smartregister.fhircore.quest.ui.family.removemember

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import org.smartregister.fhircore.engine.ui.theme.LighterGrayBackgroundColor
import org.smartregister.fhircore.engine.ui.theme.StatusTextColor
import org.smartregister.fhircore.quest.R

@Composable
fun RemoveMemberScreen(
  title: String,
  status: String,
  id: String,
  navController: NavHostController,
  modifier: Modifier = Modifier
) {
  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text(text = stringResource(R.string.remove_member)) },
        backgroundColor = MaterialTheme.colors.primary,
        navigationIcon = {
          IconButton(onClick = { navController.popBackStack() }) {
            Icon(Icons.Filled.ArrowBack, contentDescription = null)
          }
        }
      )
    }
  ) { innerPadding ->
    Box(modifier = modifier.padding(innerPadding)) {
      Column(verticalArrangement = Arrangement.SpaceBetween, modifier = modifier.fillMaxHeight()) {
        Column {
          MemberDetails(modifier, title, status, id)
          Spacer(modifier = modifier.height(8.dp))
          Column(modifier = modifier.fillMaxWidth().background(Color.White).padding(16.dp)) {
            Text(text = stringResource(R.string.reason_for_removing), fontSize = 18.sp)
            // Dropdown and date picker
          }
        }
        Button(
          modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
          onClick = { /*Todo remove member on click*/}
        ) {
          Text(
            text = stringResource(id = R.string.remove_member).uppercase(),
            modifier = modifier.padding(8.dp)
          )
        }
      }
    }
  }
}

@Composable
private fun MemberDetails(modifier: Modifier, title: String, status: String, id: String) {
  Column(modifier = modifier.fillMaxWidth().background(LighterGrayBackgroundColor).padding(16.dp)) {
    Text(
      text = title,
      fontSize = 20.sp,
      fontWeight = FontWeight.SemiBold,
      modifier = modifier.padding(bottom = 4.dp)
    )
    SubtitleText(text = status, modifier = modifier)
    SubtitleText(text = stringResource(R.string.id, id), modifier = modifier)
    Spacer(modifier = modifier.height(4.dp))
  }
}

@Composable
private fun SubtitleText(text: String, modifier: Modifier) {
  Text(
    text = text,
    color = StatusTextColor,
    fontSize = 18.sp,
    maxLines = 1,
    overflow = TextOverflow.Ellipsis
  )
  Spacer(modifier = modifier.height(4.dp))
}

@Preview(showBackground = true)
@Composable
fun RemoveMemberScreenPreview() {
  RemoveMemberScreen(
    title = "Angelina Jolie",
    status = "Family head",
    id = "3034503",
    navController = rememberNavController()
  )
}
