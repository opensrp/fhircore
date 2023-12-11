package org.smartregister.fhircore.engine.datastore

import android.content.Context
import androidx.datastore.dataStore
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import javax.inject.Singleton

@Singleton
class StoresProvider {

    val Context.dataStore by preferencesDataStore(name = DATASTORE_NAME)
    val primitive1 = intPreferencesKey(PRIMITIVE_1_KEY)
    val primitive2 = stringPreferencesKey(PRIMITIVE_2_KEY)

    private val Context.protoDataStore by dataStore(
            fileName = PROTO_DATASTORE_FILE,
            serializer = PractitionerDetailsPreferencesSerializer
    )

    companion object {
        private val DATASTORE_NAME = "params"
        private val PRIMITIVE_1_KEY = "primitive_1"
        private val PRIMITIVE_2_KEY = "primitive_2"

        val PROTO_DATASTORE_FILE = "proto_datastore.json"
    }


}