/*
 * Copyright 2021 Ona Systems, Inc
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

package org.smartregister.fhircore.quest.app.fakes

import android.accounts.AccountManager
import android.content.Context
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.FhirEngineProvider
import com.google.android.fhir.sync.Sync
import com.google.android.fhir.sync.SyncJob
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton
import org.hl7.fhir.r4.context.SimpleWorkerContext
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.app.ConfigService
import org.smartregister.fhircore.engine.di.EngineModule
import org.smartregister.fhircore.engine.sync.SyncBroadcaster
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper

@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [EngineModule::class])
class FakeEngineModule {

  @Provides
  fun provideFhirEngine(@ApplicationContext context: Context): FhirEngine {
    return FhirEngineProvider.getInstance(context)
  }

  @Singleton
  @Provides
  fun provideSyncJob(@ApplicationContext context: Context) = Sync.basicSyncJob(context)

  @Provides
  fun provideSyncBroadcaster(
    configurationRegistry: ConfigurationRegistry,
    sharedPreferencesHelper: SharedPreferencesHelper,
    configService: ConfigService,
    syncJob: SyncJob,
    fhirEngine: FhirEngine
  ) =
    SyncBroadcaster(
      configurationRegistry = configurationRegistry,
      sharedPreferencesHelper = sharedPreferencesHelper,
      configService = configService,
      fhirEngine = fhirEngine,
      syncJob = syncJob
    )

  @Singleton @Provides fun provideWorkerContextProvider() = SimpleWorkerContext()

  @Singleton
  @Provides
  fun provideApplicationManager(@ApplicationContext context: Context): AccountManager =
    AccountManager.get(context)
}
