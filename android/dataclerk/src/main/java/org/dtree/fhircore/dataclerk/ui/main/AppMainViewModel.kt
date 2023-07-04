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

package org.dtree.fhircore.dataclerk.ui.main

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.app.AppConfigClassification
import org.smartregister.fhircore.engine.configuration.view.RegisterViewConfiguration
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireType

@HiltViewModel
class AppMainViewModel
@Inject
constructor(
  val configurationRegistry: ConfigurationRegistry,
) : ViewModel() {

  val patientRegisterConfiguration: RegisterViewConfiguration by lazy {
    configurationRegistry.retrieveConfiguration(AppConfigClassification.PATIENT_REGISTER)
  }

  fun openForm(context: Context) =
    Intent(context, QuestionnaireActivity::class.java)
      .putExtras(
        QuestionnaireActivity.intentArgs(
          formName = patientRegisterConfiguration.registrationForm,
          questionnaireType = QuestionnaireType.DEFAULT
        )
      )
}
