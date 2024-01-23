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
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.JsonIOException
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.extension.encodeJson
import timber.log.Timber

@Singleton
class PreferencesDataStore
@Inject
constructor(
  @ApplicationContext val context: Context,
  val dispatcherProvider: DispatcherProvider,
) {
  val Context.dataStore by
    preferencesDataStore(
      name = PREFERENCES_STORE_NAME,
      scope = CoroutineScope(dispatcherProvider.io() + SupervisorJob()),
    )

  val gson = Gson()

  /**
   * This blocking read function was made to prevent making functions all over the codebase suspend
   * functions when they only needed as single value from the datastore and had no need to keep
   * observing the flow. A lot of the SharedPreferences reads had this need
   *
   * For instance the ConfigService.provideResourceTags() function just needed one preferences
   * value.
   *
   * In situations where you provide a non-null defaultValue, you can use !! to extract
   */
  fun <T> readOnce(key: Preferences.Key<T>, defaultValue: T? = null) = runBlocking {
    context.dataStore.data.first()[key] ?: defaultValue
  }

  inline fun <reified T> readOnce(
    key: Preferences.Key<String>,
  ): T? {
    var out: T? = null
    runBlocking {
      context.dataStore.data.first()[key].also {
        try {
          out = gson.fromJson(it, T::class.java)
        } catch (jsonIoException: JsonIOException) {
          Timber.e(jsonIoException)
        }
      }
    }

    return out
  }

  fun <T> observe(key: Preferences.Key<T>, defaultValue: T? = null) =
    context.dataStore.data
      .catch { exception ->
        if (exception is IOException) {
          emit(emptyPreferences())
        } else {
          throw exception
        }
      }
      .map { preferences -> preferences[key] ?: defaultValue }

  inline fun <reified T, M> observe(
    key: Preferences.Key<M>,
  ): Flow<T?> =
    context.dataStore.data
      .catch { exception ->
        if (exception is IOException) {
          emit(emptyPreferences())
        } else {
          throw exception
        }
      }
      .map { preferences ->
        try {
          gson.fromJson(preferences[key] as String, T::class.java)
        } catch (jsonIoException: JsonIOException) {
          Timber.e(jsonIoException)
          null
        }
      }

  suspend fun <T> write(
    key: Preferences.Key<T>,
    dataToStore: T,
  ) { // named dataToStore instead of data to prevent overload ambiguity
    context.dataStore.edit { preferences -> preferences[key] = dataToStore }
  }

  suspend inline fun <reified T> write(
    key: Preferences.Key<String>,
    data: T,
    encodeWithGson: Boolean = true,
  ) {
    val dataToStore = if (encodeWithGson) gson.toJson(data) else data.encodeJson()
    context.dataStore.edit { preferences -> preferences[key] = dataToStore }
  }

  suspend fun <T> remove(key: Preferences.Key<T>) {
    context.dataStore.edit { it.remove(key) }
  }

  suspend fun clear() {
    context.dataStore.edit { it.clear() }
  }

  // expose flows to be used all over the engine and view models
  val appId by lazy { observe(APP_ID, null) }
  val lang by lazy { observe(LANG, defaultValue = Locale.ENGLISH.toLanguageTag()) }
  val careTeamIds by lazy { observe(CARE_TEAM_IDS, defaultValue = "") }
  val careTeamNames by lazy { observe(CARE_TEAM_NAMES, defaultValue = "") }
  val lastSyncTimeStamp by lazy { observe(LAST_SYNC_TIMESTAMP, defaultValue = null) }
  val locationIds by lazy { observe(LOCATION_IDS, defaultValue = "") }
  val locationNames by lazy { observe(LOCATION_NAMES, defaultValue = "") }
  val organizationIds by lazy { observe(ORGANIZATION_IDS, defaultValue = "") }
  val organizationNames by lazy { observe(ORGANIZATION_NAMES, defaultValue = "") }
  val practitionerId by lazy { observe(PRACTITIONER_ID, defaultValue = "") }
  val practitionerLocation by lazy { observe(PRACTITIONER_LOCATION, defaultValue = "") }
  val practitionerLocationHierarchies by lazy {
    observe(PRACTITIONER_LOCATION_HIERARCHIES, defaultValue = "")
  }
  val practitionerDetails by lazy { observe(PRACTITIONER_DETAILS, defaultValue = "") }
  val remoteSyncResources by lazy { observe<List<String>, String>(REMOTE_SYNC_RESOURCES) }
  val migrationVersion by lazy { observe(MIGRATION_VERSION, defaultValue = null) }

  companion object Keys {
    const val PREFERENCES_STORE_NAME = "preferences_datastore"
    const val PREFS_SYNC_PROGRESS_TOTAL = "sync_progress_total"

    // Keys
    val APP_ID by lazy { stringPreferencesKey("APP_ID") }
    val LANG by lazy { stringPreferencesKey("LANG") }
    val CARE_TEAM_IDS by lazy { stringPreferencesKey("CARE_TEAM_IDS") }
    val CARE_TEAM_NAMES by lazy { stringPreferencesKey("CARE_TEAM_NAMES") }
    val LAST_SYNC_TIMESTAMP by lazy { stringPreferencesKey("LAST_SYNC_TIMESTAMP") }
    val LOCATION_IDS by lazy { stringPreferencesKey("LOCATION_IDS") }
    val LOCATION_NAMES by lazy { stringPreferencesKey("LOCATION_NAMES") }
    val ORGANIZATION_IDS by lazy { stringPreferencesKey("ORGANIZATION_IDS") }
    val ORGANIZATION_NAMES by lazy { stringPreferencesKey("ORGANIZATION_NAMES") }
    val PRACTITIONER_ID by lazy { stringPreferencesKey("PRACTITIONER_ID") }
    val PRACTITIONER_LOCATION by lazy { stringPreferencesKey("PRACTITIONER_LOCATION ") }
    val PRACTITIONER_LOCATION_HIERARCHIES by lazy { stringPreferencesKey("LOCATION_HIERARCHIES") }
    val USER_INFO by lazy { stringPreferencesKey("USER_INFO") }
    val PRACTITIONER_DETAILS by lazy { stringPreferencesKey("PRACTITIONER_DETAILS") }
    val REMOTE_SYNC_RESOURCES by lazy { stringPreferencesKey("REMOTE_SYNC_RESOURCES") }
    val MIGRATION_VERSION by lazy { intPreferencesKey("migrationVersion") }
  }
}
