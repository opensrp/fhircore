package org.smartregister.fhircore.quest.workflow

import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.QuestionnaireResponse

/**
 * Created by Ephraim Kigamba - nek.eam@gmail.com on 28-01-2022.
 */
class WorkflowProvider {

    fun computeClientState(patient: Patient, stateObservation: Observation, visits: List<QuestionnaireResponse>) : List<Observation> {
        if (visits.missedLast2Visits()) {
            return listOf(createStatusObs("discharged"), createStatusTagObs("defaulter"))
        } else if (visits.missedLastVisit()) {
            val statusTagObs = createStatusTagObs("visit-overdue")
            if (!stateObservation.isPatientActive()) {
                return listOf(statusTagObs)
            } else {
                val activeStatusObs = createStatusObs("active")
                return listOf(activeStatusObs, statusTagObs)
            }
        } else {
            return listOf()
        }
    }

    fun nextStep(patient: Patient, stateObservation: Observation, visits: List<QuestionnaireResponse>) : Step {
        if (stateObservation.isPatientStatus("discharged")) {
            return Step.DIMSISS
        } else if (!visits.isNextVisitDue()) {
            return Step.DIMSISS
        } else if (visits.isEmpty()) {
            return Step.ASSISTANCE_STEP_FIRST_VISIT
        } else if (visits.size == 1) {
            return Step.ANTHROPOMETRIC_STEP_FIRST_VISIT
        } else if (visits.size%2 == 1) {
            return Step.ASSISTANCE_STEP_FOLLOWING_VISIT
        } else {
            return Step.ANTHROPOMETRIC_STEP_FOLLOWING_VISIT
        }
    }

    fun List<QuestionnaireResponse>.missedLastVisit() : Boolean {
        return true
    }

    fun List<QuestionnaireResponse>.missedLast2Visits() : Boolean {
        return true
    }

    fun List<QuestionnaireResponse>.isNextVisitDue() : Boolean {
        return true
    }

    fun List<QuestionnaireResponse>.isLastAnthroVisit() : Boolean {
        return true
    }

    fun List<QuestionnaireResponse>.isLastAssistanceVisit() : Boolean {
        return true
    }

    fun Observation.isPatientActive() : Boolean {
        return true
    }

    fun createStatusObs(status : String) : Observation {

    }

    fun createStatusTagObs(status : String) : Observation {

    }

    fun Observation.isPatientStatus(status: String) : Boolean {
        return true
    }


    fun Observation.isPatientStatusTag(status: String) : Boolean {
        return true
    }

    enum class Step {
        ANTHROPOMETRIC_STEP_FIRST_VISIT,
        ANTHROPOMETRIC_STEP_FOLLOWING_VISIT,
        ASSISTANCE_STEP_FIRST_VISIT,
        ASSISTANCE_STEP_FOLLOWING_VISIT,
        DIMSISS
    }
}