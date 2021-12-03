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

package org.smartregister.fhircore.quest.ui.patient.details

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.smartregister.fhircore.quest.R

const val PATIENT_BIO_INFO = "patientBioInfo"

@Composable
fun QuestPatientTestResultScreen(questPatientDetailViewModel: QuestPatientDetailViewModel) {
  val patientItem by questPatientDetailViewModel.patientItem.observeAsState(null)

  Surface(color = colorResource(id = R.color.white_smoke)) {
    Column {
      TopAppBar(
        title = {
          Text(
            text = stringResource(id = R.string.back_to_clients),
            Modifier.testTag(TOOLBAR_TITLE)
          )
        },
        navigationIcon = {
          IconButton(
            onClick = { questPatientDetailViewModel.onBackPressed(true) },
            Modifier.testTag(TOOLBAR_BACK_ARROW)
          ) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back arrow") }
        }
      )
      Column(
        modifier =
          Modifier.fillMaxWidth()
            .background(color = colorResource(id = R.color.colorPrimary))
            .padding(12.dp)
            .testTag(PATIENT_BIO_INFO)
      ) {
        Text(
          text = patientItem?.name ?: "",
          color = colorResource(id = R.color.white),
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = "${patientItem?.gender} - ${patientItem?.age ?: ""}",
          color = colorResource(id = R.color.cornflower_blue),
          fontSize = 16.sp
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
          text = stringResource(R.string.patient_test_results),
          color = colorResource(id = R.color.cornflower_blue),
          fontSize = 16.sp
        )
      }

      Column(
        modifier =
          Modifier.fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 12.dp, end = 12.dp)
      ) {
        Spacer(Modifier.height(24.dp))
        Text(
          text = stringResource(R.string.no_test_results_found),
          color = colorResource(id = R.color.grayText),
          fontSize = 15.sp,
        )
      }
    }
  }
}
