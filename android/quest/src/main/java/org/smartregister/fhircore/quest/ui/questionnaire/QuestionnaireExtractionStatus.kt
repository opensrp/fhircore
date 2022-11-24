package org.smartregister.fhircore.quest.ui.questionnaire

sealed class QuestionnaireExtractionStatus {
    object Success : QuestionnaireExtractionStatus()
    data class Error(val exception: Exception) : QuestionnaireExtractionStatus()
}


