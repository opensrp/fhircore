package org.smartregister.fhircore.engine.datastore

import kotlinx.serialization.Serializable

@Serializable
data class SerializableUserInfo (
        val name: String = "sample name"
)