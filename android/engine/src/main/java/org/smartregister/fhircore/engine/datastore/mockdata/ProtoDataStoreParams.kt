package org.smartregister.fhircore.engine.datastore.mockdata

import kotlinx.serialization.Serializable

/**
 * just needed around to discuss the idea of combining al objects into one file
 */
@Serializable
data class ProtoDataStoreParams (
        val practitionerDetails: SerializablePractitionerDetails = SerializablePractitionerDetails(),
        val userInfo: SerializableUserInfo = SerializableUserInfo()
)