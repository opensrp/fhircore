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

package org.smartregister.fhircore.auth.account

import android.accounts.Account
import android.accounts.AccountManager
import android.accounts.AccountManagerCallback
import android.accounts.NetworkErrorException
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo
import android.os.Build
import android.os.Bundle
import android.os.Handler
import io.jsonwebtoken.Jwts
import java.util.Date
import okhttp3.ResponseBody
import org.hl7.fhir.r4.test.utils.TestingUtilities.context
import org.smartregister.fhircore.BuildConfig
import org.smartregister.fhircore.api.OAuthService
import org.smartregister.fhircore.auth.OAuthResponse
import org.smartregister.fhircore.auth.secure.SecureConfig
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import timber.log.Timber

class AccountHelper(context: Context) {
  private val mContext = context

  fun getUserInfo(): Call<ResponseBody> {
    return OAuthService.create(mContext).userInfo()
  }

  fun refreshToken(refreshToken: String): OAuthResponse? {
    val data = buildOAuthPayload("refresh_token")

    data["refresh_token"] = refreshToken

    return OAuthService.create(mContext).fetchToken(data).execute().body()
  }

  @Throws(NetworkErrorException::class)
  fun fetchToken(username: String, password: CharArray): Call<OAuthResponse> {
    val data = buildOAuthPayload("password")

    data["username"] = username
    data["password"] = password.concatToString()

    return try {
      OAuthService.create(mContext).fetchToken(data)
    } catch (e: Exception) {
      throw NetworkErrorException(e)
    }
  }

  private fun buildOAuthPayload(grantType: String): MutableMap<String, String> {
    val payload = mutableMapOf<String, String>()

    payload["grant_type"] = grantType
    payload["client_id"] = BuildConfig.OAUTH_CIENT_ID
    payload["client_secret"] = BuildConfig.OAUTH_CLIENT_SECRET
    payload["scope"] = BuildConfig.OAUTH_SCOPE

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

    val account = Account(username, AccountConfig.ACCOUNT_TYPE)

    accountManager.addAccountExplicitly(account, refreshToken, null)
    accountManager.setAuthToken(account, AccountConfig.AUTH_TOKEN_TYPE, accessToken)
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
    val accounts = accountManager.getAccountsByType(AccountConfig.ACCOUNT_TYPE)

    if (accounts.isEmpty()) {
      return
    }

    val account = accounts.find { it.name.equals(username) } ?: return

    Timber.i("Got account %s : ", account.name)

    accountManager.getAuthToken(
      account,
      AccountConfig.AUTH_TOKEN_TYPE,
      Bundle(),
      true,
      callback,
      errorHandler
    )
  }

  fun logout(accountManager: AccountManager) {
    val account = accountManager.getAccountsByType(AccountConfig.ACCOUNT_TYPE).firstOrNull()

    val refreshToken = getRefreshToken(accountManager)
    if (refreshToken != null) {
      OAuthService.create(mContext)
        .logout(BuildConfig.OAUTH_CIENT_ID, BuildConfig.OAUTH_CLIENT_SECRET, refreshToken)
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
    val secureConfig = SecureConfig(mContext)
    secureConfig.deleteCredentials()

    val logoutIntent = getLogoutUserIntent()
    mContext.startActivity(logoutIntent)
    if (mContext is Activity) {
      mContext.finish()
    }
  }

  fun getLogoutUserIntent(): Intent {
    var intent = Intent(Intent.ACTION_MAIN)
    intent.addCategory(Intent.CATEGORY_LAUNCHER)
    intent.setPackage(mContext.packageName)

    // retrieve the main/launcher activity defined in the manifest and open it
    val activities: List<ResolveInfo> =
      mContext.getPackageManager().queryIntentActivities(intent, 0)
    if (activities.isNotEmpty()) {
      intent = intent.setClassName(mContext.packageName, activities[0].activityInfo.name)
      intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
      intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
    }
    return intent
  }

  fun getRefreshToken(accountManager: AccountManager): String? {
    val account = accountManager.getAccountsByType(AccountConfig.ACCOUNT_TYPE).firstOrNull()
    return accountManager.getPassword(account)
  }
}
