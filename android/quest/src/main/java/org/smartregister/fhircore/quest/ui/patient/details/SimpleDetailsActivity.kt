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

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import dagger.hilt.android.AndroidEntryPoint
import org.smartregister.fhircore.engine.ui.base.BaseMultiLanguageActivity
import org.smartregister.fhircore.engine.ui.theme.AppTheme

@AndroidEntryPoint
class SimpleDetailsActivity : BaseMultiLanguageActivity() {

  private lateinit var encounterId: String

  val viewModel by viewModels<SimpleDetailsViewModel>()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    encounterId = intent.extras?.getString(RECORD_ID_ARG)!!

    viewModel.onBackPressClicked.observe(this, { if (it) finish() })

    viewModel.loadData(encounterId)

    setContent { AppTheme { SimpleDetailsScreen(viewModel) } }
  }

  companion object {
    const val RECORD_ID_ARG = "RECORD_ID"
  }
}
