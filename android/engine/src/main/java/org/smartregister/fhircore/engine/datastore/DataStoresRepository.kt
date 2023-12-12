package org.smartregister.fhircore.engine.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import org.smartregister.fhircore.engine.datastore.mockdata.PrefsDataStoreParams
import org.smartregister.fhircore.engine.datastore.mockdata.ProtoDataStoreParams
import org.smartregister.fhircore.engine.datastore.mockdata.SerializablePractitionerDetails
import org.smartregister.fhircore.engine.datastore.mockdata.SerializableUserInfo
import org.smartregister.fhircore.engine.datastore.prefsdatastore.PrefsDataStoreProvider
import org.smartregister.fhircore.engine.ui.bottomsheet.RegisterBottomSheetFragment.Companion.TAG
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataStoresRepository @Inject constructor(
        private val prefsDataStore: DataStore<Preferences>,
        private val protoDataStore: DataStore<ProtoDataStoreParams>,

) {
    private object PreferenceKeys {
        val APP_ID = stringPreferencesKey("appId")
        val LANG = stringPreferencesKey("lang")
    }
    val readPrefsDataStore: Flow<PrefsDataStoreParams> = prefsDataStore.data
        .catch { exception ->
        if (exception is IOException) {
            emit(emptyPreferences())
        } else {
            throw exception
        }
        }.map { preferences ->
            PrefsDataStoreParams(
                    appId = preferences[PreferenceKeys.APP_ID] ?: "",
                    lang = preferences[PreferenceKeys.LANG] ?: ""
            )
        }
    }

    suspend fun writeToPrefs

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