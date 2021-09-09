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

package org.smartregister.fhircore.anc.ui.anccare.details

import android.graphics.Color
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.Menu
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import org.smartregister.fhircore.anc.R
import org.smartregister.fhircore.anc.databinding.ActivityAncDetailsBinding
import org.smartregister.fhircore.engine.ui.base.BaseMultiLanguageActivity
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity

class AncDetailsActivity : BaseMultiLanguageActivity() {

  private lateinit var patientId: String

  private lateinit var activityAncDetailsBinding: ActivityAncDetailsBinding

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    activityAncDetailsBinding = DataBindingUtil.setContentView(this, R.layout.activity_anc_details)
    setSupportActionBar(activityAncDetailsBinding.patientDetailsToolbar)

    if (savedInstanceState == null) {

      patientId =
        intent.extras?.getString(QuestionnaireActivity.QUESTIONNAIRE_ARG_PATIENT_KEY) ?: ""

      supportFragmentManager
        .beginTransaction()
        .replace(
          R.id.container,
          AncDetailsFragment.newInstance(
            bundleOf(Pair(QuestionnaireActivity.QUESTIONNAIRE_ARG_PATIENT_KEY, patientId))
          )
        )
        .commitNow()
    }

    activityAncDetailsBinding.patientDetailsToolbar.setNavigationOnClickListener { onBackPressed() }
  }

  override fun onCreateOptionsMenu(menu: Menu?): Boolean {
    menuInflater.inflate(R.menu.profile_menu, menu)
    return super.onCreateOptionsMenu(menu)
  }

  override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
    val mColorFullMenuBtn = menu!!.findItem(R.id.remove_this_person) // extract the menu item here
    val title = mColorFullMenuBtn.title.toString()
    val s = SpannableString(title)
    with(s) {
      setSpan(
        ForegroundColorSpan(Color.parseColor("#DD0000")),
        0,
        length,
        Spannable.SPAN_INCLUSIVE_INCLUSIVE
      )
    } // provide whatever color you want here.
    mColorFullMenuBtn.title = s
    return super.onPrepareOptionsMenu(menu)
  }
}
