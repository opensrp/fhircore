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
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.smartregister.fhircore.engine.data.remote.model.response.LocationHierarchyInfo
import org.smartregister.fhircore.engine.data.remote.model.response.UserInfo
import org.smartregister.fhircore.engine.domain.model.PractitionerPreferences
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProtoDataStore
@Inject
constructor(
  @ApplicationContext val context: Context,
  val dataStore: DataStore<PractitionerPreferences>,
) {

  val observe: Flow<PractitionerPreferences> =
    dataStore.data.catch { exception ->
      if (exception is IOException) {
        Timber.e(exception, "Error observing proto datastore: GenericProtoDataStore details")
        emit(PractitionerPreferences())
      } else {
        throw exception
      }
    }

  fun readOnce(key: Keys, defaultValue: List<String>? = null): List<String>? {
    var data: PractitionerPreferences
    runBlocking { data = observe.first() }
    return when (key) {
      Keys.CARE_TEAM_IDS -> data.careTeamIds
      Keys.CARE_TEAM_NAMES -> data.careTeamNames
      Keys.LOCATION_IDS -> data.locationIds
      Keys.LOCATION_NAMES -> data.locationNames
      Keys.ORGANIZATION_IDS -> data.organizationIds
      Keys.ORGANIZATION_NAMES -> data.organizationNames
      else -> {
        val error = "The key provided is not valid"
        Timber.e(error)
        throw IllegalArgumentException(error)
      }
    }
  }

  suspend fun write(key: Keys, data: List<String>) {
    dataStore.updateData {
      when (key) {
        Keys.CARE_TEAM_IDS -> it.copy(careTeamIds = data)
        Keys.CARE_TEAM_NAMES -> it.copy(careTeamNames = data)
        Keys.LOCATION_IDS -> it.copy(locationIds = data)
        Keys.LOCATION_NAMES -> it.copy(locationNames = data)
        Keys.ORGANIZATION_IDS -> it.copy(organizationIds = data)
        Keys.ORGANIZATION_NAMES -> it.copy(organizationNames = data)
        else -> {
          val error = "The key provided is not valid"
          Timber.e(error)
          throw IllegalArgumentException(error)
        }
      }
    }
  }

  suspend fun writeUserInfo(data: UserInfo) {
    dataStore.updateData { it.copy(userInfo = data) }
  }

  fun readOnceUserInfo() = runBlocking {dataStore.data.first().userInfo }

  suspend fun writeLocationHierarchies(data: List<LocationHierarchyInfo>) {
    dataStore.updateData { it.copy(locationHierarchies = data) }
  }

  fun readOnceLocationHierarchies() = runBlocking {dataStore.data.first().locationHierarchies }

  suspend fun clear() {
    dataStore.updateData {
      it.copy(
        careTeamIds = null,
        careTeamNames = null,
        locationIds = null,
        locationNames = null,
        organizationIds = null,
        organizationNames = null,
        remoteSyncResources = null,
      )
    }
  }

  enum class Keys {
    CARE_TEAM_IDS,
    CARE_TEAM_NAMES,
    LOCATION_IDS,
    LOCATION_NAMES,
    LOCATION_HIERARCHIES,
    ORGANIZATION_IDS,
    ORGANIZATION_NAMES,
  }
}
