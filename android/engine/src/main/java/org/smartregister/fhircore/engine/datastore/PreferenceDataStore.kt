package org.smartregister.fhircore.engine.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

const val DATASTORE_NAME = "preferences_datastore"
val Context.dataStore : DataStore<Preferences> by preferencesDataStore(name = DATASTORE_NAME)

@Singleton
class PreferenceDataStore @Inject constructor(@ApplicationContext val context: Context ) {
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
        val appIdKeyName = "appId"
        val langKeyName = "lang"
        val APP_ID by lazy { stringPreferencesKey(appIdKeyName) }
        val LANG by lazy { stringPreferencesKey(langKeyName) }
    }
}