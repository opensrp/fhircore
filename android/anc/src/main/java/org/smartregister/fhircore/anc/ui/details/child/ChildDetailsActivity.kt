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

package org.smartregister.fhircore.anc.ui.details.child

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import dagger.hilt.android.AndroidEntryPoint
import org.smartregister.fhircore.anc.R
import org.smartregister.fhircore.engine.ui.base.BaseMultiLanguageActivity
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity.Companion.QUESTIONNAIRE_BACK_REFERENCE_KEY
import org.smartregister.fhircore.engine.util.extension.extractId
import org.smartregister.fhircore.engine.util.extension.showToast

@AndroidEntryPoint
class ChildDetailsActivity : BaseMultiLanguageActivity() {

  lateinit var patientId: String

  private val childDetailsViewModel by viewModels<ChildDetailsViewModel>()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    patientId = intent.extras?.getString(QuestionnaireActivity.QUESTIONNAIRE_ARG_PATIENT_KEY) ?: ""
    val childProfileViewData by childDetailsViewModel.childProfileViewData

    setContent {
      ChildDetailsScreen(
        childProfileViewData = childProfileViewData,
        onTaskRowClick = { id -> onTaskRowClick(id) },
        onBackPress = { onBackPressed() },
      )
    }

    childDetailsViewModel.retrieveChildProfileViewData(patientId)
  }

  fun onTaskRowClick(id: String) {
    childDetailsViewModel.retrieveTask(id).observe(this) {
      it?.let {
        if (true /*it.hasStarted()*/)
          this.startActivityForResult(
            Intent(this, QuestionnaireActivity::class.java).apply {
              QuestionnaireActivity.intentArgs(
                  clientIdentifier = patientId,
                  formName = it.reasonReference.extractId(),
                  backReference = id
                )
                .let { putExtras(it) }
            },
            0
          )
        else this.showToast(getString(R.string.task_not_due_yet))
      }
    }
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)

    if (resultCode == Activity.RESULT_OK)
      data?.getStringExtra(QUESTIONNAIRE_BACK_REFERENCE_KEY)?.let {
        childDetailsViewModel.completeTask(it)
      }
  }

  override fun onResume() {
    super.onResume()
    childDetailsViewModel.retrieveChildProfileViewData(patientId)
  }
}
