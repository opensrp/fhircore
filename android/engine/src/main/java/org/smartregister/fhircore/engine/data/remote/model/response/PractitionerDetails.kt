package org.smartregister.fhircore.engine.data.remote.model.response

import kotlinx.serialization.Serializable

@Serializable
data class PractitionerDetails(
        val id: Int = 1,
        val name: String = "some name"
)
