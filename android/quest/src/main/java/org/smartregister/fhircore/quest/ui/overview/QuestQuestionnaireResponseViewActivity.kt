package org.smartregister.fhircore.quest.ui.overview

import android.app.Application
import android.view.View
import ca.uhn.fhir.context.FhirContext
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireViewModel
import org.smartregister.fhircore.engine.util.extension.showToast

/**
 * Created by Ephraim Kigamba - nek.eam@gmail.com on 28-10-2021.
 */
class QuestQuestionnaireResponseViewActivity : QuestionnaireActivity() {

    override fun onClick(view: View) {
        if (view.id == R.id.btn_save_client_info) {
            finish()
        } else {
            showToast(getString(R.string.error_saving_form))
        }
    }

    override fun createViewModel(application: Application): QuestionnaireViewModel {
        val questionnaireResponse = intent.getStringExtra("questionnaire-response")
        return QuestQuestionnaireResponseViewModel(application, questionnaireResponse = FhirContext.forR4().newJsonParser().parseResource(QuestionnaireResponse::class.java, questionnaireResponse))
    }
}