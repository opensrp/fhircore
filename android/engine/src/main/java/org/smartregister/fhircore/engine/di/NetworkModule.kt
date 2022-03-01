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

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.parser.IParser
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.data.remote.auth.OAuthService
import org.smartregister.fhircore.engine.data.remote.fhir.resource.FhirConverterFactory
import org.smartregister.fhircore.engine.data.remote.fhir.resource.FhirResourceService
import org.smartregister.fhircore.engine.data.remote.shared.interceptor.OAuthInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

@InstallIn(SingletonComponent::class)
@Module
class NetworkModule {

  @Provides fun provideGson(): Gson = GsonBuilder().setLenient().create()

  @Provides
  @AuthOkHttpClientQualifier
  fun provideAuthOkHttpClient(oAuthInterceptor: OAuthInterceptor) =
    OkHttpClient.Builder()
      .addInterceptor(oAuthInterceptor)
      .addInterceptor(HttpLoggingInterceptor().apply { HttpLoggingInterceptor.Level.BASIC })
      .build()

  @Provides
  @OkHttpClientQualifier
  fun provideOkHttpClient(interceptor: OAuthInterceptor) =
    OkHttpClient.Builder()
      .addInterceptor(interceptor)
      .addInterceptor(HttpLoggingInterceptor().apply { HttpLoggingInterceptor.Level.BODY })
      .build()

  @Provides
  fun provideOauthService(
    @AuthOkHttpClientQualifier okHttpClient: OkHttpClient,
    configurationRegistry: ConfigurationRegistry,
    gson: Gson
  ): OAuthService =
    Retrofit.Builder()
      .baseUrl(configurationRegistry.authConfiguration.oauthServerBaseUrl)
      .client(okHttpClient)
      .addConverterFactory(GsonConverterFactory.create(gson))
      .build()
      .create(OAuthService::class.java)

  @Provides fun provideParser(): IParser = FhirContext.forR4Cached().newJsonParser()

  @Provides
  fun provideFhirResourceService(
    parser: IParser,
    @OkHttpClientQualifier okHttpClient: OkHttpClient,
    configurationRegistry: ConfigurationRegistry,
    gson: Gson
  ): FhirResourceService =
    Retrofit.Builder()
      .baseUrl(configurationRegistry.authConfiguration.fhirServerBaseUrl)
      .client(okHttpClient)
      .addConverterFactory(FhirConverterFactory(parser))
      .addConverterFactory(GsonConverterFactory.create(gson))
      .build()
      .create(FhirResourceService::class.java)
}
