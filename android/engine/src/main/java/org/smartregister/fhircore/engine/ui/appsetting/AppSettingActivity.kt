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

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Checkbox
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.auth.AccountAuthenticator
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.ui.theme.AppTheme
import org.smartregister.fhircore.engine.util.annotation.ExcludeFromJacocoGeneratedReport
import org.smartregister.fhircore.engine.util.extension.showToast

@AndroidEntryPoint
class AppSettingActivity : AppCompatActivity() {

  @Inject lateinit var accountAuthenticator: AccountAuthenticator

  @Inject lateinit var configurationRegistry: ConfigurationRegistry

  private val appSettingViewModel: AppSettingViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    appSettingViewModel.loadConfigs.observe(
      this,
      { loadConfigs ->
        if (loadConfigs != null && loadConfigs) {
          configurationRegistry.loadAppConfigurations(
            appId = appSettingViewModel.appId.value!!,
            accountAuthenticator = accountAuthenticator
          ) { finish() }
        } else if (loadConfigs != null && !loadConfigs)
          showToast(getString(R.string.application_not_supported, appSettingViewModel.appId.value))
      }
    )
    setContent { AppTheme { AppScreenLayout(appSettingViewModel) } }
  }
}

@Composable
private fun AppScreenLayout(appSettingViewModel: AppSettingViewModel) {
  val appId by appSettingViewModel.appId.observeAsState("")
  val rememberApp by appSettingViewModel.rememberApp.observeAsState(false)
  AppSettingScreen(
    appId = appId,
    rememberApp = rememberApp,
    onAppIdChanged = appSettingViewModel::onApplicationIdChanged,
    onRememberAppChecked = appSettingViewModel::onRememberAppChecked,
    onLoadConfigurations = appSettingViewModel::loadConfigurations
  )
}

@Composable
private fun AppSettingScreen(
  modifier: Modifier = Modifier,
  appId: String,
  rememberApp: Boolean,
  onAppIdChanged: (String) -> Unit,
  onRememberAppChecked: (Boolean) -> Unit,
  onLoadConfigurations: (Boolean) -> Unit
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
      placeholder = {
        Text(
          color = Color.LightGray,
          text = stringResource(R.string.enter_app_id),
        )
      },
      modifier = modifier.fillMaxWidth().padding(vertical = 2.dp)
    )
    Text(
      text = stringResource(R.string.app_id_sample),
      fontSize = 12.sp,
      modifier = modifier.padding(vertical = 8.dp)
    )
    Row(modifier = modifier.padding(vertical = 20.dp)) {
      Checkbox(checked = rememberApp, onCheckedChange = onRememberAppChecked)
      Text(
        text = stringResource(R.string.remember_app),
        fontSize = 14.sp,
        modifier = modifier.padding(start = 8.dp).clickable { onRememberAppChecked(!rememberApp) }
      )
    }
    Spacer(modifier = modifier.height(20.dp))
    Button(
      onClick = { onLoadConfigurations(true) },
      enabled = appId.isNotEmpty(),
      modifier = modifier.fillMaxWidth()
    ) {
      Text(
        color = Color.White,
        text = stringResource(id = R.string.load_configurations),
        modifier = modifier.padding(8.dp)
      )
    }
  }
}

@Composable
@Preview(showBackground = true)
@ExcludeFromJacocoGeneratedReport
private fun AppSettingScreenPreview() {
  AppSettingScreen(
    appId = "",
    onLoadConfigurations = {},
    onAppIdChanged = {},
    onRememberAppChecked = {},
    rememberApp = false
  )
}
