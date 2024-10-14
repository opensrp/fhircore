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
import java.io.File
import java.io.FileInputStream
import javax.inject.Singleton
import org.hl7.fhir.r4.context.SimpleWorkerContext
import org.hl7.fhir.r4.model.Parameters
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.utils.FHIRPathEngine
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.util.helper.TransformSupportServices

@InstallIn(SingletonComponent::class)
@Module
class CoreModule {

  @Singleton
  @Provides
  fun provideWorkerContextProvider(@ApplicationContext context: Context): SimpleWorkerContext =
    SimpleWorkerContext().apply {
      setExpansionProfile(Parameters())
      isCanRunWithoutTerminology = true
      context.filesDir
        .resolve(ConfigurationRegistry.KNOWLEDGE_MANAGER_ASSETS_SUBFOLDER)
        .list()
        ?.forEach { resourceFolder ->
          context.filesDir
            .resolve("${ConfigurationRegistry.KNOWLEDGE_MANAGER_ASSETS_SUBFOLDER}/$resourceFolder")
            .list()
            ?.forEach { file ->
              cacheResource(
                FhirContext.forR4Cached()
                  .newJsonParser()
                  .parseResource(
                    FileInputStream(
                      File(context.filesDir.resolve("km/$resourceFolder/$file").toString()),
                    ),
                  ) as Resource,
              )
            }
        }
    }

  @Singleton
  @Provides
  fun provideFHIRPathEngine(transformSupportServices: TransformSupportServices) =
    FHIRPathEngine(transformSupportServices.simpleWorkerContext)

  @Singleton
  @Provides
  fun provideApplicationManager(@ApplicationContext context: Context): AccountManager =
    AccountManager.get(context)

  @Singleton
  @Provides
  fun provideKnowledgeManager(@ApplicationContext context: Context): KnowledgeManager =
    KnowledgeManager.create(context)

  @Singleton @Provides fun provideFhirContext(): FhirContext = FhirContext.forR4Cached()!!

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
}
