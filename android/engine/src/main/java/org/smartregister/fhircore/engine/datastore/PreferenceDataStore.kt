package org.smartregister.fhircore.engine.datastore

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import org.smartregister.fhircore.engine.datastore.mockdata.SerializablePractitionerDetails
import org.smartregister.fhircore.engine.datastore.mockdata.SerializableUserInfo
import org.smartregister.fhircore.engine.ui.bottomsheet.RegisterBottomSheetFragment.Companion.TAG
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataStoresPreference @Inject constructor(@ApplicationContext val context: Context ) {
    fun <T> read(key: Preferences.Key<T>) = context.dataStore.data
        .catch { exception ->
        if (exception is IOException) {
            emit(emptyPreferences())
        } else {
            throw exception
        }
        }.map { preferences ->
           preferences[key] as T
        }

    suspend fun <T> write(key: Preferences.Key<T>, data: T) {
        context.dataStore.edit {preferences ->
            preferences[key] = data
        }
    }

    companion object Keys {
        val APP_ID = stringPreferencesKey("appId")
        val LANG = stringPreferencesKey("lang")
    }
}