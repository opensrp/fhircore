package org.smartregister.fhircore.api

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.smartregister.fhircore.BuildConfig
import org.smartregister.fhircore.auth.account.OauthResponse
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.FieldMap
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface OauthService {

    @FormUrlEncoded
    @POST("protocol/openid-connect/token/refresh")
    fun refreshToken(@FieldMap(encoded = false) body: Map<String, String>): OauthResponse

    @FormUrlEncoded
    @POST("protocol/openid-connect/token")
    fun fetchToken(@FieldMap(encoded = false) body: Map<String, String>): Call<OauthResponse>

    companion object {
        fun create(): OauthService? {
            val logger = HttpLoggingInterceptor()
            logger.level = HttpLoggingInterceptor.Level.BODY

            Log.i(javaClass.name, BuildConfig.OAUTH_BASE_URL)

            val client = OkHttpClient.Builder().addInterceptor(logger).build()
            return Retrofit.Builder()
                .baseUrl(BuildConfig.OAUTH_BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(OauthService::class.java)
        }
    }

}