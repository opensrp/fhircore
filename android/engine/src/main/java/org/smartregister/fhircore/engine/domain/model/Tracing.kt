package org.smartregister.fhircore.engine.domain.model

import java.util.Date

data class TracingHistory(
        val startDate: Date,
        val endDate: Date,
        val status: Boolean,
        val outcomes: List<TracingOutcome>,
        val numberOfAttempts: Int
)

data class TracingOutcome(
        val title: String,
        val date: Date?,
        val reasons: List<String>,
        val conducted: Boolean,
        val outcome: String,
        val dateOfAppointment: Date?,
)

data class CurrentTracingAttempt(
        val lastAttempt: Date?,
        val numberOfAttempts: Int,
        val outcome: String,
        val reasons: List<String>,
)