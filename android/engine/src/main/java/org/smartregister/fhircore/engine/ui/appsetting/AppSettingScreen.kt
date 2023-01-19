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

package org.smartregister.fhircore.engine.ui.appsetting

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.ui.components.CircularProgressBar
import org.smartregister.fhircore.engine.util.annotation.PreviewWithBackgroundExcludeGenerated

const val APP_ID_TEXT_INPUT_TAG = "appIdTextInputTag"

@Composable
fun AppSettingScreen(
  modifier: Modifier = Modifier,
  appId: String,
  onAppIdChanged: (String) -> Unit,
  onLoadConfigurations: (Boolean) -> Unit,
  showProgressBar: Boolean = false
) {

  Column(
    verticalArrangement = Arrangement.Center,
    modifier = modifier.fillMaxSize().padding(horizontal = 20.dp)
  ) {
    Text(
      text = stringResource(R.string.fhir_core_app),
      fontWeight = FontWeight.Bold,
      textAlign = TextAlign.Center,
      fontSize = 32.sp,
      modifier = modifier.padding(vertical = 8.dp).align(Alignment.CenterHorizontally)
    )
    Spacer(modifier = modifier.height(20.dp))
    Text(
      text = stringResource(R.string.application_id),
      modifier = modifier.padding(vertical = 8.dp)
    )
    OutlinedTextField(
      onValueChange = onAppIdChanged,
      value = appId,
      maxLines = 1,
      singleLine = true,
      placeholder = {
        Text(
          color = Color.LightGray,
          text = stringResource(R.string.enter_app_id),
        )
      },
      modifier = modifier.testTag(APP_ID_TEXT_INPUT_TAG).fillMaxWidth().padding(vertical = 2.dp)
    )
    Text(
      text = stringResource(R.string.app_id_sample),
      fontSize = 12.sp,
      modifier = modifier.padding(vertical = 8.dp)
    )
    Spacer(modifier = modifier.height(20.dp))
    Box(contentAlignment = Alignment.Center, modifier = modifier.fillMaxWidth()) {
      Button(
        onClick = { onLoadConfigurations(true) },
        enabled = !showProgressBar && appId.isNotEmpty(),
        modifier = modifier.fillMaxWidth()
      ) {
        Text(
          color = Color.White,
          text = stringResource(id = R.string.load_configurations),
          modifier = modifier.padding(8.dp)
        )
      }
      if (showProgressBar) {
        CircularProgressBar(modifier = modifier.matchParentSize().padding(4.dp))
      }
    }
  }
}

@PreviewWithBackgroundExcludeGenerated
@Composable
private fun AppSettingScreenPreview() {
  AppSettingScreen(appId = "", onAppIdChanged = {}, onLoadConfigurations = {})
}
