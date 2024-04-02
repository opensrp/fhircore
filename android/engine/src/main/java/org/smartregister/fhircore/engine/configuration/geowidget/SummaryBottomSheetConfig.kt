package org.smartregister.fhircore.engine.configuration.geowidget

import kotlinx.serialization.Serializable
import org.smartregister.fhircore.engine.configuration.view.ViewProperties
import org.smartregister.fhircore.engine.domain.model.RuleConfig

@Serializable
data class SummaryBottomSheetConfig(val rules: List<RuleConfig>, val views : List<ViewProperties>, val servicePointID : String)