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

import android.content.Context
import com.google.android.fhir.DatabaseErrorStrategy
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.FhirEngineConfiguration
import com.google.android.fhir.FhirEngineProvider
import com.google.android.fhir.ServerConfiguration
import com.google.android.fhir.sync.Authenticator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import org.smartregister.fhircore.engine.auth.TokenManagerService
import org.smartregister.fhircore.engine.configuration.app.ConfigService

/**
 * Provide [FhirEngine] dependency in isolation so we can replace it with a fake dependency in test
 */
@InstallIn(SingletonComponent::class)
@Module(includes = [CoreModule::class])
class FhirEngineModule {

  @Singleton
  @Provides
  fun provideFhirEngine(
    @ApplicationContext context: Context,
    tokenManagerService: TokenManagerService,
    configService: ConfigService
  ): FhirEngine {
    FhirEngineProvider.init(
      FhirEngineConfiguration(
        enableEncryptionIfSupported = true,
        DatabaseErrorStrategy.UNSPECIFIED,
        ServerConfiguration(
          baseUrl = configService.provideAuthConfiguration().fhirServerBaseUrl,
          authenticator =
            object : Authenticator {
              override fun getAccessToken() = tokenManagerService.getBlockingActiveAuthToken() ?: ""
            }
        )
      )
    )

    return FhirEngineProvider.getInstance(context)
  }
}
