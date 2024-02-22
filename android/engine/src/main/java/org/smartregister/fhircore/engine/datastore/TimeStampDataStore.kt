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
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.datastore.serializers.TimeStampDataStoreSerializer
import org.smartregister.fhircore.engine.domain.model.TimeStampPreferences
import org.smartregister.fhircore.engine.util.DispatcherProvider
import timber.log.Timber

@Singleton
class TimeStampDataStore
@Inject
constructor(@ApplicationContext context: Context, val dispatcherProvider: DispatcherProvider) {
  var dataStore: DataStore<TimeStampPreferences> =
    DataStoreFactory.create(
      serializer = TimeStampDataStoreSerializer,
      scope = CoroutineScope(dispatcherProvider.io() + SupervisorJob()),
      produceFile = { context.preferencesDataStoreFile(TIMESTAMP_DATASTORE) },
    )

  val observe: Flow<TimeStampPreferences> =
    dataStore.data.catch { exception ->
      if (exception is IOException) {
        Timber.e(exception, "Error observing timestamp protostore")
        emit(TimeStampPreferences(emptyMap()))
      } else {
        throw exception
      }
    }

  fun readOnce(resourceType: ResourceType) = runBlocking {
    dataStore.data.first().map.get(resourceType)
  }

  suspend fun write(resourceType: ResourceType, timeStamp: String) {
    dataStore.updateData {
      val currentValue = it.map
      val updatedValue = currentValue + (resourceType to timeStamp)

      it.copy(
        map = updatedValue,
      )
    }
  }

  companion object {
    const val TIMESTAMP_DATASTORE = "time_stamp_datastore.json"
  }
}
