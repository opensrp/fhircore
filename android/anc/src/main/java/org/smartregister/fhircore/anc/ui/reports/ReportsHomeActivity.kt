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

package org.smartregister.fhircore.anc.ui.reports

import android.os.Bundle
import androidx.activity.compose.setContent
import org.smartregister.fhircore.anc.AncApplication
import org.smartregister.fhircore.anc.data.report.ReportRepository
import org.smartregister.fhircore.engine.ui.base.BaseMultiLanguageActivity
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity
import org.smartregister.fhircore.engine.ui.theme.AppTheme

class ReportsHomeActivity : BaseMultiLanguageActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val patientId =
      intent.extras?.getString(QuestionnaireActivity.QUESTIONNAIRE_ARG_PATIENT_KEY) ?: ""
    val repository = ReportRepository((application as AncApplication).fhirEngine, patientId)
    val viewModel = ReportsViewModel.get(this, application as AncApplication, repository)
    viewModel.setAppBackClickListener(this::finish)
    setContent { AppTheme { ReportsHomeScreen(viewModel) } }
  }
}
