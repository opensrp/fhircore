package org.smartregister.fhircore.api

import ca.uhn.fhir.parser.IParser
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.logging.HttpLoggingInterceptor
import org.smartregister.fhircore.BuildConfig
import org.smartregister.fhircore.auth.account.OauthResponse
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

interface OauthService {

    @POST("/protocol/openid-connect/token/refresh")
    fun refreshToken(@Body body: String): Call<OauthResponse>

    companion object {
        fun create(): OauthService? {
            val logger = HttpLoggingInterceptor()
            logger.level = HttpLoggingInterceptor.Level.BODY

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