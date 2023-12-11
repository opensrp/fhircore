package org.smartregister.fhircore.engine.datastore

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import org.smartregister.fhircore.engine.ui.bottomsheet.RegisterBottomSheetFragment.Companion.TAG
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.reflect.KClass

@Singleton
class DataStoresRepository @Inject constructor(
        private val dataStore: DataStore<Preferences>,
        private val protoDataStore: DataStore<ProtoDataStoreParams>,
        private val storesProvider: StoresProvider
) {
    val primitive1Preference: Flow<Int> = dataStore.data.map { preferences ->
        preferences[storesProvider.primitive1]?: -1
    }

    suspend fun writePrimitive1Preference (value: Int) {
        dataStore.edit { preferences ->
            preferences[storesProvider.primitive1] = value
        }
    }

    val primitive2Preference: Flow<String> = dataStore.data.map { preferences ->
        preferences[storesProvider.primitive2]?: ""
    }

    suspend fun writePrimitive2Preference (value: String) {
        dataStore.edit { preferences ->
            preferences[storesProvider.primitive2] = value
        }
    }

    val protoDataStoreParams = protoDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    Timber.tag(TAG).e(exception, "Error reading practitioner details preferences.")
                    emit(ProtoDataStoreParams())
                } else {
                    throw exception
                }
            }

    suspend fun writeUserInfo(serializableUserInfo: SerializableUserInfo) {
        protoDataStore.updateData {protoDataStoreParams ->
            protoDataStoreParams.copy(userInfo = serializableUserInfo)

        }
    }

    suspend fun writePractitionerDetails( serializablePractitionerDetails: SerializablePractitionerDetails) {
        protoDataStore.updateData { protoDataStoreParams ->
            protoDataStoreParams.copy(practitionerDetails = serializablePractitionerDetails)

        }
    }

}