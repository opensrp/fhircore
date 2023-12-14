package org.smartregister.fhircore.engine.datastore

import androidx.datastore.preferences.core.Preferences
import org.smartregister.fhircore.engine.datastore.mockdata.SerializablePractitionerDetails
import org.smartregister.fhircore.engine.datastore.mockdata.SerializableUserInfo
import javax.inject.Inject
import javax.inject.Singleton

/**
 *
 * This class is what the app will use to communicate with both preferences data store and proto datastore
 * If a key is provided, we write to preferences data store. Otherwise, we use the Object's type to determine which proto data store to store in
 *
 */
@Singleton
class DataStoreHelper @Inject constructor(private val dataStoresRepository: DataStoresRepository){

    val preferences = dataStoresRepository.preferences
    suspend fun <T> writePreference ( key: Preferences.Key<T>, value: T ) = dataStoresRepository.writePrefs(key, value)

    val practitionerDetails = dataStoresRepository.practitioner
    suspend fun writePractitioner (serializablePractitionerDetails: SerializablePractitionerDetails) = dataStoresRepository.writePractitioner(serializablePractitionerDetails)

    val userInfo = dataStoresRepository.userInfo
    suspend fun writeUserInfo(serializableUserInfo: SerializableUserInfo) = dataStoresRepository.writeUserInfo(serializableUserInfo)

    /*
    * Attempt at generics for proto data store fails unless we introduce a key that enables us to know the data type before hand
    * Second attempt still failed. cant cast generic to concrete type. Can only check if it is of that type
     */
//    suspend fun <T: Object> writeObject(value: T ) {
//        when(value) {
//            is String, Int, Long, Boolean -> throw RuntimeException("Error: writeObject() does not accept built in Objects. Use writePreference()") // we don't want to store primitive data types here. Serialization cost
//            is SerializablePractitionerDetails -> dataStoresRepository.writePractitioner(T as SerializablePractitionerDetails)
//            is SerializablePractitionerDetails -> dataStoresRepository.writeUserInfo(T as SerializablePractitionerDetails)
//        }
//    }
//
//    suspend fun <T> writeClass( key: ProtoKeys, value: T) {
//        when(key) {
//            ProtoKeys.PRACTITIONER_DETAILS -> dataStoresRepository.writePractitioner(T as SerializablePractitionerDetails)
//        }
//    }
}