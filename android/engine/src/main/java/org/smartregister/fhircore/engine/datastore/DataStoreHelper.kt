package org.smartregister.fhircore.engine.datastore

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataStoreHelper @Inject constructor(private val storesProvider: StoresProvider){
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