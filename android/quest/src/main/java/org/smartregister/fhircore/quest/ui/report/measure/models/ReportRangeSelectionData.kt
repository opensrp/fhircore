package org.smartregister.fhircore.quest.ui.report.measure.models

import java.util.Date


data class ReportRangeSelectionData(
    val month: String,
    val year: String,
    val date: Date
)
