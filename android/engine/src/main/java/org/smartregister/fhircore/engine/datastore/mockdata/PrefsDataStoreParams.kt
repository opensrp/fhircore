package org.smartregister.fhircore.engine.datastore.mockdata

/**
 * provided to the UI as a flow even though the individual parameters are stored...
 * ... and read as separate keys in the datastore
 */
data class PrefsDataStoreParams(
        val appId: String = "",
        val lang: String = ""
)
