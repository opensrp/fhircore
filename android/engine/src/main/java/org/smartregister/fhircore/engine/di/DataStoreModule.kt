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

package org.smartregister.fhircore.engine.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import org.smartregister.fhircore.engine.datastore.serializers.PractitionerDataStoreSerializer
import org.smartregister.fhircore.engine.datastore.serializers.TimeStampDataStoreSerializer
import org.smartregister.fhircore.engine.domain.model.PractitionerPreferences
import org.smartregister.fhircore.engine.domain.model.TimeStampPreferences
import org.smartregister.fhircore.engine.util.DispatcherProvider

@InstallIn(SingletonComponent::class)
@Module
class DataStoreModule {

  @Singleton
  @Provides
  fun providePreferencesDataStore(
    @ApplicationContext context: Context,
    dispatcherProvider: DispatcherProvider,
  ): DataStore<Preferences> {
    val userPreferences = "preferences_datastore"
    return PreferenceDataStoreFactory.create(
      scope = CoroutineScope(dispatcherProvider.io() + SupervisorJob()),
      produceFile = { context.preferencesDataStoreFile(userPreferences) },
    )
  }

  //  @Binds
  //  abstract fun bindPractitionerDataStore(practitionerDataStore: PractitionerDataStore):
  // DataStore<PractitionerPreferences>
  @Singleton
  @Provides
  fun providePractitionerDataStore(
    @ApplicationContext context: Context,
    dispatcherProvider: DispatcherProvider,
  ): DataStore<PractitionerPreferences> {
    val protoStore = "practitioner_datastore.json"

    return DataStoreFactory.create(
      serializer = PractitionerDataStoreSerializer,
      scope = CoroutineScope(dispatcherProvider.io() + SupervisorJob()),
      produceFile = { context.preferencesDataStoreFile(protoStore) },
    )
  }

  //  @Binds
  //  abstract fun bindTimeStampDataStore(timeStampDataStore: TimeStampDataStore):
  // DataStore<TimeStampPreferences>
  @Singleton
  @Provides
  fun provideTimeStampDataStore(
    @ApplicationContext context: Context,
    dispatcherProvider: DispatcherProvider,
  ): DataStore<TimeStampPreferences> {
    val timeStampProtoStore = "time_stamp_datastore.json"

    return DataStoreFactory.create(
      serializer = TimeStampDataStoreSerializer,
      scope = CoroutineScope(dispatcherProvider.io() + SupervisorJob()),
      produceFile = { context.preferencesDataStoreFile(timeStampProtoStore) },
    )
  }
}
