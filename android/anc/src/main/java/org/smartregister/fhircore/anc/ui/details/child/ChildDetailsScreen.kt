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

package org.smartregister.fhircore.anc.ui.details.child

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.smartregister.fhircore.anc.ui.details.child.components.ChildProfileTaskRow
import org.smartregister.fhircore.anc.ui.details.child.components.PersonalData
import org.smartregister.fhircore.anc.ui.details.child.model.ChildProfileViewData
import org.smartregister.fhircore.engine.R

@Composable
fun ChildDetailsScreen(
  childProfileViewData: ChildProfileViewData,
  onTaskRowClick: (String) -> Unit,
  onBackPress: () -> Unit,
  modifier: Modifier = Modifier,
) {

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text(text = "") },
        navigationIcon = {
          IconButton(onClick = onBackPress) { Icon(Icons.Filled.ArrowBack, null) }
        },
        backgroundColor = colorResource(id = R.color.colorPrimary),
        contentColor = Color.White
      )
    }
  ) { innerPadding ->
    Box(modifier = modifier.padding(innerPadding)) {
      Column(
        modifier = modifier.background(Color(0xFFF2F4F7)).verticalScroll(rememberScrollState())
      ) {
        // Personal Data: e.g. sex, age, dob
        PersonalData(childProfileViewData)

        Text(
          stringResource(R.string.tasks).uppercase(),
          modifier = modifier.padding(top = 32.dp, start = 32.dp, bottom = 16.dp),
          fontWeight = FontWeight.Bold,
          color = Color.Gray,
          fontSize = 18.sp
        )

        // Patient tasks: List of tasks for the patients
        Card(modifier = modifier.padding(16.dp), elevation = 3.dp) {
          Column {
            childProfileViewData.tasks.forEach {
              ChildProfileTaskRow(childProfileRowItem = it, onRowClick = onTaskRowClick)
              Divider()
            }
          }
        }
      }
    }
  }
}

@Composable
@Preview(showBackground = true)
fun ChildDetailsScreenPreview() {
  ChildDetailsScreen(
    childProfileViewData =
      ChildProfileViewData(
        "Jane Eod",
        status = "01-02-2022",
        sex = "Female",
        age = "4",
        dob = "23 Aug",
        id = "12334"
      ),
    onTaskRowClick = {},
    onBackPress = { /*TODO*/}
  )
}
