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

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.parser.IParser
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.net.URL
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import org.smartregister.fhircore.engine.BuildConfig
import org.smartregister.fhircore.engine.configuration.app.ConfigService
import org.smartregister.fhircore.engine.data.remote.auth.KeycloakService
import org.smartregister.fhircore.engine.data.remote.auth.OAuthService
import org.smartregister.fhircore.engine.data.remote.fhir.resource.FhirConverterFactory
import org.smartregister.fhircore.engine.data.remote.fhir.resource.FhirResourceService
import org.smartregister.fhircore.engine.data.remote.shared.TokenAuthenticator
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.TimeZoneTypeAdapter
import org.smartregister.fhircore.engine.util.extension.getCustomJsonParser
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import timber.log.Timber

@InstallIn(SingletonComponent::class)
@Module
class NetworkModule {
  private var _isNonProxy = BuildConfig.IS_NON_PROXY_APK

  @Provides
  @NoAuthorizationOkHttpClientQualifier
  fun provideAuthOkHttpClient() =
    OkHttpClient.Builder()
      .addInterceptor(
        HttpLoggingInterceptor().apply {
          level =
            if (BuildConfig.DEBUG) {
              HttpLoggingInterceptor.Level.BODY
            } else {
              HttpLoggingInterceptor.Level.BASIC
            }
          redactHeader(AUTHORIZATION)
          redactHeader(COOKIE)
        },
      )
      .connectTimeout(TIMEOUT_DURATION, TimeUnit.SECONDS)
      .readTimeout(TIMEOUT_DURATION, TimeUnit.SECONDS)
      .callTimeout(TIMEOUT_DURATION, TimeUnit.SECONDS)
      .build()

  @Provides
  @WithAuthorizationOkHttpClientQualifier
  fun provideOkHttpClient(
    tokenAuthenticator: TokenAuthenticator,
    sharedPreferencesHelper: SharedPreferencesHelper,
    configService: ConfigService,
  ) =
    OkHttpClient.Builder()
      .addInterceptor(
        Interceptor { chain: Interceptor.Chain ->
          try {
            var request = chain.request()
            val requestPath = request.url.encodedPath.substring(1)
            val resourcePath = if (!_isNonProxy) requestPath.replace("fhir/", "") else requestPath
            val host = URL(configService.provideAuthConfiguration().fhirServerBaseUrl).host

            if (request.url.host == host && CUSTOM_ENDPOINTS.contains(resourcePath)) {
              val newUrl = request.url.newBuilder().encodedPath("/$resourcePath").build()
              request = request.newBuilder().url(newUrl).build()
            }

            chain.proceed(request)
          } catch (e: Exception) {
            Timber.e(e)
            Response.Builder()
              .request(chain.request())
              .protocol(Protocol.HTTP_1_1)
              .code(901)
              .message(e.message ?: "Failed to overwrite URL request successfully")
              .body("{$e}".toResponseBody(null))
              .build()
          }
        },
      )
      .addInterceptor(
        Interceptor { chain: Interceptor.Chain ->
          try {
            val accessToken = tokenAuthenticator.getAccessToken()
            // NB: Build new request before setting Auth header; otherwise the header will be
            // bypassed
            val request = chain.request().newBuilder()
            if (accessToken.isNotEmpty()) {
              request.addHeader(AUTHORIZATION, "Bearer $accessToken")
              sharedPreferencesHelper.retrieveApplicationId()?.let {
                request.addHeader(APPLICATION_ID, it)
              }
            }
            chain.proceed(request.build())
          } catch (e: Exception) {
            Timber.e(e)
            Response.Builder()
              .request(chain.request())
              .protocol(Protocol.HTTP_1_1)
              .code(900)
              .message(e.message ?: "Failed to complete request successfully")
              .body("{$e}".toResponseBody(null))
              .build()
          }
        },
      )
      .addInterceptor(
        HttpLoggingInterceptor().apply {
          level =
            if (BuildConfig.DEBUG) {
              HttpLoggingInterceptor.Level.BODY
            } else {
              HttpLoggingInterceptor.Level.BASIC
            }
          redactHeader(AUTHORIZATION)
          redactHeader(COOKIE)
        },
      )
      .connectTimeout(TIMEOUT_DURATION, TimeUnit.SECONDS)
      .readTimeout(TIMEOUT_DURATION, TimeUnit.SECONDS)
      .callTimeout(TIMEOUT_DURATION, TimeUnit.SECONDS)
      .retryOnConnectionFailure(false) // Avoid silent retries sometimes before token is provided
      .build()

  @Provides
  fun provideGson(): Gson =
    GsonBuilder()
      .setLenient()
      .registerTypeAdapter(TimeZone::class.java, TimeZoneTypeAdapter().nullSafe())
      .create()

  @Provides fun provideParser(): IParser = FhirContext.forR4Cached().getCustomJsonParser()

  @Provides
  @Singleton
  fun provideKotlinJson() = Json {
    encodeDefaults = true
    ignoreUnknownKeys = true
    isLenient = true
    useAlternativeNames = true
  }

  @Provides
  @AuthenticationRetrofit
  fun provideAuthRetrofit(
    @NoAuthorizationOkHttpClientQualifier okHttpClient: OkHttpClient,
    configService: ConfigService,
    gson: Gson,
  ): Retrofit =
    Retrofit.Builder()
      .baseUrl(configService.provideAuthConfiguration().oauthServerBaseUrl)
      .client(okHttpClient)
      .addConverterFactory(GsonConverterFactory.create(gson))
      .build()

  @OptIn(ExperimentalSerializationApi::class)
  @Provides
  @KeycloakRetrofit
  fun provideKeycloakRetrofit(
    @WithAuthorizationOkHttpClientQualifier okHttpClient: OkHttpClient,
    configService: ConfigService,
    json: Json,
  ): Retrofit =
    Retrofit.Builder()
      .baseUrl(configService.provideAuthConfiguration().oauthServerBaseUrl)
      .client(okHttpClient)
      .addConverterFactory(json.asConverterFactory(JSON_MEDIA_TYPE))
      .build()

  @Provides
  @RegularRetrofit
  fun provideRegularRetrofit(
    @WithAuthorizationOkHttpClientQualifier okHttpClient: OkHttpClient,
    configService: ConfigService,
    gson: Gson,
    parser: IParser,
  ): Retrofit =
    Retrofit.Builder()
      .baseUrl(configService.provideAuthConfiguration().fhirServerBaseUrl)
      .client(okHttpClient)
      .addConverterFactory(FhirConverterFactory(parser))
      .addConverterFactory(GsonConverterFactory.create(gson))
      .build()

  @Provides
  fun provideOauthService(
    @AuthenticationRetrofit retrofit: Retrofit,
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
    const val APPLICATION_ID = "App-Id"
    const val COOKIE = "Cookie"
    val JSON_MEDIA_TYPE = "application/json".toMediaType()
    val CUSTOM_ENDPOINTS = listOf("PractitionerDetail", "LocationHierarchy")
  }
}
