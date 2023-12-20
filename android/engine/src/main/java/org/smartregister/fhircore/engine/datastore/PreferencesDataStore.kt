/*
 * Copyright 2021-2023 Ona Systems, Inc
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
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

const val DATASTORE_NAME = "preferences_datastore"
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = DATASTORE_NAME)

@Singleton
class PreferencesDataStore @Inject constructor(@ApplicationContext val context: Context) {
   fun <T> read(key: Preferences.Key<T>) =
    context.dataStore.data
      .catch { exception ->
        if (exception is IOException) {
          emit(emptyPreferences())
        } else {
          throw exception
        }
      }
      .map { preferences -> preferences[key] as T }

    // expose flows to be used all over the engine and view models
    val appId by lazy { read(APP_ID) }
    val lang by lazy { read(LANG) }
    val careTeam by lazy { read(CARE_TEAM) }
    val organization by lazy { read(ORGANIZATION) }
    val practitionerLocation by lazy { read(PRACTITIONER_LOCATION) }
    val practitionerId by lazy { read(PRACTITIONER_ID) }


  suspend fun <T> write(key: Preferences.Key<T>, data: T) {
    context.dataStore.edit { preferences -> preferences[key] = data }
  }

  companion object Keys {
      val APP_ID by lazy { stringPreferencesKey("APP_ID") }
      val LANG by lazy { stringPreferencesKey("LANG") }
      val CARE_TEAM by lazy { stringPreferencesKey("CARE_TEAM") }
      val ORGANIZATION by lazy { stringPreferencesKey("ORGANIZATION") }
      val PRACTITIONER_ID by lazy { stringPreferencesKey("PRACTITIONER_ID") }
      val PRACTITIONER_LOCATION by lazy { stringPreferencesKey("PRACTITIONER_LOCATION ") }
  }
}
