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
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.knowledge.KnowledgeManager
import com.google.android.fhir.workflow.FhirOperator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Provider
import javax.inject.Singleton
import org.hl7.fhir.r4.context.SimpleWorkerContext
import org.smartregister.fhircore.engine.auditEvent.AuditEventRepository
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.app.ConfigService
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.data.local.register.dao.HivRegisterDao
import org.smartregister.fhircore.engine.data.remote.shared.TokenAuthenticator
import org.smartregister.fhircore.engine.domain.repository.PatientDao
import org.smartregister.fhircore.engine.sync.SyncBroadcaster
import org.smartregister.fhircore.engine.trace.PerformanceReporter
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.worker.CoreSimpleWorkerContext

@InstallIn(SingletonComponent::class)
@Module
class CoreModule {

  @Singleton
  @Provides
  fun provideSyncBroadcaster(
    @ApplicationContext context: Context,
    configurationRegistry: ConfigurationRegistry,
    configService: ConfigService,
    fhirEngine: FhirEngine,
    tracer: PerformanceReporter,
    tokenAuthenticator: TokenAuthenticator,
    sharedPreferencesHelper: SharedPreferencesHelper,
  ) =
    SyncBroadcaster(
      configurationRegistry = configurationRegistry,
      configService = configService,
      fhirEngine = fhirEngine,
      appContext = context,
      tracer = tracer,
      tokenAuthenticator = tokenAuthenticator,
      sharedPreferencesHelper = sharedPreferencesHelper,
    )

  @Singleton
  @Provides
  fun provideWorkerContextProvider(): SimpleWorkerContext {
    return CoreSimpleWorkerContext()
  }

  @Singleton
  @Provides
  fun provideApplicationManager(@ApplicationContext context: Context): AccountManager =
    AccountManager.get(context)

  @Singleton @Provides fun provideFhirContext(): FhirContext = FhirContext.forR4Cached()!!

  @Singleton
  @Provides
  fun provideKnowledgeManager(@ApplicationContext context: Context): KnowledgeManager =
    KnowledgeManager.create(context)

  @Singleton
  @Provides
  fun provideFhirOperator(
    @ApplicationContext context: Context,
    fhirContext: FhirContext,
    fhirEngine: FhirEngine,
    knowledgeManager: KnowledgeManager,
  ): FhirOperator =
    FhirOperator.Builder(context)
      .fhirEngine(fhirEngine)
      .fhirContext(fhirContext)
      .knowledgeManager(knowledgeManager)
      .build()

  @Singleton
  @Provides
  @HivPatient
  fun providePatientDao(
    fhirEngine: FhirEngine,
    defaultRepository: DefaultRepository,
    configurationRegistry: Provider<ConfigurationRegistry>,
  ): PatientDao = HivRegisterDao(fhirEngine, defaultRepository, configurationRegistry)

  @Singleton
  @Provides
  fun provideAudiEventRepository(
    defaultRepository: DefaultRepository,
    sharedPreferencesHelper: SharedPreferencesHelper,
  ): AuditEventRepository = AuditEventRepository(defaultRepository, sharedPreferencesHelper)
}
