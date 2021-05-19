package org.smartregister.fhircore.auth.account

import android.accounts.Account
import android.accounts.NetworkErrorException
import okhttp3.internal.format
import org.json.JSONObject
import org.smartregister.fhircore.BuildConfig
import org.smartregister.fhircore.api.OauthService
import retrofit2.Call
import retrofit2.HttpException
import retrofit2.Retrofit
import java.io.UnsupportedEncodingException
import java.net.URL
import java.net.URLEncoder


class AccountHelper {

    @Throws(NetworkErrorException::class)
    fun refreshToken(refreshToken: String): OauthResponse? {
        val data: MutableMap<String, String> = HashMap()
        data["refresh_token"] = refreshToken
        data["grant_type"] = "refresh_token"
        return try {
            return OauthService.create()!!.refreshToken(data).execute().body()
        } catch (e: HttpException) {
            throw e
        } catch (e: Exception) {
            throw NetworkErrorException(e)
        }
    }

    @Throws(NetworkErrorException::class)
    fun fetchToken(username: String, password: CharArray): Call<OauthResponse> {
        val data: MutableMap<String, String> = HashMap()
        data["grant_type"] = "password"
        data["username"] = username
        data["password"] = password.concatToString()
        data["client_id"] = BuildConfig.OAUTH_CIENT_ID
        data["client_secret"] = BuildConfig.OAUTH_CLIENT_SECRET
        return try {
            return OauthService.create()!!.fetchToken(data)
        } catch (e: HttpException) {
            throw e
        } catch (e: Exception) {
            throw NetworkErrorException(e)
        }
    }
}