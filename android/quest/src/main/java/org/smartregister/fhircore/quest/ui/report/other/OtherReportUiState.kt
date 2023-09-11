package org.smartregister.fhircore.quest.ui.report.other

import org.smartregister.fhircore.engine.configuration.report.other.OtherReportConfiguration
import org.smartregister.fhircore.engine.domain.model.ResourceData

data class OtherReportUiState(
    val reportId: String = "",
    val resourceData: ResourceData? = null,
    val otherReportConfiguration: OtherReportConfiguration? = null,
    val showDataLoadProgressIndicator: Boolean = true,
)
