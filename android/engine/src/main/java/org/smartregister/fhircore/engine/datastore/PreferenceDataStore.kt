/*
 * Copyright 2021-2024 Ona Systems, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.smartregister.fhircore.engine.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.Dispatcher
import kotlin.coroutines.CoroutineContext

//const val DATASTORE_NAME = "preferences_datastore"
//val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = DATASTORE_NAME)

@Singleton
class PreferenceDataStore @Inject constructor(@ApplicationContext val context: Context, val dataStore: DataStore<Preferences>) {
  fun <T> read(key: Preferences.Key<T>) =
    dataStore.data
      .catch { exception ->
        if (exception is IOException) {
          emit(emptyPreferences())
        } else {
          throw exception
        }
      }
      .map { preferences -> preferences[key] as T }

  fun <T> readOnce(key: Preferences.Key<T>, defaultValue: T? = null) = runBlocking {
    dataStore.data.map {preferences ->
      preferences[key] ?: defaultValue
    }.firstOrNull()
  }

  suspend fun <T> write(key: Preferences.Key<T>, value: T) {
    dataStore.edit { preferences -> preferences[key] = value }
  }

  suspend fun write(key: Preferences.Key<String>, value: String) {
    dataStore.edit { preferences -> preferences[key] = value }
  }

  companion object Keys {
    val APP_ID by lazy { stringPreferencesKey("appId") }
    val LANG by lazy { stringPreferencesKey("lang") }
    val MIGRATION_VERSION by lazy { intPreferencesKey("migrationVersion") }
    val LAST_SYNC_TIMESTAMP by lazy { stringPreferencesKey("lastSyncTimestamp") }
    val PRACTITIONER_ID by lazy { stringPreferencesKey("practitionerId") }
    val PRACTITIONER_LOCATION by lazy { stringPreferencesKey("practitionerLocation") }
    val REMOTE_SYNC_RESOURCES by lazy { stringPreferencesKey("remoteSyncResources") }

    val CARE_TEAM_ID by lazy { stringPreferencesKey("careTeamId") }
    val ORGANIZATION_ID by lazy { stringPreferencesKey("organizationId") }
    val LOCATION_ID by lazy { stringPreferencesKey("locationId") }
    val PRACTITIONER_LOCATION_NAME by lazy { stringPreferencesKey("locationId") }
    val CARE_TEAM_NAME by lazy { stringPreferencesKey("locationId") }
    val ORGANIZATION_NAME by lazy { stringPreferencesKey("locationId") }
  }
}
