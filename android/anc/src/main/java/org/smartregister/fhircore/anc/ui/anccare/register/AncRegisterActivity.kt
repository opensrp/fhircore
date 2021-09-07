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

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.smartregister.fhircore.anc.R
import org.smartregister.fhircore.anc.form.config.AncFormConfig
import org.smartregister.fhircore.anc.ui.family.register.FamilyRegisterActivity
import org.smartregister.fhircore.engine.configuration.view.registerViewConfigurationOf
import org.smartregister.fhircore.engine.sdk.PatientExtended
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireUtils.buildQuestionnaireIntent
import org.smartregister.fhircore.engine.ui.register.BaseRegisterActivity
import org.smartregister.fhircore.engine.ui.register.model.SideMenuOption
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.FormConfigUtil

class AncRegisterActivity : BaseRegisterActivity() {

  val dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider

  private lateinit var ancFormConfig: AncFormConfig

  private val ancItemMapper = AncItemMapper

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    configureViews(registerViewConfigurationOf().apply {
      appTitle = getString(R.string.app_name)
      showScanQRCode = false
      showNewClientButton = false
    })

    viewsUpdate()
  }

  private fun viewsUpdate(){
    findViewById<View>(R.id.btn_register_new_client).visibility = View.INVISIBLE
  }

  override fun sideMenuOptions(): List<SideMenuOption> = listOf(
    SideMenuOption(
      itemId = R.id.menu_item_family,
      titleResource = R.string.menu_family,
      iconResource = ContextCompat.getDrawable(this, R.drawable.ic_hamburger)!!,
      opensMainRegister = false
    ),
    SideMenuOption(
      itemId = R.id.menu_item_anc,
      titleResource = R.string.menu_anc,
      iconResource = ContextCompat.getDrawable(this, R.drawable.ic_baby_mother)!!,
      opensMainRegister = true,
      searchFilterLambda = { search -> search.filter(PatientExtended.TAG){value = "Pregnant"} }
    )
  )

  override fun onSideMenuOptionSelected(item: MenuItem): Boolean {
    when(item.itemId){
      R.id.menu_item_family -> startActivity(Intent(this, FamilyRegisterActivity::class.java))
      R.id.menu_item_anc -> startActivity(Intent(this, AncRegisterActivity::class.java))
    }
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
