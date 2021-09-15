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
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import kotlinx.coroutines.runBlocking
import org.smartregister.fhircore.anc.AncApplication
import org.smartregister.fhircore.anc.R
import org.smartregister.fhircore.anc.data.FamilyRepository
import org.smartregister.fhircore.anc.ui.anccare.register.AncRegisterActivity
import org.smartregister.fhircore.anc.ui.family.form.FamilyFormConstants
import org.smartregister.fhircore.anc.ui.family.form.FamilyQuestionnaireActivity
import org.smartregister.fhircore.engine.configuration.view.registerViewConfigurationOf
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity
import org.smartregister.fhircore.engine.ui.register.BaseRegisterActivity
import org.smartregister.fhircore.engine.ui.register.model.SideMenuOption
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.engine.util.DispatcherProvider

class FamilyRegisterActivity : BaseRegisterActivity() {

  val dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider

  internal lateinit var familyRepository: FamilyRepository

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    configureViews(
      registerViewConfigurationOf(showScanQRCode = false).apply {
        appTitle = getString(R.string.family_register_title)
      }
    )

    familyRepository =
      FamilyRepository((application as AncApplication).fhirEngine, FamilyItemMapper)
  }

  override fun sideMenuOptions(): List<SideMenuOption> =
    listOf(
      SideMenuOption(
        itemId = R.id.menu_item_family,
        titleResource = R.string.family_register_title,
        iconResource = ContextCompat.getDrawable(this, R.drawable.ic_calender)!!,
        opensMainRegister = true,
        countMethod = { runBlocking { familyRepository.countAll() } }
      ),
      SideMenuOption(
        itemId = R.id.menu_item_anc,
        titleResource = R.string.anc_register_title,
        iconResource = ContextCompat.getDrawable(this, R.drawable.ic_baby_mother)!!,
        opensMainRegister = false,
        countMethod = { runBlocking { familyRepository.ancPatientRepository.countAll() } }
      )
    )

  override fun onSideMenuOptionSelected(item: MenuItem): Boolean {
    when (item.itemId) {
      R.id.menu_item_family -> startActivity(Intent(this, FamilyRegisterActivity::class.java))
      R.id.menu_item_anc -> startActivity(Intent(this, AncRegisterActivity::class.java))
    }
    return true
  }

  override fun registerClient() {
    startActivity(
      Intent(this, FamilyQuestionnaireActivity::class.java)
        .putExtras(
          QuestionnaireActivity.requiredIntentArgs(
            clientIdentifier = null,
            form = FamilyFormConstants.FAMILY_REGISTER_FORM
          )
        )
    )
  }

  override fun supportedFragments(): List<Fragment> = listOf(FamilyRegisterFragment())
}
