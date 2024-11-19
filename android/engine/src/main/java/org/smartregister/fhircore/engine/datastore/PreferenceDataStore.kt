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
import androidx.datastore.dataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.android.fhir.sync.SyncJobStatus
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.util.SharedPreferenceKey
import org.smartregister.fhircore.engine.util.extension.lastOffset
import org.smartregister.model.practitioner.PractitionerDetails
import java.util.Locale

@Singleton
open class PreferenceDataStore
@Inject
constructor(@ApplicationContext val context: Context, val dataStore: DataStore<Preferences>) {
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
    dataStore.data.map { preferences -> preferences[key] ?: defaultValue }.firstOrNull()
  }

  suspend fun <T> write(key: Preferences.Key<T>, value: T) {
    dataStore.edit { preferences -> preferences[key] = value }
  }

  suspend fun write(key: Preferences.Key<String>, value: String) {
    dataStore.edit { preferences -> preferences[key] = value }
  }

  suspend fun <T> remove(key: Preferences.Key<T>) {
    dataStore.edit { it.remove(key) }
  }

  suspend fun clear() {
    dataStore.edit { it.clear() }
  }

  fun retrieveApplicationId() = read(APP_ID).toString()

  fun resetPreferences() {
    runBlocking {
      dataStore.edit {
        preferences ->
          preferences.clear()
      }
    }
  }

  companion object Keys {
    val PRACTITIONER_DETAILS by lazy { stringPreferencesKey("practitioner_details") }
    val APP_ID by lazy { stringPreferencesKey("appId") }
    val LANG by lazy { stringPreferencesKey("lang") }
    val MIGRATION_VERSION by lazy { intPreferencesKey("migrationVersion") }
    val LAST_SYNC_TIMESTAMP by lazy { stringPreferencesKey("lastSyncTimestamp") }
    val PRACTITIONER_ID by lazy { stringPreferencesKey("practitionerId") }
    val PRACTITIONER_LOCATION by lazy { stringPreferencesKey("practitionerLocation") }
    val PRACTITIONER_LOCATION_ID by lazy { stringPreferencesKey("practitionerLocationId") }
    val REMOTE_SYNC_RESOURCES by lazy { stringPreferencesKey("remoteSyncResources") }
    val PREFS_SYNC_PROGRESS_TOTAL by lazy { longPreferencesKey("syncProgressTotal") }
    val CARE_TEAM_ID by lazy { stringPreferencesKey("careTeamId") }
    val ORGANIZATION_ID by lazy { stringPreferencesKey("organizationId") }
    val LOCATION_ID by lazy { stringPreferencesKey("locationId") }
    val PRACTITIONER_LOCATION_NAME by lazy { stringPreferencesKey("practitionerLocationName") }
    val CARE_TEAM_NAME by lazy { stringPreferencesKey("careTeamName") }
    val ORGANIZATION_NAME by lazy { stringPreferencesKey("organizationName") }

    val CAREPLAN_LAST_SYNC_TIMESTAMP by lazy { stringPreferencesKey("careplanLastSyncTimestamp") }
    val COMPLETE_CAREPLAN_WORKER_LAST_OFFSET by lazy { intPreferencesKey("completeCarePlanWorkerLastOffset".lastOffset()) }

    val CAREPLAN_WORK_ID by lazy { stringPreferencesKey("careplanWorkerId") }
    val CAREPLAN_BATCH_SIZE_FACTOR by lazy { intPreferencesKey("careplanBatchSizeFactor") }

    val USER_INFO by lazy { stringPreferencesKey("user_info") }
    fun resourceTimestampKey(resourceType: ResourceType) = stringPreferencesKey(
      "${resourceType.name.uppercase()}_${SharedPreferenceKey.LAST_SYNC_TIMESTAMP.name}"
    )

    val PRACTITIONER_LOCATION_HIERARCHIES by lazy { stringPreferencesKey("practitionerLocationHierarchies") }

    fun syncProgress(progressSyncJobStatus: SyncJobStatus.InProgress) = longPreferencesKey(
      "$PREFS_SYNC_PROGRESS_TOTAL + ${
            progressSyncJobStatus.syncOperation.name}")

    fun currentLanguage() = stringPreferencesKey(
      "$LANG, ${Locale.ENGLISH.toLanguageTag()}")

    val PREFS_NAME by lazy { stringPreferencesKey("params")  }

    val Context.dataStore by preferencesDataStore(PREFS_NAME.name)

    //TODO: check this vs similar one above
    fun resetPrefs(context: Context) = runBlocking {
      context.dataStore.edit { it.clear() }
    }
  }
}
