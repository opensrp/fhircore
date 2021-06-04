package org.smartregister.fhircore.api

import android.content.Context
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import org.smartregister.fhircore.BuildConfig
import org.smartregister.fhircore.auth.OAuthResponse
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.FieldMap
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST

interface OAuthService {

  @FormUrlEncoded
  @POST("protocol/openid-connect/token")
  fun fetchToken(@FieldMap(encoded = false) body: Map<String, String>): Call<OAuthResponse>

  @GET("protocol/openid-connect/userinfo") fun userInfo(): Call<ResponseBody>

  companion object {
    fun create(context: Context): OAuthService {
      val logger = HttpLoggingInterceptor()
      logger.level = HttpLoggingInterceptor.Level.BODY

      Log.i(javaClass.name, BuildConfig.OAUTH_BASE_URL)

      val client =
        OkHttpClient.Builder()
          .addInterceptor(OAuthInterceptor(context))
          .addInterceptor(logger)
          .build()

      return Retrofit.Builder()
        .baseUrl(BuildConfig.OAUTH_BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(OAuthService::class.java)
    }
  }
}
