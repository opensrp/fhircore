package org.smartregister.fhircore.engine.datastore

import android.content.Context
import androidx.datastore.dataStore
import androidx.datastore.preferences.preferencesDataStore
import javax.inject.Singleton

@Singleton
class StoresProvider {

    val Context.primitivesPreferenceStore by preferencesDataStore(
            name = PRIMITTIVES_DATASTORE
    )

    val Context.practitionerPreferencesStore by dataStore(
            fileName = PATIENT_INFO_DATASTORE_FILE,
            serializer = PractitionerDetailsPreferencesSerializer
    )

    companion object {
        val PRIMITTIVES_DATASTORE = "primitives_datastore"
        val PATIENT_INFO_DATASTORE_FILE = "practitioner_details.json"
    }
}