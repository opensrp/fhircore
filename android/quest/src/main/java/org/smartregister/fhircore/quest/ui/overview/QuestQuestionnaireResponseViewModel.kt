package org.smartregister.fhircore.quest.ui.overview

import android.app.Application
import android.content.Intent
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireViewModel

/**
 * Created by Ephraim Kigamba - nek.eam@gmail.com on 28-10-2021.
 */
class QuestQuestionnaireResponseViewModel(
    application: Application,
    val questionnaireResponse: QuestionnaireResponse
) : QuestionnaireViewModel(
    application
) {

    override suspend fun generateQuestionnaireResponse(
        questionnaire: Questionnaire,
        intent: Intent
    ): QuestionnaireResponse {
        return questionnaireResponse
    }

    override suspend fun loadQuestionnaire(id: String): Questionnaire? {
        val questionnaireId = id.split("/")[1]
        return super.loadQuestionnaire(questionnaireId)?.apply {
            changeQuestionsToReadOnly(this.item)
        }
    }

    private fun changeQuestionsToReadOnly(items: List<Questionnaire.QuestionnaireItemComponent>) {
        items.forEach { item ->
            if (item.type != Questionnaire.QuestionnaireItemType.GROUP) {
                item.readOnly = true
            } else {
                changeQuestionsToReadOnly(item.item)
            }
        }
    }
}