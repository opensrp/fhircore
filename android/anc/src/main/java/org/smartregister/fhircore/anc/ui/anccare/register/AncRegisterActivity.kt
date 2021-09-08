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

package org.smartregister.fhircore.anc.ui.anccare.register

import android.os.Bundle
import android.view.MenuItem
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.smartregister.fhircore.anc.R
import org.smartregister.fhircore.anc.form.config.AncFormConfig
import org.smartregister.fhircore.engine.configuration.view.registerViewConfigurationOf
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireUtils.buildQuestionnaireIntent
import org.smartregister.fhircore.engine.ui.register.BaseRegisterActivity
import org.smartregister.fhircore.engine.ui.register.model.SideMenuOption
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.FormConfigUtil

class AncRegisterActivity : BaseRegisterActivity() {

  val dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider

  private lateinit var ancFormConfig: AncFormConfig

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    configureViews(registerViewConfigurationOf().apply { appTitle = getString(R.string.app_name) })
  }

  override fun sideMenuOptions(): List<SideMenuOption> =
    listOf(
      SideMenuOption(
        itemId = R.id.menu_item_anc,
        titleResource = R.string.app_name,
        iconResource = ContextCompat.getDrawable(this, R.drawable.ic_baby_mother)!!,
        opensMainRegister = false
      )
    )

  override fun onSideMenuOptionSelected(item: MenuItem): Boolean {
    return true
  }

  override fun registerClient() {
    lifecycleScope.launch {
      ancFormConfig =
        withContext(dispatcherProvider.io()) {
          FormConfigUtil.loadConfig(
            AncFormConfig.ANC_DETAIL_VIEW_CONFIG_ID,
            this@AncRegisterActivity
          )
        }

      with(ancFormConfig) {
        val questionnaireId = registrationQuestionnaireIdentifier
        val questionnaireTitle = registrationQuestionnaireTitle

        startActivity(
          buildQuestionnaireIntent(
            context = this@AncRegisterActivity,
            questionnaireTitle = questionnaireTitle,
            questionnaireId = questionnaireId,
            patientId = null,
            isNewPatient = true
          )
        )
      }
    }
  }

  override fun supportedFragments(): List<Fragment> = listOf(AncRegisterFragment())
}
