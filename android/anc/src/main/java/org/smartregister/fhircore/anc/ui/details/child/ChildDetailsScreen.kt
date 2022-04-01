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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.smartregister.fhircore.anc.ui.details.child.components.ChildProfileTaskRow
import org.smartregister.fhircore.anc.ui.details.child.components.PersonalData
import org.smartregister.fhircore.anc.ui.details.child.model.ChildProfileViewData
import org.smartregister.fhircore.engine.R

@Composable
fun ChildDetailsScreen(
  onBackPress: () -> Unit,
  childProfileViewData: ChildProfileViewData,
  modifier: Modifier = Modifier,
) {

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text(text = "") },
        navigationIcon = {
          IconButton(onClick = onBackPress) { Icon(Icons.Filled.ArrowBack, null) }
        }
      )
    }
  ) { innerPadding ->
    Box(modifier = modifier.padding(innerPadding)) {
      LazyColumn {
        // Personal Data: e.g. sex, age, dob
        item { PersonalData(childProfileViewData) }

        item { Text(stringResource(R.string.tasks).uppercase(), modifier.padding(8.dp)) }
        // Patient tasks: List of tasks for the patients
        items(items = childProfileViewData.tasks) {
          Card(
            elevation = 3.dp,
            shape = RoundedCornerShape(6.dp),
            modifier = modifier.fillMaxWidth()
          ) { ChildProfileTaskRow(it) }
        }
      }
    }
  }
}
