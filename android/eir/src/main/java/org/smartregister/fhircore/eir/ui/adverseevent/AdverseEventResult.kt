package org.smartregister.fhircore.eir.ui.adverseevent

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.smartregister.fhircore.eir.form.config.QuestionnaireFormConfig
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity.Companion.QUESTIONNAIRE_ARG_RESPONSE_KEY
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireUtils

class AdverseEventResult(private val patientId: String) : ActivityResultContract<QuestionnaireFormConfig, QuestionnaireResponse?>() {
  override fun createIntent(context: Context, input: QuestionnaireFormConfig): Intent {
    return QuestionnaireUtils.buildQuestionnaireIntent(
      context,
      input.adverseEventsQuestionnaireTitle,
      input.adverseEventsQuestionnaireIdentifier,
      patientId,
      false
    )
  }

  override fun parseResult(resultCode: Int, intent: Intent?): QuestionnaireResponse? {
    val data = intent?.getStringExtra(QUESTIONNAIRE_ARG_RESPONSE_KEY)

    return if (resultCode == Activity.RESULT_OK && data != null) {
      QuestionnaireUtils.asQuestionnaireResponse(data)
    } else null
  }

}