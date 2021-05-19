package org.smartregister.fhircore.auth.account

import android.accounts.NetworkErrorException
import org.smartregister.fhircore.BuildConfig
import org.smartregister.fhircore.api.OauthService
import retrofit2.Call
import retrofit2.HttpException


class AccountHelper {

    fun refreshToken(refreshToken: String): OauthResponse {
        val data: MutableMap<String, String> = HashMap()
        data["refresh_token"] = refreshToken
        data["grant_type"] = "refresh_token"

        return OauthService.create()!!.refreshToken(data)
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