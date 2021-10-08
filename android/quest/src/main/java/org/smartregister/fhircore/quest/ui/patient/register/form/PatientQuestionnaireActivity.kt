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

package org.smartregister.fhircore.quest.ui.patient.register.form

import android.os.Bundle
import android.widget.Button
import androidx.lifecycle.lifecycleScope
import com.google.android.fhir.datacapture.mapping.ResourceMapper
import java.util.UUID
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity
import org.smartregister.fhircore.engine.util.extension.hide
import org.smartregister.fhircore.quest.QuestApplication

class PatientQuestionnaireActivity : QuestionnaireActivity() {
  internal lateinit var repository: DefaultRepository
  private lateinit var saveBtn: Button

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    saveBtn = findViewById(org.smartregister.fhircore.engine.R.id.btn_save_client_info)

    repository = DefaultRepository(QuestApplication.getContext().fhirEngine)
  }

  override fun handleQuestionnaireResponse(questionnaireResponse: QuestionnaireResponse) {
    lifecycleScope.launch {
      saveBtn.hide(false)

      val patient =
        ResourceMapper.extract(questionnaire!!, questionnaireResponse).entry[0].resource as Patient
      patient.id = UUID.randomUUID().toString()

      repository.save(patient)
      finish()
    }
  }
}
