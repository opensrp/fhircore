package org.smartregister.fhircore.engine.datastore

import org.smartregister.fhircore.engine.datastore.prefsdatastore.PrefsDataStoreProvider
import javax.inject.Inject
import javax.inject.Singleton

/**
 *
 * This class is what the app will use to communicate with both preferences data store and proto datastore
 * If a key is provided, we write to preferences data store. Otherwise, we use the Object's type to determine which proto data store to store in
 *
 */
@Singleton
class DataStoreHelper @Inject constructor(
        private val prefsDataStoreProvider: PrefsDataStoreProvider,
){

    fun <String, Boole> read() {
    prefsDataStoreProvider.
    }

    fun read() {

    }

    // For writing Primitives
    fun <T> write(key: String, value: T) {
        when(value) {
            is Int ->
                storesProvider.write
        }
    }

    fun
}