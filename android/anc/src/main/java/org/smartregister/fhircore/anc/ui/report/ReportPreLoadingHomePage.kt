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

package org.smartregister.fhircore.anc.ui.report

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import org.smartregister.fhircore.anc.R

@Composable
fun ReportPreLoadingHomeScreen(viewModel: ReportViewModel) {
  ReportPreLoadingHomePage(
    topBarTitle = stringResource(id = R.string.reports),
    onBackPress = viewModel::onBackPress
  )
}

@Composable
fun ReportPreLoadingHomePage(topBarTitle: String, onBackPress: () -> Unit) {
  Surface(color = colorResource(id = R.color.white)) {
    Column(modifier = Modifier.fillMaxSize()) {
      TopBarBox(topBarTitle, onBackPress)
      Box(modifier = Modifier.fillMaxSize()) {
        Column(
          modifier = Modifier.align(Alignment.Center),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Text(text = "Loading libraries..")
          LoadingItem()
        }
      }
    }
  }
}
