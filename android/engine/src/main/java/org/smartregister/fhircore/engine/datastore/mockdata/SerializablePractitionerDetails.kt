package org.smartregister.fhircore.engine.datastore.mockdata

import kotlinx.serialization.Serializable
import org.smartregister.model.practitioner.PractitionerDetails

@Serializable
data class SerializablePractitionerDetails(
    val name: String = "sample_name",
    val id: Int = 1
)
