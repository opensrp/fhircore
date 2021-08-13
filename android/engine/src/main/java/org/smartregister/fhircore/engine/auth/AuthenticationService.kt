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
import io.jsonwebtoken.Jwts
import java.util.Date
import okhttp3.ResponseBody
import org.smartregister.fhircore.engine.configuration.app.ApplicationConfiguration
import org.smartregister.fhircore.engine.data.model.response.OAuthResponse
import org.smartregister.fhircore.engine.data.remote.auth.OAuthService
import org.smartregister.fhircore.engine.util.SecureSharedPreference
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import timber.log.Timber

abstract class AuthenticationService(open val context: Context) {

  fun getUserInfo(): Call<ResponseBody> =
      OAuthService.create(context, getApplicationConfigurations()).userInfo()

  fun refreshToken(refreshToken: String): OAuthResponse? {
    val data = buildOAuthPayload(REFRESH_TOKEN)
    data[REFRESH_TOKEN] = refreshToken
    return OAuthService.create(context, getApplicationConfigurations())
        .fetchToken(data)
        .execute()
        .body()
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

  fun isSessionActive(token: String?): Boolean {
    if (token.isNullOrEmpty()) return false
    kotlin
        .runCatching {
          val tokenOnly = token.substring(0, token.lastIndexOf('.') + 1)
          return Jwts.parser().parseClaimsJwt(tokenOnly).body.expiration.after(Date())
        }
        .onFailure { Timber.e(it) }

    return false
  }

  fun addAuthenticatedAccount(
      accountManager: AccountManager,
      successResponse: Response<OAuthResponse>,
      username: String
  ) {
    Timber.i("Adding authenticated account %s", username)

    val accessToken = successResponse.body()!!.accessToken!!
    val refreshToken = successResponse.body()!!.refreshToken!!

    val account = Account(username, getAccountType())

    accountManager.addAccountExplicitly(account, refreshToken, null)
    accountManager.setAuthToken(account, AUTH_TOKEN_TYPE, accessToken)
    accountManager.setPassword(account, refreshToken)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      accountManager.notifyAccountAuthenticated(account)
    }
  }

  fun loadAccount(
      accountManager: AccountManager,
      username: String?,
      callback: AccountManagerCallback<Bundle>,
      errorHandler: Handler
  ) {
    val accounts = accountManager.getAccountsByType(getAccountType())

    if (accounts.isEmpty()) return

    val account = accounts.find { it.name.equals(username) } ?: return
    Timber.i("Got account %s : ", account.name)
    accountManager.getAuthToken(account, AUTH_TOKEN_TYPE, Bundle(), true, callback, errorHandler)
  }

  fun logout(accountManager: AccountManager) {
    val account = accountManager.getAccountsByType(getAccountType()).firstOrNull()

    val refreshToken = getRefreshToken(accountManager)
    if (refreshToken != null) {
      OAuthService.create(context, getApplicationConfigurations())
          .logout(clientId(), clientSecret(), refreshToken)
          .enqueue(
              object : Callback<ResponseBody> {
                override fun onResponse(
                    call: Call<ResponseBody>,
                    response: Response<ResponseBody>
                ) {
                  accountManager.clearPassword(account)
                  cleanup()
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                  cleanup()
                }
              })
    } else {
      cleanup()
    }
  }

  fun cleanup() {
    SecureSharedPreference(context).deleteCredentials()
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

  private fun getRefreshToken(accountManager: AccountManager): String? {
    val account = accountManager.getAccountsByType(getAccountType()).firstOrNull()
    return accountManager.getPassword(account)
  }

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
