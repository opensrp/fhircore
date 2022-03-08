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

import android.accounts.AbstractAccountAuthenticator
import android.accounts.Account
import android.accounts.AccountAuthenticatorResponse
import android.accounts.AccountManager
import android.accounts.AccountManagerCallback
import android.accounts.NetworkErrorException
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.core.os.bundleOf
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.ResponseBody
import org.smartregister.fhircore.engine.configuration.app.ConfigService
import org.smartregister.fhircore.engine.data.remote.auth.OAuthService
import org.smartregister.fhircore.engine.data.remote.model.response.OAuthResponse
import org.smartregister.fhircore.engine.ui.appsetting.AppSettingActivity
import org.smartregister.fhircore.engine.ui.login.LoginActivity
import org.smartregister.fhircore.engine.util.SecureSharedPreference
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.toSha1
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import timber.log.Timber

@Singleton
class AccountAuthenticator
@Inject
constructor(
  @ApplicationContext val context: Context,
  val accountManager: AccountManager,
  val oAuthService: OAuthService,
  val configService: ConfigService,
  val secureSharedPreference: SecureSharedPreference,
  val tokenManagerService: TokenManagerService,
  val sharedPreference: SharedPreferencesHelper
) : AbstractAccountAuthenticator(context) {

  override fun addAccount(
    response: AccountAuthenticatorResponse,
    accountType: String,
    authTokenType: String?,
    requiredFeatures: Array<out String>?,
    options: Bundle
  ): Bundle {
    Timber.i("Adding account of type $accountType with auth token of type $authTokenType")

    val intent =
      Intent(context, getLoginActivityClass()).apply {
        putExtra(AccountManager.KEY_ACCOUNT_TYPE, getAccountType())
        putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response)
        putExtra(AUTH_TOKEN_TYPE, authTokenType)
        putExtra(IS_NEW_ACCOUNT, true)
      }

    return Bundle().apply { putParcelable(AccountManager.KEY_INTENT, intent) }
  }

  override fun getAuthToken(
    response: AccountAuthenticatorResponse,
    account: Account,
    authTokenType: String,
    options: Bundle?
  ): Bundle {
    var accessToken = tokenManagerService.getLocalSessionToken()

    Timber.i(
      "Access token for user ${account.name}, account type ${account.type}, token type $authTokenType is available:${accessToken?.isNotBlank()}"
    )

    if (accessToken.isNullOrBlank()) {
      // Use saved refresh token to try to get new access token. Logout user otherwise
      getRefreshToken()?.let {
        Timber.i("Saved active refresh token is available")

        runCatching {
          refreshToken(it)?.let { newTokenResponse ->
            accessToken = newTokenResponse.accessToken!!
            updateSession(newTokenResponse)
          }
        }
          .onFailure {
            Timber.e("Refresh token expired before it was used", it.stackTraceToString())
          }
          .onSuccess { Timber.i("Got new accessToken") }
      }
    }

    if (accessToken?.isNotBlank() == true) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        accountManager.notifyAccountAuthenticated(account)
      }

      return bundleOf(
        Pair(AccountManager.KEY_ACCOUNT_NAME, account.name),
        Pair(AccountManager.KEY_ACCOUNT_TYPE, account.type),
        Pair(AccountManager.KEY_AUTHTOKEN, accessToken)
      )
    }

    // failed to validate any token. now update credentials using auth activity
    return updateCredentials(response, account, authTokenType, options)
  }

  override fun editProperties(
    response: AccountAuthenticatorResponse?,
    accountType: String?
  ): Bundle = Bundle()

  override fun confirmCredentials(
    response: AccountAuthenticatorResponse,
    account: Account,
    options: Bundle?
  ): Bundle = bundleOf()

  override fun getAuthTokenLabel(authTokenType: String): String {
    return authTokenType.uppercase(Locale.ROOT)
  }

  override fun updateCredentials(
    response: AccountAuthenticatorResponse,
    account: Account,
    authTokenType: String?,
    options: Bundle?
  ): Bundle {
    Timber.i("Updating credentials for ${account.name} from auth activity")

    val intent =
      Intent(context, getLoginActivityClass()).apply {
        putExtra(AccountManager.KEY_ACCOUNT_TYPE, account.type)
        putExtra(AccountManager.KEY_ACCOUNT_NAME, account.name)
        putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response)
        putExtra(AUTH_TOKEN_TYPE, authTokenType)
      }
    return Bundle().apply { putParcelable(AccountManager.KEY_INTENT, intent) }
  }

  override fun hasFeatures(
    response: AccountAuthenticatorResponse,
    account: Account,
    features: Array<out String>
  ): Bundle {
    return bundleOf(Pair(AccountManager.KEY_BOOLEAN_RESULT, false))
  }

  fun getUserInfo(): Call<ResponseBody> = oAuthService.userInfo()

  fun refreshToken(refreshToken: String): OAuthResponse? {
    val data = buildOAuthPayload(REFRESH_TOKEN)
    data[REFRESH_TOKEN] = refreshToken
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
      oAuthService.fetchToken(data)
    } catch (exception: Exception) {
      throw NetworkErrorException(exception)
    }
  }

  private fun buildOAuthPayload(grantType: String) =
    mutableMapOf(
      GRANT_TYPE to grantType,
      CLIENT_ID to clientId(),
      CLIENT_SECRET to clientSecret(),
      SCOPE to providerScope()
    )

  fun getRefreshToken(): String? {
    Timber.v("Checking local storage for refresh token")
    val token = secureSharedPreference.retrieveCredentials()?.refreshToken
    return if (tokenManagerService.isTokenActive(token)) token else null
  }

  fun hasActiveSession(): Boolean {
    Timber.v("Checking for an active session")
    return tokenManagerService.getLocalSessionToken()?.isNotBlank() == true
  }

  fun hasActivePin(): Boolean {
    Timber.v("Checking for an active PIN")
    return secureSharedPreference.retrieveSessionPin()?.isNotBlank() == true
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

  fun loadActiveAccount(
    callback: AccountManagerCallback<Bundle>,
    errorHandler: Handler = Handler(Looper.getMainLooper(), DefaultErrorHandler)
  ) {
    tokenManagerService.getActiveAccount()?.let { loadAccount(it, callback, errorHandler) }
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
    val account = tokenManagerService.getActiveAccount()

    val refreshToken = getRefreshToken()
    if (refreshToken != null) {
      oAuthService
        .logout(clientId(), clientSecret(), refreshToken)
        .enqueue(
          object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
              accountManager.clearPassword(account)
              secureSharedPreference.deleteCredentials()
              launchScreen(AppSettingActivity::class.java)
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
              secureSharedPreference.deleteCredentials()
              launchScreen(AppSettingActivity::class.java)
            }
          }
        )
    } else {
      secureSharedPreference.deleteCredentials()
      launchScreen(AppSettingActivity::class.java)
    }
  }

  fun launchLoginScreen() {
    launchScreen(getLoginActivityClass())
  }

  fun launchScreen(clazz: Class<*>) {
    context.startActivity(
      Intent(Intent.ACTION_MAIN).apply {
        setClassName(context.packageName, clazz.name)
        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
        addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        addCategory(Intent.CATEGORY_LAUNCHER)
      }
    )
  }

  fun getLoginActivityClass(): Class<*> = LoginActivity::class.java

  fun getAccountType(): String = configService.provideAuthConfiguration().accountType

  fun clientSecret(): String = configService.provideAuthConfiguration().clientSecret

  fun clientId(): String = configService.provideAuthConfiguration().clientId

  fun providerScope(): String = configService.provideAuthConfiguration().scope

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
