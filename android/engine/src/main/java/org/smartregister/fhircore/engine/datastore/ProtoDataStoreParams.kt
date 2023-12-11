package org.smartregister.fhircore.engine.datastore

import kotlinx.serialization.Serializable

@Serializable
data class ProtoDataStoreParams (
        val practitionerDetails: SerializablePractitionerDetails = SerializablePractitionerDetails(),
        val userInfo: SerializableUserInfo = SerializableUserInfo()
)