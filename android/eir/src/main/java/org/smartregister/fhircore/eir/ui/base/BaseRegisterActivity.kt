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

package org.smartregister.fhircore.eir.ui.base

import android.os.Bundle
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import org.smartregister.fhircore.eir.R
import org.smartregister.fhircore.eir.ui.base.model.BaseRegister
import org.smartregister.fhircore.eir.util.Utils
import org.smartregister.fhircore.eir.util.extension.view.addOnDrawableClickedListener

abstract class BaseRegisterActivity : BaseDrawerActivity() {
  val register by lazy { buildRegister() }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    setUpViews()
  }

  abstract fun buildRegister(): BaseRegister

  protected fun setUpViews() {
    setupRegistrationView()

    setupPager()

    setupSearchBox()
  }

  protected fun setupRegistrationView() {
    register.newRegistrationView()?.setOnClickListener { startRegistrationActivity(null) }
  }

  protected fun setupPager() {
    register.viewPager().adapter = PagerAdapter(this)
  }

  protected fun setupSearchBox() {
    var editText = register.searchBox() ?: return

    editText.doAfterTextChanged {
      if (it!!.isEmpty()) {
        editText.setOnTouchListener(null)
        editText.setCompoundDrawablesWithIntrinsicBounds(
            getDrawable(R.drawable.ic_search), null, null, null)
      } else {
        editText.setCompoundDrawablesWithIntrinsicBounds(
            null, null, getDrawable(R.drawable.ic_cancel), null)
        editText.addOnDrawableClickedListener(Utils.DrawablePosition.DRAWABLE_RIGHT) { it.clear() }
      }
    }
  }

  fun startRegistrationActivity(preAssignedId: String?) {
    val questionnaireId = register.newRegistrationQuestionnaireIdentifier!!
    val questionnaireTitle =
        register.newRegistrationQuestionnaireTitle ?: getString(R.string.client_info)

    startQuestionnaire(questionnaireTitle, questionnaireId, preAssignedId, true)
  }

  private inner class PagerAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {
    override fun getItemCount(): Int {
      return 1
    }

    override fun createFragment(position: Int): Fragment {
      return register.listFragment
    }
  }
}
