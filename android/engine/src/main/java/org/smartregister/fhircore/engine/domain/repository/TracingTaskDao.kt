package org.smartregister.fhircore.engine.domain.repository

import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Task

interface TracingTaskDao {
   suspend fun loadValidTracingTasks(patient: Patient): List<Task>
}