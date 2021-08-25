package org.smartregister.fhircore.model

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.smartregister.fhircore.activity.core.QuestionnaireActivity
import org.smartregister.fhircore.util.QuestionnaireUtils

class AdverseEventResult(private val patientId: String) : ActivityResultContract<CovaxDetailView, QuestionnaireResponse?>() {
  override fun createIntent(context: Context, input: CovaxDetailView): Intent {
    return QuestionnaireUtils.buildQuestionnaireIntent(
      context,
      input.adverseEventsQuestionnaireTitle,
      input.adverseEventsQuestionnaireIdentifier,
      patientId,
      false
    )
  }

  override fun parseResult(resultCode: Int, intent: Intent?): QuestionnaireResponse? {
    val data = intent?.getStringExtra(QuestionnaireActivity.QUESTIONNAIRE_ARG_RESPONSE_KEY)

    return if (resultCode == Activity.RESULT_OK && data != null) {
      QuestionnaireUtils.asQuestionnaireResponse(data)
    } else null
  }

}