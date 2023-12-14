package org.smartregister.fhircore.engine.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import org.smartregister.fhircore.engine.datastore.mockdata.PrefsDataStoreParams
import org.smartregister.fhircore.engine.datastore.mockdata.ProtoDataStoreParams
import org.smartregister.fhircore.engine.datastore.mockdata.SerializablePractitionerDetails
import org.smartregister.fhircore.engine.datastore.mockdata.SerializableUserInfo
import org.smartregister.fhircore.engine.ui.bottomsheet.RegisterBottomSheetFragment.Companion.TAG
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.reflect.KClass

@Singleton
class DataStoresRepository @Inject constructor( private val context: Context ) {
    companion object Keys {
        val APP_ID = stringPreferencesKey("appId")
        val LANG = stringPreferencesKey("lang")
    }

    val preferences: Flow<PrefsDataStoreParams> = context.dataStore.data
        .catch { exception ->
        if (exception is IOException) {
            emit(emptyPreferences())
        } else {
            throw exception
        }
        }.map { preferences ->
            PrefsDataStoreParams(
                    appId = preferences[APP_ID] ?: "",
                    lang = preferences[LANG] ?: ""
            )
        }

    suspend fun <T> writePrefs(key: Preferences.Key<T>, data: T) {
        context.dataStore.edit {preferences ->
            preferences[key] = data
        }
    }

    val practitioner = context.practitionerProtoStore.data
            .catch { exception ->
                if (exception is IOException) {
                    Timber.tag(TAG).e(exception, "Error reading practitioner details preferences.")
                    emit(SerializablePractitionerDetails())
                } else {
                    throw exception
                }
            }

    suspend fun writePractitioner(serializablePractitionerDetails: SerializablePractitionerDetails) {
        context.practitionerProtoStore.updateData { practitionerData ->
            practitionerData.copy(
                    name = serializablePractitionerDetails.name,
                    id = serializablePractitionerDetails.id
            )
        }
    }

    val userInfo = context.userInfoProtoStore.data
            .catch { exception ->
                if (exception is IOException) {
                    Timber.tag(TAG).e(exception, "Error reading practitioner details preferences.")
                    emit(SerializableUserInfo())
                } else {
                    throw exception
                }
            }

    suspend fun writeUserInfo(serializableUserInfo: SerializableUserInfo) {
        context.userInfoProtoStore.updateData { userInfo ->
            userInfo.copy(
                    name = serializableUserInfo.name
            )
        }
    }
}