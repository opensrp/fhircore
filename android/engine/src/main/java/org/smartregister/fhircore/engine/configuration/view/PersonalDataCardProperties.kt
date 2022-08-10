package org.smartregister.fhircore.engine.configuration.view

import kotlinx.serialization.Serializable
import org.smartregister.fhircore.engine.domain.model.ViewType

@Serializable
data class PersonalDataCardProperties (
    override val viewType: ViewType = ViewType.PERSONAL_DATA_CARD,
    val personalDataItem: List<CompoundTextProperties> = emptyList()
        ): ViewProperties()