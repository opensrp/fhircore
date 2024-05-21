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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.domain.util.DataLoadState
import org.smartregister.fhircore.engine.ui.login.LOGIN_ERROR_TEXT_TAG
import org.smartregister.fhircore.engine.util.extension.appVersion

@Composable
fun AppSettingScreen(
  modifier: Modifier = Modifier,
  appVersionPair: Pair<Int, String>? = null,
  goToHome: () -> Unit,
  retry: () -> Unit,
  state: DataLoadState<Boolean>,
) {
  val context = LocalContext.current
  val (versionCode, versionName) = remember { appVersionPair ?: context.appVersion() }

  LaunchedEffect(state) {
    if (state is DataLoadState.Success) {
      goToHome()
    }
  }

  Column(
    modifier = modifier.fillMaxSize().padding(16.dp),
  ) {
    Column(
      verticalArrangement = Arrangement.Center,
      modifier = modifier.weight(1f).padding(horizontal = 20.dp),
    ) {
      Text(
        text = stringResource(com.google.android.fhir.R.string.app_name),
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        fontSize = 32.sp,
        modifier = modifier.padding(vertical = 8.dp).align(Alignment.CenterHorizontally),
      )
      Box(modifier = Modifier.fillMaxSize()) {
        when (state) {
          is DataLoadState.Error -> {
            Column(
              Modifier.fillMaxWidth().align(Alignment.Center),
            ) {
              Text(
                fontSize = 14.sp,
                color = MaterialTheme.colors.error,
                text = stringResource(id = getMessageFromException(state.exception)),
                modifier =
                  modifier
                    .wrapContentWidth()
                    .padding(vertical = 10.dp)
                    .align(Alignment.CenterHorizontally)
                    .testTag(LOGIN_ERROR_TEXT_TAG),
              )
              Button(onClick = { retry() }) { Text(text = "Retry") }
            }
          }
          is DataLoadState.Success -> {
            Text(text = "Data loaded successfully")
          }
          else -> {
            Column(
              verticalArrangement = Arrangement.Center,
              horizontalAlignment = Alignment.CenterHorizontally,
              modifier = Modifier.align(Alignment.Center),
            ) {
              CircularProgressIndicator(
                modifier = modifier.padding(bottom = 16.dp),
                strokeWidth = 1.6.dp,
              )
              Text(
                text = "Loading Configurations from server, this might take a while...",
                textAlign = TextAlign.Center,
              )
            }
          }
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

fun getMessageFromException(ex: Exception): Int {
  return when (ex) {
    is InternetConnectionException -> {
      R.string.error_loading_config_no_internet
    }
    is ServerException -> {
      R.string.error_loading_config_general
    }
    is ConfigurationErrorException -> {
      R.string.error_loading_config_http_error
    }
    else -> {
      R.string.error_loading_config_http_error
    }
  }
}
