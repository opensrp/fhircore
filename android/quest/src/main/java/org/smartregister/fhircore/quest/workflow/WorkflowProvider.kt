package org.smartregister.fhircore.quest.workflow

import java.util.*
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.DateTimeType
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.Reference
import org.hl7.fhir.r4.model.StringType

/**
 * Created by Ephraim Kigamba - nek.eam@gmail.com on 28-01-2022.
 */
class WorkflowProvider {

    fun computeClientState(patient: Patient, stateObservation: Observation, visits: List<QuestionnaireResponse>) : List<Observation> {
        return if (visits.missedLast2Visits()) {
            listOf(createStatusObs(patient, "discharged"), createStatusTagObs(patient, "defaulter"))
        } else if (visits.missedLastVisit()) {
            val statusTagObs = createStatusTagObs(patient, "visit-overdue")
            if (!stateObservation.isStatusActive()) {
                listOf(statusTagObs)
            } else {
                val activeStatusObs = createStatusObs(patient, "active")
                listOf(activeStatusObs, statusTagObs)
            }
        } else {
            listOf()
        }
    }

    fun nextStep(stateObservation: Observation, visits: List<QuestionnaireResponse>) : Step {
        return if (stateObservation.isPatientStatus("discharged")) {
            Step.DIMSISS
        } else if (!visits.isNextVisitDue()) {
            Step.DIMSISS
        } else if (visits.isEmpty()) {
            Step.ASSISTANCE_STEP_FIRST_VISIT
        } else if (visits.size == 1) {
            Step.ANTHROPOMETRIC_STEP_FIRST_VISIT
        } else if (visits.size%2 == 1) {
            Step.ASSISTANCE_STEP_FOLLOWING_VISIT
        } else {
            Step.ANTHROPOMETRIC_STEP_FOLLOWING_VISIT
        }
    }

    fun List<QuestionnaireResponse>.missedLastVisit() : Boolean {
        return size > 0 && this[0].authored.daysFromToday() > 14
    }

    fun Date.daysFromToday() : Int =
        ((Calendar.getInstance().time.time - this.time)/(1000 * 60 * 60 * 24)).toInt()

    fun List<QuestionnaireResponse>.missedLast2Visits() : Boolean {
        return size > 1 && this[0].authored.daysFromToday() > 28
    }

    fun List<QuestionnaireResponse>.isNextVisitDue() : Boolean {
        return size > 0 && this[0].authored.daysFromToday() == 14
    }

    fun List<QuestionnaireResponse>.isLastAnthroVisit() : Boolean {
        return size > 0 && this[0].questionnaire.endsWith("anthro-following-visit")
    }

    fun List<QuestionnaireResponse>.isLastAssistanceVisit() : Boolean {
        return size > 0 && this[0].questionnaire.endsWith("assistance-visit")
    }

    fun Observation.isStatusActive() : Boolean {
        return valueStringType.valueAsString.equals("active")
    }

    fun createStatusObs(patient: Patient, status : String) : Observation {
        val date = Calendar.getInstance().time
        return Observation()
            .apply {
                value = StringType(status)
                code = CodeableConcept(Coding("https://smartregister.org/wfp-coda", "patient-status", "Patient status"))
                setEffective(DateTimeType(date))
                subject = Reference(patient)
                issued = date
                id = UUID.randomUUID().toString()
                setStatus(Observation.ObservationStatus.FINAL)
            }
    }

    fun createStatusTagObs(patient: Patient, status : String) : Observation {
        val date = Calendar.getInstance().time
        return Observation()
            .apply {
                value = StringType(status)
                code = CodeableConcept(Coding("https://smartregister.org/wfp-coda", "patient-status-tag", "Patient status tag"))
                setEffective(DateTimeType(date))
                subject = Reference(patient)
                issued = date
                id = UUID.randomUUID().toString()
                setStatus(Observation.ObservationStatus.FINAL)
            }
    }

    fun Observation.isPatientStatus(status: String) : Boolean {
        return code.coding[0].code == "patient-status" && valueStringType.valueAsString == status
    }


    fun Observation.isPatientStatusTag(status: String) : Boolean {
        return code.coding[0].code == "patient-status-tag" && valueStringType.valueAsString == status
    }

    enum class Step {
        ANTHROPOMETRIC_STEP_FIRST_VISIT,
        ANTHROPOMETRIC_STEP_FOLLOWING_VISIT,
        ASSISTANCE_STEP_FIRST_VISIT,
        ASSISTANCE_STEP_FOLLOWING_VISIT,
        DIMSISS
    }
}