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
import ca.uhn.fhir.parser.IParser
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.smartregister.fhircore.engine.auth.TokenManagerService
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.data.remote.auth.OAuthService
import org.smartregister.fhircore.engine.data.remote.fhir.resource.FhirConverterFactory
import org.smartregister.fhircore.engine.data.remote.fhir.resource.FhirResourceService
import org.smartregister.fhircore.engine.data.remote.shared.interceptor.OAuthInterceptor
import org.smartregister.fhircore.engine.util.SecureSharedPreference
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module(includes = [CommonModule::class])
class NetworkModule {

  @Provides fun provideGson(): Gson = GsonBuilder().setLenient().create()

  @Singleton
  @Provides
  fun provideApplicationManager(@ApplicationContext context: Context): AccountManager =
    AccountManager.get(context)

  @Provides fun provideTokenManagerService(@ApplicationContext context: Context,
    applicationManager: AccountManager, secureSharedPreference: SecureSharedPreference
  ): TokenManagerService =
    TokenManagerService(context, applicationManager, secureSharedPreference)

  @Provides fun provideOAuthInterceptor(
    @ApplicationContext context: Context, tokenManagerService: TokenManagerService): OAuthInterceptor =
    OAuthInterceptor(context, tokenManagerService)

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
    gson: Gson
  ): FhirResourceService =
    Retrofit.Builder()
            // TODO???????????????????
      .baseUrl("https://fhir.labs.smartregister.org/fhir/")
      .client(okHttpClient)
      .addConverterFactory(FhirConverterFactory(parser))
      .addConverterFactory(GsonConverterFactory.create(gson))
      .build()
      .create(FhirResourceService::class.java)
}
