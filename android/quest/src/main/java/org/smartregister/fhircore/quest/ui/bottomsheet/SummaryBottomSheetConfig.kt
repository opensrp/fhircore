package org.smartregister.fhircore.quest.ui.bottomsheet

import org.smartregister.fhircore.engine.configuration.view.ViewProperties
import org.smartregister.fhircore.engine.domain.model.ResourceData
import org.smartregister.fhircore.engine.domain.model.RuleConfig


data class SummaryBottomSheetConfig(
    val rules: List<RuleConfig>,
    val bottomSheetView: List<ViewProperties>,
    val resourceData: ResourceData,
    val servicePointID: String? = "123"
)