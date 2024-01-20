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

package org.smartregister.fhircore.engine.app.di.module

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import org.smartregister.fhircore.engine.app.testDispatcher
import org.smartregister.fhircore.engine.datastore.PreferencesDataStore
import org.smartregister.fhircore.engine.di.DataStoreModule

@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [DataStoreModule::class])
class FakeDataStoreModule {

  @OptIn(ExperimentalCoroutinesApi::class)
  @Provides
  @Singleton
  fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
    val testCoroutineScope = CoroutineScope(testDispatcher + SupervisorJob())
    val testDataStoreName = "test_datastore"
    return PreferenceDataStoreFactory.create(
      scope = testCoroutineScope,
      produceFile = { context.preferencesDataStoreFile(testDataStoreName) },
    )
  }

  @Provides
  @Singleton
  fun providePreferencesDataStore(
    @ApplicationContext context: Context,
    testDataStore: DataStore<Preferences>,
  ): PreferencesDataStore =
    PreferencesDataStore(
      context,
      testDataStore,
    )
}
