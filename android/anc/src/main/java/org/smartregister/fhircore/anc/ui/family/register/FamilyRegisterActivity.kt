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

package org.smartregister.fhircore.anc.ui.family.register

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.smartregister.fhircore.anc.AncApplication
import org.smartregister.fhircore.anc.R
import org.smartregister.fhircore.anc.data.FamilyRepository
import org.smartregister.fhircore.anc.ui.anccare.register.AncItemMapper
import org.smartregister.fhircore.anc.ui.anccare.register.AncRegisterActivity
import org.smartregister.fhircore.anc.ui.anccare.register.AncRegisterFragment
import org.smartregister.fhircore.anc.ui.family.form.FamilyFormConfig
import org.smartregister.fhircore.anc.ui.family.form.RegisterFamilyMemberInput
import org.smartregister.fhircore.anc.ui.family.form.RegisterFamilyMemberOutput
import org.smartregister.fhircore.anc.ui.family.form.RegisterFamilyMemberResult
import org.smartregister.fhircore.engine.configuration.view.registerViewConfigurationOf
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireUtils.getUniqueId
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireViewModel
import org.smartregister.fhircore.engine.ui.register.BaseRegisterActivity
import org.smartregister.fhircore.engine.ui.register.model.SideMenuOption
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.FormConfigUtil

class FamilyRegisterActivity : BaseRegisterActivity() {

  val dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider

  internal lateinit var familyFormConfig: FamilyFormConfig
  internal val viewModel by viewModels<QuestionnaireViewModel>()

  internal lateinit var familyMemberRegistration: ActivityResultLauncher<RegisterFamilyMemberInput>
  internal lateinit var familyRepository: FamilyRepository

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    configureViews(
      registerViewConfigurationOf(showScanQRCode = false).apply {
        appTitle = getString(R.string.family_register_title)
      }
    )

    familyFormConfig =
      FormConfigUtil.loadConfig(
        FamilyFormConfig.FAMILY_REGISTER_CONFIG_ID,
        this@FamilyRegisterActivity
      )

    familyMemberRegistration =
      registerForActivityResult(RegisterFamilyMemberResult()) {
        it?.run { handleRegisterFamilyMemberResult(it) }
      }

    familyRepository =
      FamilyRepository(
        (application as AncApplication).fhirEngine,
        AncItemMapper
      )
  }

  override fun sideMenuOptions(): List<SideMenuOption> =
    listOf(
      SideMenuOption(
        itemId = R.id.menu_item_family,
        titleResource = R.string.family_register_title,
        iconResource = ContextCompat.getDrawable(this, R.drawable.ic_calender)!!,
        opensMainRegister = false
      ),
      SideMenuOption(
        itemId = R.id.menu_item_anc,
        titleResource = R.string.anc_register_title,
        iconResource = ContextCompat.getDrawable(this, R.drawable.ic_baby_mother)!!,
        opensMainRegister = false
      )
    )

  override fun onSideMenuOptionSelected(item: MenuItem): Boolean {
    when (item.itemId) {
      R.id.menu_item_family -> startActivity(Intent(this, FamilyRegisterActivity::class.java))
      R.id.menu_item_anc -> startActivity(Intent(this, AncRegisterActivity::class.java))
    }
    return true
  }

  // TODO change to family registration with https://github.com/opensrp/fhircore/issues/276
  override fun registerClient() {
    familyMemberRegistration.launch(RegisterFamilyMemberInput(getUniqueId(), familyFormConfig))
  }

  // TODO add family fragment with https://github.com/opensrp/fhircore/issues/276
  override fun supportedFragments(): List<Fragment> = listOf(AncRegisterFragment())

  internal fun handleRegisterFamilyMemberResult(output: RegisterFamilyMemberOutput) {
    lifecycleScope.launch {
      val questionnaire = viewModel.loadQuestionnaire(familyFormConfig.memberRegistrationQuestionnaireId)
      familyRepository.postProcessFamilyMember(questionnaire, output.questionnaireResponse)
    }
  }
}
