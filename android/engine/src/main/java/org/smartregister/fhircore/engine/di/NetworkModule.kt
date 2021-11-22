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
import org.smartregister.fhircore.engine.data.remote.shared.interceptor.LoginInterceptor
import org.smartregister.fhircore.engine.data.remote.shared.interceptor.OAuthInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

@InstallIn(SingletonComponent::class)
@Module
class NetworkModule {

  @Provides fun provideGson(): Gson = GsonBuilder().setLenient().create()

  @Provides
  @AuthOkHttpClientQualifier
  fun provideAuthOkHttpClient(interceptor: LoginInterceptor) =
    OkHttpClient.Builder()
      .addInterceptor(interceptor)
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

  @Provides fun provideParser(): IParser = FhirContext.forR4().newJsonParser()

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
