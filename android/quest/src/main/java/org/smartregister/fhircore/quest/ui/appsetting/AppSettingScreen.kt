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

@file:OptIn(ExperimentalFoundationApi::class)

package org.smartregister.fhircore.quest.ui.appsetting

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.util.annotation.PreviewWithBackgroundExcludeGenerated
import org.smartregister.fhircore.engine.util.extension.appVersion

const val APP_ID_TEXT_INPUT_TAG = "appIdTextInputTag"

@Composable
fun AppSettingScreen(
  modifier: Modifier = Modifier,
  appId: String,
  onAppIdChanged: (String) -> Unit,
  onLoadConfigurations: (Boolean) -> Unit,
  showProgressBar: Boolean = false,
  appVersionPair: Pair<Int, String>? = null
) {
  val context = LocalContext.current
  val (versionCode, versionName) = remember { appVersionPair ?: context.appVersion() }
  val coroutineScope = rememberCoroutineScope()
  val bringIntoViewRequester = BringIntoViewRequester()
  val focusRequester = remember { FocusRequester() }

  LaunchedEffect(Unit) {
    delay(300)
    focusRequester.requestFocus()
  }

  Column(modifier = modifier.fillMaxSize()) {
    Column(
      verticalArrangement = Arrangement.Center,
      modifier = modifier.weight(1f).padding(horizontal = 20.dp)
    ) {
      Text(
        text = stringResource(R.string.fhir_core_app),
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        fontSize = 32.sp,
        modifier = modifier.padding(vertical = 8.dp).align(Alignment.CenterHorizontally)
      )
      Spacer(modifier = modifier.height(80.dp))
      Text(
        text = stringResource(R.string.application_id),
        modifier = modifier.padding(vertical = 4.dp)
      )
      OutlinedTextField(
        onValueChange = onAppIdChanged,
        value = appId,
        maxLines = 1,
        singleLine = true,
        placeholder = {
          Text(
            color = Color.LightGray,
            text = stringResource(R.string.app_id_sample),
          )
        },
        modifier =
          modifier
            .testTag(APP_ID_TEXT_INPUT_TAG)
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .onFocusEvent { event ->
              if (event.isFocused) coroutineScope.launch { bringIntoViewRequester.bringIntoView() }
            }
            .focusRequester(focusRequester)
      )

      Spacer(modifier = modifier.height(30.dp))
      Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.bringIntoViewRequester(bringIntoViewRequester).fillMaxWidth()
      ) {
        Button(
          onClick = { onLoadConfigurations(true) },
          enabled = !showProgressBar && appId.isNotEmpty(),
          modifier = modifier.fillMaxWidth(),
          colors =
            ButtonDefaults.buttonColors(
              disabledContentColor = Color.Gray,
              contentColor = Color.White
            ),
          elevation = null
        ) {
          Text(
            text = if (!showProgressBar) stringResource(id = R.string.load_configurations) else "",
            modifier = modifier.padding(8.dp)
          )
        }
        if (showProgressBar) {
          CircularProgressIndicator(
            modifier = modifier.align(Alignment.Center).size(18.dp),
            strokeWidth = 1.6.dp,
            color = Color.White
          )
        }
      }
    }
    Text(
      color = Color.Gray,
      fontSize = 16.sp,
      text = stringResource(id = R.string.app_version, versionCode, versionName),
      modifier = modifier.padding(16.dp).wrapContentWidth().align(Alignment.End),
    )
  }
}

@Composable
@PreviewWithBackgroundExcludeGenerated
private fun AppSettingScreenPreview() {
  AppSettingScreen(
    appId = "",
    onAppIdChanged = {},
    onLoadConfigurations = {},
    appVersionPair = Pair(1, "0.0.1")
  )
}
