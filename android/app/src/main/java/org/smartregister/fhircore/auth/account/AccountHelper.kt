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
            val bodyString: String = getBody(data)

            return OauthService.create()?.refreshToken(bodyString)?.execute()?.body()
        } catch (e: HttpException) {
            throw e
        } catch (e: Exception) {
            throw NetworkErrorException(e)
        }
    }

    private fun getBody(data: Map<String, String>): String {
        val bodyBuilder = StringBuilder()
        val formTemplate = "%s=%s"
        var amp = ""
        for ((key, value) in data) {
            bodyBuilder.append(amp)
            try {
                bodyBuilder.append(
                    format(
                        formTemplate,
                        key, URLEncoder.encode(value, "UTF-8")
                    )
                )
            } catch (e: UnsupportedEncodingException) {
                throw RuntimeException(e)
            }
            amp = "&"
        }
        return bodyBuilder.toString()
    }
}