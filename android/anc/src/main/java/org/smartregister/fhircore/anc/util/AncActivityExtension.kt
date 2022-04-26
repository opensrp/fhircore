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

package org.smartregister.fhircore.anc.util

import android.app.Activity
import android.content.Intent
import org.smartregister.fhircore.anc.ui.family.form.FamilyFormConstants
import org.smartregister.fhircore.anc.ui.family.form.FamilyQuestionnaireActivity
import org.smartregister.fhircore.anc.ui.family.form.FamilyQuestionnaireActivity.Companion.QUESTIONNAIRE_CALLING_ACTIVITY
import org.smartregister.fhircore.anc.ui.family.form.FamilyQuestionnaireActivity.Companion.QUESTIONNAIRE_RELATED_TO_KEY
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity

fun Activity.startAncEnrollment(patientId: String) {
  startActivity(getFamilyQuestionnaireIntent(patientId, FamilyFormConstants.ANC_ENROLLMENT_FORM))
}

fun Activity.startFamilyMemberRegistration(familyId: String) {
  startActivity(
    getFamilyQuestionnaireIntent(form = FamilyFormConstants.FAMILY_MEMBER_REGISTER_FORM)
      .putExtra(QUESTIONNAIRE_RELATED_TO_KEY, familyId)
  )
}
//todo - from here questionnaireType will be default which will need to
// have a Save as prefix on save button text, need to handle that case on QuestionnaireActivity
fun Activity.getFamilyQuestionnaireIntent(patientId: String? = null, form: String): Intent {
  return Intent(this, FamilyQuestionnaireActivity::class.java)
    .putExtras(QuestionnaireActivity.intentArgs(clientIdentifier = patientId, formName = form))
    .putExtra(QUESTIONNAIRE_CALLING_ACTIVITY, getCallerActivity())
}

fun Activity.getCallerActivity(): String {
  return intent.getStringExtra(QUESTIONNAIRE_CALLING_ACTIVITY) ?: this::class.java.name
}
