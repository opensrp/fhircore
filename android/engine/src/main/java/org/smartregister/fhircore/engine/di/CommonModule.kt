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

package org.smartregister.fhircore.engine.di

import android.accounts.AccountManager
import android.content.Context
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.FhirEngineProvider
import com.google.android.fhir.sync.Sync
import com.google.android.fhir.sync.SyncJob
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import org.hl7.fhir.r4.context.SimpleWorkerContext
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.data.remote.fhir.resource.FhirResourceDataSource
import org.smartregister.fhircore.engine.sync.SyncBroadcaster

@InstallIn(SingletonComponent::class)
@Module(includes = [NetworkModule::class, DispatcherModule::class])
class CommonModule {

  @Singleton
  @Provides
  fun provideFhirEngine(@ApplicationContext context: Context) =
    FhirEngineProvider.getInstance(context)

  @Singleton
  @Provides
  fun provideSyncJob(@ApplicationContext context: Context) = Sync.basicSyncJob(context)

  @Singleton
  @Provides
  fun provideSyncBroadcaster(
    fhirResourceDataSource: FhirResourceDataSource,
    configurationRegistry: ConfigurationRegistry,
    syncJob: SyncJob,
    fhirEngine: FhirEngine
  ) =
    SyncBroadcaster(
      fhirEngine = fhirEngine,
      syncJob = syncJob,
      configurationRegistry = configurationRegistry,
      fhirResourceDataSource = fhirResourceDataSource
    )

  @Singleton @Provides fun provideWorkerContextProvider() = SimpleWorkerContext()
}
