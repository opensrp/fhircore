package org.smartregister.fhircore.engine.configuration.view

import kotlinx.serialization.Serializable
import org.smartregister.fhircore.engine.domain.model.ViewType

@Serializable
data class DemographicsCardProperties (
    override val viewType: ViewType = ViewType.SERVICE_CARD,
    val details: List<ViewGroupProperties> = emptyList(),
): ViewProperties()