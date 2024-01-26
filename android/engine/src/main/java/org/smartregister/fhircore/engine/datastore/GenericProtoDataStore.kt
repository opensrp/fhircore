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
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.catch
import org.smartregister.fhircore.engine.domain.model.GenericProtoStoreItems
import timber.log.Timber

@Singleton
class GenericProtoDataStore
@Inject
constructor(
  @ApplicationContext val context: Context,
  val dataStore: DataStore<GenericProtoStoreItems>,
) {

  val observe =
    dataStore.data.catch { exception ->
      if (exception is IOException) {
        Timber.e(exception, "Error reading practitioner details preferences.")
        emit(GenericProtoStoreItems())
      } else {
        throw exception
      }
    }

  private suspend fun write(key: Keys, data: List<String>) {
    dataStore.updateData {
      when (key) {
        Keys.CARE_TEAM_IDS -> it.copy(careTeamIds = data)
        Keys.CARE_TEAM_NAMES -> it.copy(careTeamIds = data)
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

  enum class Keys {
    CARE_TEAM_IDS,
    CARE_TEAM_NAMES,
    LOCATION_IDS,
    LOCATION_NAMES,
    ORGANIZATION_IDS,
    ORGANIZATION_NAMES,
  }
}
