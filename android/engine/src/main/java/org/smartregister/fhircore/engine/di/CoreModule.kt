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
import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.FhirVersionEnum
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.workflow.FhirOperator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import org.hl7.fhir.r4.context.SimpleWorkerContext
import org.hl7.fhir.r4.model.Parameters
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.app.ConfigService
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.data.local.register.dao.HivRegisterDao
import org.smartregister.fhircore.engine.domain.repository.PatientDao
import org.smartregister.fhircore.engine.sync.SyncBroadcaster

@InstallIn(SingletonComponent::class)
@Module(includes = [NetworkModule::class, DispatcherModule::class])
class CoreModule {

  @Singleton
  @Provides
  fun provideSyncBroadcaster(
    @ApplicationContext context: Context,
    configurationRegistry: ConfigurationRegistry,
    configService: ConfigService,
    fhirEngine: FhirEngine
  ) =
    SyncBroadcaster(
      configurationRegistry = configurationRegistry,
      configService = configService,
      fhirEngine = fhirEngine,
      appContext = context
    )

  @Singleton
  @Provides
  fun provideWorkerContextProvider(): SimpleWorkerContext =
    SimpleWorkerContext().apply {
      setExpansionProfile(Parameters())
      isCanRunWithoutTerminology = true
    }

  @Singleton
  @Provides
  fun provideApplicationManager(@ApplicationContext context: Context): AccountManager =
    AccountManager.get(context)

  @Singleton
  @Provides
  fun provideFhirOperator(fhirEngine: FhirEngine): FhirOperator =
    FhirOperator(fhirContext = FhirContext.forCached(FhirVersionEnum.R4), fhirEngine = fhirEngine)

  @Singleton
  @Provides
  @HivPatient
  fun providePatientDao(
    fhirEngine: FhirEngine,
    defaultRepository: DefaultRepository,
    configurationRegistry: ConfigurationRegistry
  ): PatientDao = HivRegisterDao(fhirEngine, defaultRepository, configurationRegistry)
}
