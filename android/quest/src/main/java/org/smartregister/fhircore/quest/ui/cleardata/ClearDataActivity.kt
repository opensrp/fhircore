/*
 * Copyright 2021-2024 Ona Systems, Inc
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

package org.smartregister.fhircore.quest.ui.cleardata

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.smartregister.fhircore.engine.ui.base.BaseMultiLanguageActivity

@AndroidEntryPoint
class ClearDataActivity : BaseMultiLanguageActivity() {

  private val viewModel by viewModels<ClearDataViewModel>()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    lifecycleScope.launch {
      val unsyncedResources = viewModel.getUnsyncedResourceCount()
      val appName = viewModel.getAppName()

      setContent {
        ClearDataScreen(
          viewModel = viewModel,
          unsyncedResourceCount = unsyncedResources,
          appName = appName.toString(),
          onDeleteData = {
            viewModel.clearAppData()
            finish()
          },
        )
      }
    }
  }
}
