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

package org.smartregister.fhircore.eir.ui.patient.register

import android.os.Bundle
import android.view.MenuItem
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.smartregister.fhircore.eir.R
import org.smartregister.fhircore.eir.form.config.QuestionnaireFormConfig
import org.smartregister.fhircore.eir.ui.questionnaire.QuestionnaireUtils.buildQuestionnaireIntent
import org.smartregister.fhircore.engine.configuration.view.registerViewConfigurationOf
import org.smartregister.fhircore.engine.data.local.repository.patient.PatientPaginatedDataSource
import org.smartregister.fhircore.engine.ui.register.BaseRegisterActivity
import org.smartregister.fhircore.engine.ui.register.model.SideMenuOption
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.FormConfigUtil

class PatientRegisterActivity : BaseRegisterActivity() {

  val dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider

  private lateinit var questionnaireFormConfig: QuestionnaireFormConfig

  private val patientItemMapper = PatientItemMapper

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    configureViews(registerViewConfigurationOf().apply { appTitle = getString(R.string.covax_app) })
  }

  override fun sideMenuOptions(): List<SideMenuOption> =
    listOf(
      SideMenuOption(
        itemId = COVAX_MENU_OPTION,
        titleResource = R.string.client_list_title_covax,
        iconResource = ContextCompat.getDrawable(this, R.drawable.ic_baby_mother)!!,
        opensMainRegister = false
      )
    )

  override fun onSideMenuOptionSelected(item: MenuItem): Boolean {
    return true
  }

  override fun registerClient() {
    lifecycleScope.launch {
      questionnaireFormConfig =
        withContext(dispatcherProvider.io()) {
          FormConfigUtil.loadConfig(
            QuestionnaireFormConfig.COVAX_DETAIL_VIEW_CONFIG_ID,
            this@PatientRegisterActivity
          )
        }

      with(questionnaireFormConfig) {
        val questionnaireId = registrationQuestionnaireIdentifier
        val questionnaireTitle = registrationQuestionnaireTitle

        startActivity(
          buildQuestionnaireIntent(
            context = this@PatientRegisterActivity,
            questionnaireTitle = questionnaireTitle,
            questionnaireId = questionnaireId,
            patientId = null,
            isNewPatient = true
          )
        )
      }
    }
  }

  override fun supportedFragments(): List<Fragment> {
    val patientRegisterFragment =
      PatientRegisterFragment().apply {
        paginatedDataSource = PatientPaginatedDataSource(fhirEngine, patientItemMapper)
      }
    return listOf(patientRegisterFragment)
  }

  companion object {
    const val COVAX_MENU_OPTION = 1000
  }
}
