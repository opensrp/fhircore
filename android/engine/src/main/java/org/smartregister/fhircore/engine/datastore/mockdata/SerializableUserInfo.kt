package org.smartregister.fhircore.engine.datastore.mockdata

import kotlinx.serialization.Serializable

@Serializable
data class SerializableUserInfo (
        val name: String = "sample name"
)