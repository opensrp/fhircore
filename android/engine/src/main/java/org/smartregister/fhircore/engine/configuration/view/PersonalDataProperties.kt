package org.smartregister.fhircore.engine.configuration.view

import kotlinx.serialization.Serializable
import org.smartregister.fhircore.engine.domain.model.ViewType

@Serializable
data class PersonalDataProperties (
    override val viewType: ViewType = ViewType.PERSONAL_DATA,
    val personalDataItems: List<PersonalDataItem> = emptyList()
        ): ViewProperties()

@Serializable
data class PersonalDataItem (
    val label:CompoundTextProperties,
    val displayValue:CompoundTextProperties
)