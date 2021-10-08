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

package org.smartregister.fhircore.engine.auth

import android.accounts.Account
import android.accounts.AccountManager
import android.accounts.AccountManagerCallback
import android.accounts.NetworkErrorException
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.MalformedJwtException
import io.jsonwebtoken.UnsupportedJwtException
import java.util.Date
import okhttp3.ResponseBody
import org.smartregister.fhircore.engine.configuration.app.ApplicationConfiguration
import org.smartregister.fhircore.engine.data.remote.auth.OAuthService
import org.smartregister.fhircore.engine.data.remote.model.response.OAuthResponse
import org.smartregister.fhircore.engine.util.SecureSharedPreference
import org.smartregister.fhircore.engine.util.toSha1
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import timber.log.Timber

abstract class AuthenticationService(open val context: Context) {
  val secureSharedPreference by lazy { SecureSharedPreference(context) }
  val accountManager by lazy { AccountManager.get(context) }

  fun getUserInfo(): Call<ResponseBody> =
    OAuthService.create(context, getApplicationConfigurations()).userInfo()

  fun refreshToken(refreshToken: String): OAuthResponse? {
    val data = buildOAuthPayload(REFRESH_TOKEN)
    data[REFRESH_TOKEN] = refreshToken
    val oAuthService = OAuthService.create(context, getApplicationConfigurations())
    return try {
      oAuthService.fetchToken(data).execute().body()
    } catch (exception: Exception) {
      Timber.e("Failed to refresh token, refresh token may have expired", exception)
      return null
    }
  }

  @Throws(NetworkErrorException::class)
  fun fetchToken(username: String, password: CharArray): Call<OAuthResponse> {
    val data = buildOAuthPayload(PASSWORD)
    data[USERNAME] = username
    data[PASSWORD] = password.concatToString()
    return try {
      OAuthService.create(context, getApplicationConfigurations()).fetchToken(data)
    } catch (e: Exception) {
      throw NetworkErrorException(e)
    }
  }

  private fun buildOAuthPayload(grantType: String): MutableMap<String, String> {
    val payload = mutableMapOf<String, String>()
    payload[GRANT_TYPE] = grantType
    payload[CLIENT_ID] = clientId()
    payload[CLIENT_SECRET] = clientSecret()
    payload[SCOPE] = providerScope()
    return payload
  }

  fun isTokenActive(token: String?): Boolean {
    if (token.isNullOrEmpty()) return false
    return try {
      val tokenOnly = token.substring(0, token.lastIndexOf('.') + 1)
      Jwts.parser().parseClaimsJwt(tokenOnly).body.expiration.after(Date())
    } catch (expiredJwtException: ExpiredJwtException) {
      Timber.w("Token is expired", expiredJwtException)
      false
    } catch (unsupportedJwtException: UnsupportedJwtException) {
      Timber.w("JWT format not recognized", unsupportedJwtException)
      false
    } catch (malformedJwtException: MalformedJwtException) {
      Timber.w(malformedJwtException)
      false
    }
  }

  fun getLocalSessionToken(): String? {
    Timber.v("Checking local storage for access token")
    val token = secureSharedPreference.retrieveSessionToken()
    return if (isTokenActive(token)) token else null
  }

  fun getRefreshToken(): String? {
    Timber.v("Checking local storage for refresh token")
    val token = secureSharedPreference.retrieveCredentials()?.refreshToken
    return if (isTokenActive(token)) token else null
  }

  fun hasActiveSession(): Boolean {
    Timber.v("Checking for an active session")
    return getLocalSessionToken()?.isNotBlank() == true
  }

  fun validLocalCredentials(username: String, password: CharArray): Boolean {
    Timber.v("Validating credentials with local storage")
    return secureSharedPreference.retrieveCredentials()?.let {
      it.username.contentEquals(username) &&
        it.password.contentEquals(password.concatToString().toSha1())
    }
      ?: false
  }

  fun updateSession(successResponse: OAuthResponse) {
    Timber.v("Updating tokens on local storage")
    val credentials =
      secureSharedPreference.retrieveCredentials()!!.apply {
        this.sessionToken = successResponse.accessToken!!
        this.refreshToken = successResponse.refreshToken!!
      }
    secureSharedPreference.saveCredentials(credentials)
  }

  fun addAuthenticatedAccount(
    successResponse: Response<OAuthResponse>,
    username: String,
    password: CharArray
  ) {
    Timber.i("Adding authenticated account %s of type %s", username, getAccountType())

    val accessToken = successResponse.body()!!.accessToken!!
    val refreshToken = successResponse.body()!!.refreshToken!!

    val account = Account(username, getAccountType())

    accountManager.addAccountExplicitly(account, null, null)
    secureSharedPreference.saveCredentials(
      AuthCredentials(username, password.concatToString().toSha1(), accessToken, refreshToken)
    )

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      accountManager.notifyAccountAuthenticated(account)
    }
  }

  fun getBlockingActiveAuthToken(): String? {
    getLocalSessionToken()?.let {
      return it
    }
    Timber.v("Trying to get blocking auth token from account manager")
    return getActiveAccount()?.let {
      accountManager.blockingGetAuthToken(it, AUTH_TOKEN_TYPE, false)
    }
  }

  fun getActiveAccount(): Account? {
    Timber.v("Checking for an active account stored")
    return secureSharedPreference.retrieveSessionUsername()?.let { username ->
      accountManager.getAccountsByType(getAccountType()).find { it.name.equals(username) }
    }
  }

  fun loadActiveAccount(
    callback: AccountManagerCallback<Bundle>,
    errorHandler: Handler = Handler(Looper.getMainLooper(), DefaultErrorHandler)
  ) {
    getActiveAccount()?.let { loadAccount(it, callback, errorHandler) }
  }

  fun loadAccount(
    account: Account,
    callback: AccountManagerCallback<Bundle>,
    errorHandler: Handler = Handler(Looper.getMainLooper(), DefaultErrorHandler)
  ) {
    Timber.i("Trying to load from getAuthToken for account %s", account.name)
    accountManager.getAuthToken(account, AUTH_TOKEN_TYPE, Bundle(), false, callback, errorHandler)
  }

  fun logout() {
    val account = getActiveAccount()

    val refreshToken = getRefreshToken()
    if (refreshToken != null) {
      OAuthService.create(context, getApplicationConfigurations())
        .logout(clientId(), clientSecret(), refreshToken)
        .enqueue(
          object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
              accountManager.clearPassword(account)
              cleanup()
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
              cleanup()
            }
          }
        )
    } else {
      cleanup()
    }
  }

  fun cleanup() {
    secureSharedPreference.deleteCredentials()
    context.startActivity(getLogoutUserIntent())
    if (context is Activity) (context as Activity).finish()
  }

  private fun getLogoutUserIntent(): Intent {
    var intent = Intent(Intent.ACTION_MAIN)
    intent.addCategory(Intent.CATEGORY_LAUNCHER)
    intent = intent.setClassName(context.packageName, getLoginActivityClass().name)
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

    return intent
  }

  abstract fun skipLogin(): Boolean

  abstract fun getLoginActivityClass(): Class<*>

  abstract fun getAccountType(): String

  abstract fun clientSecret(): String

  abstract fun clientId(): String

  abstract fun providerScope(): String

  abstract fun getApplicationConfigurations(): ApplicationConfiguration

  companion object {
    const val AUTH_TOKEN_TYPE = "AUTH_TOKEN_TYPE"
    const val IS_NEW_ACCOUNT = "IS_NEW_ACCOUNT"
    const val GRANT_TYPE = "grant_type"
    const val CLIENT_ID = "client_id"
    const val CLIENT_SECRET = "client_secret"
    const val SCOPE = "scope"
    const val USERNAME = "username"
    const val PASSWORD = "password"
    const val REFRESH_TOKEN = "refresh_token"
  }
}
