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

package org.smartregister.fhircore.engine.data.remote.auth

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import org.smartregister.fhircore.engine.configuration.app.ApplicationConfiguration
import org.smartregister.fhircore.engine.data.model.response.OAuthResponse
import org.smartregister.fhircore.engine.data.remote.shared.interceptor.OAuthInterceptor
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Field
import retrofit2.http.FieldMap
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import timber.log.Timber

interface OAuthService {

  @FormUrlEncoded
  @POST("protocol/openid-connect/token")
  fun fetchToken(@FieldMap(encoded = false) body: Map<String, String>): Call<OAuthResponse>

  @GET("protocol/openid-connect/userinfo") fun userInfo(): Call<ResponseBody>

  @FormUrlEncoded
  @POST("protocol/openid-connect/logout")
  fun logout(
    @Field("client_id") clientId: String,
    @Field("client_secret") clientSecret: String,
    @Field("refresh_token") refreshToken: String
  ): Call<ResponseBody>

  companion object {
    fun create(context: Context, applicationConfiguration: ApplicationConfiguration): OAuthService {
      val logger = HttpLoggingInterceptor()
      logger.level = HttpLoggingInterceptor.Level.BODY

      Timber.i(applicationConfiguration.oauthServerBaseUrl)

      val client =
        OkHttpClient.Builder()
          .addInterceptor(OAuthInterceptor(context))
          .addInterceptor(logger)
          .build()

      return Retrofit.Builder()
        .baseUrl(applicationConfiguration.oauthServerBaseUrl)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(OAuthService::class.java)
    }
  }
}
