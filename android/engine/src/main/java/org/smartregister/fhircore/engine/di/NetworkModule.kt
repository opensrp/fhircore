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
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.smartregister.fhircore.engine.BuildConfig
import org.smartregister.fhircore.engine.configuration.app.ConfigService
import org.smartregister.fhircore.engine.data.remote.auth.KeycloakService
import org.smartregister.fhircore.engine.data.remote.auth.OAuthService
import org.smartregister.fhircore.engine.data.remote.fhir.resource.FhirConverterFactory
import org.smartregister.fhircore.engine.data.remote.fhir.resource.FhirResourceService
import org.smartregister.fhircore.engine.data.remote.shared.TokenAuthenticator
import org.smartregister.fhircore.engine.util.extension.getCustomJsonParser
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

@InstallIn(SingletonComponent::class)
@Module
class NetworkModule {

  @Provides
  @AuthOkHttpClientQualifier
  @Singleton
  fun provideAuthOkHttpClient() =
    OkHttpClient.Builder()
      .addInterceptor(
        HttpLoggingInterceptor().apply {
          level = HttpLoggingInterceptor.Level.BASIC
          redactHeader(AUTHORIZATION)
          redactHeader(COOKIE)
        }
      )
      .connectTimeout(TIMEOUT_DURATION, TimeUnit.SECONDS)
      .readTimeout(TIMEOUT_DURATION, TimeUnit.SECONDS)
      .callTimeout(TIMEOUT_DURATION, TimeUnit.SECONDS)
      .build()

  @Provides
  @KeycloakOkHttpClientQualifier
  @Singleton
  fun provideKeycloakOkHttpClient(tokenAuthenticator: TokenAuthenticator) =
    OkHttpClient.Builder()
      .addInterceptor(
        HttpLoggingInterceptor().apply {
          level =
            if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
            else HttpLoggingInterceptor.Level.BASIC
          redactHeader(AUTHORIZATION)
          redactHeader(COOKIE)
        }
      )
      .addInterceptor(
        Interceptor { chain: Interceptor.Chain ->
          val accessToken = tokenAuthenticator.getAccessToken()
          val request =
            chain.request().newBuilder().addHeader(AUTHORIZATION, "Bearer $accessToken").build()
          chain.proceed(request)
        }
      )
      .connectTimeout(TIMEOUT_DURATION, TimeUnit.SECONDS)
      .readTimeout(TIMEOUT_DURATION, TimeUnit.SECONDS)
      .callTimeout(TIMEOUT_DURATION, TimeUnit.SECONDS)
      .build()

  @Provides
  @OkHttpClientQualifier
  @Singleton
  fun provideOkHttpClient(tokenAuthenticator: TokenAuthenticator) =
    OkHttpClient.Builder()
      .addInterceptor(
        HttpLoggingInterceptor().apply {
          level =
            if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
            else HttpLoggingInterceptor.Level.BASIC
          redactHeader(AUTHORIZATION)
          redactHeader(COOKIE)
        }
      )
      .addInterceptor(
        Interceptor { chain: Interceptor.Chain ->
          val accessToken = tokenAuthenticator.getAccessToken()
          val request =
            chain.request().newBuilder().addHeader(AUTHORIZATION, "Bearer $accessToken").build()
          chain.proceed(request)
        }
      )
      .connectTimeout(TIMEOUT_DURATION, TimeUnit.SECONDS)
      .readTimeout(TIMEOUT_DURATION, TimeUnit.SECONDS)
      .callTimeout(TIMEOUT_DURATION, TimeUnit.SECONDS)
      .build()

  @Provides fun provideGson(): Gson = GsonBuilder().setLenient().create()

  @Provides fun provideParser(): IParser = FhirContext.forR4Cached().getCustomJsonParser()

  @Provides
  @AuthRetrofit
  @Singleton
  fun provideAuthRetrofit(
    @AuthOkHttpClientQualifier okHttpClient: OkHttpClient,
    configService: ConfigService,
    gson: Gson
  ): Retrofit =
    Retrofit.Builder()
      .baseUrl(configService.provideAuthConfiguration().oauthServerBaseUrl)
      .client(okHttpClient)
      .addConverterFactory(GsonConverterFactory.create(gson))
      .build()

  @Provides
  @Singleton
  fun provideKotlinJson() = Json {
    encodeDefaults = true
    ignoreUnknownKeys = true
    isLenient = true
    useAlternativeNames = true
  }

  @OptIn(ExperimentalSerializationApi::class)
  @Provides
  @KeycloakRetrofit
  @Singleton
  fun provideKeycloakRetrofit(
    @KeycloakOkHttpClientQualifier okHttpClient: OkHttpClient,
    configService: ConfigService,
    json: Json
  ): Retrofit =
    Retrofit.Builder()
      .baseUrl(configService.provideAuthConfiguration().oauthServerBaseUrl)
      .client(okHttpClient)
      .addConverterFactory(json.asConverterFactory(JSON_MEDIA_TYPE))
      .build()

  @Provides
  @RegularRetrofit
  @Singleton
  fun provideRegularRetrofit(
    @OkHttpClientQualifier okHttpClient: OkHttpClient,
    configService: ConfigService,
    gson: Gson,
    parser: IParser
  ): Retrofit =
    Retrofit.Builder()
      .baseUrl(configService.provideAuthConfiguration().fhirServerBaseUrl)
      .client(okHttpClient)
      .addConverterFactory(FhirConverterFactory(parser))
      .addConverterFactory(GsonConverterFactory.create(gson))
      .build()

  @Provides
  fun provideOauthService(
    @AuthRetrofit retrofit: Retrofit,
  ): OAuthService = retrofit.create(OAuthService::class.java)

  @Provides
  fun provideKeycloakService(@KeycloakRetrofit retrofit: Retrofit): KeycloakService =
    retrofit.create(KeycloakService::class.java)

  @Provides
  fun provideFhirResourceService(@RegularRetrofit retrofit: Retrofit): FhirResourceService =
    retrofit.create(FhirResourceService::class.java)

  companion object {
    const val TIMEOUT_DURATION = 120L
    const val AUTHORIZATION = "Authorization"
    const val COOKIE = "Cookie"
    val JSON_MEDIA_TYPE = "application/json".toMediaType()
  }
}
