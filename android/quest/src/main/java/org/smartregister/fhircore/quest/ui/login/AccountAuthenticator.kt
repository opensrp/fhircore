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

package org.smartregister.fhircore.quest.ui.login

import android.accounts.AbstractAccountAuthenticator
import android.accounts.Account
import android.accounts.AccountAuthenticatorResponse
import android.accounts.AccountManager
import android.accounts.NetworkErrorException
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.core.os.bundleOf
import ca.uhn.fhir.parser.IParser
import dagger.hilt.android.qualifiers.ApplicationContext
import java.net.UnknownHostException
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.auth.AuthCredentials
import org.smartregister.fhircore.engine.auth.TokenManagerService
import org.smartregister.fhircore.engine.auth.TokenManagerService.Companion.AUTH_TOKEN_TYPE
import org.smartregister.fhircore.engine.configuration.app.ConfigService
import org.smartregister.fhircore.engine.data.remote.auth.OAuthService
import org.smartregister.fhircore.engine.data.remote.fhir.resource.FhirResourceService
import org.smartregister.fhircore.engine.data.remote.model.response.OAuthResponse
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.SecureSharedPreference
import org.smartregister.fhircore.engine.util.extension.practitionerEndpointUrl
import org.smartregister.fhircore.engine.util.extension.showToast
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
  val fhirResourceService: FhirResourceService,
  val parser: IParser,
  val configService: ConfigService,
  val secureSharedPreference: SecureSharedPreference,
  val tokenManagerService: TokenManagerService,
  val dispatcherProvider: DispatcherProvider
) : AbstractAccountAuthenticator(context) {

  private val authConfiguration by lazy { configService.provideAuthConfiguration() }

  override fun addAccount(
    response: AccountAuthenticatorResponse,
    accountType: String,
    authTokenType: String?,
    requiredFeatures: Array<out String>?,
    options: Bundle
  ): Bundle {
    Timber.i("Adding account of type $accountType with auth token of type $authTokenType")

    val intent =
      Intent(context, LoginActivity::class.java).apply {
        putExtra(AccountManager.KEY_ACCOUNT_TYPE, accountType)
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
    var accessToken = tokenManagerService.getActiveAuthToken()
    Timber.i("Token available for user ${account.name}, account type ${account.type}")

    if (accessToken.isNullOrBlank()) {
      getRefreshToken()?.let {
        runCatching { refreshToken(it) }
          .onFailure {
            Timber.e("Refresh token expired before it was used", it.stackTraceToString())
          }
          .onSuccess { oAuthResponse ->
            Timber.i("Got new accessToken")
            if (oAuthResponse != null) {
              accessToken = oAuthResponse.accessToken
              updateSession(oAuthResponse)
            }
          }
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
  ): Bundle = bundleOf()

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
      Intent(context, LoginActivity::class.java).apply {
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
    return oAuthService.fetchToken(data).execute().body()
  }

  suspend fun getPractitionerDetails(keycloakUuid: String): org.hl7.fhir.r4.model.Bundle {
    return fhirResourceService.getResource(url = keycloakUuid.practitionerEndpointUrl())
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
      CLIENT_ID to authConfiguration.clientId,
      CLIENT_SECRET to authConfiguration.clientSecret,
      SCOPE to authConfiguration.scope
    )

  fun getRefreshToken(): String? = secureSharedPreference.retrieveCredentials()?.refreshToken

  fun hasActiveSession() = tokenManagerService.getActiveAuthToken()?.isNotEmpty() == true

  fun hasActivePin() = secureSharedPreference.retrieveSessionPin()?.isNotEmpty() == true

  fun validateLocalCredentials(username: String, password: CharArray): Boolean {
    Timber.v("Validating credentials")
    return if (accountExists(username)) {
      val credentials = secureSharedPreference.retrieveCredentials()
      credentials?.username.contentEquals(username, true) &&
        credentials?.password.contentEquals(password.concatToString().toSha1())
    } else false
  }

  fun updateSession(successResponse: OAuthResponse) {
    secureSharedPreference
      .retrieveCredentials()
      ?.apply {
        this.sessionToken = successResponse.accessToken
        this.refreshToken = successResponse.refreshToken
      }
      ?.run { secureSharedPreference.saveCredentials(this) }
  }

  fun addAuthenticatedAccount(
    successResponse: Response<OAuthResponse>,
    username: String,
    password: CharArray
  ) {
    Timber.i("Adding authenticated account %s of type %s", username, authConfiguration.accountType)

    val accessToken = successResponse.body()?.accessToken
    val refreshToken = successResponse.body()?.refreshToken
    val account = Account(username, authConfiguration.accountType)

    accountManager.addAccountExplicitly(account, null, null)
    secureSharedPreference.saveCredentials(
      AuthCredentials(username, password.concatToString().toSha1(), accessToken, refreshToken)
    )

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      accountManager.notifyAccountAuthenticated(account)
    }
  }

  fun logout(onLogout: () -> Unit = {}) {
    val refreshToken = getRefreshToken()
    if (refreshToken == null) {
      onLogout()
      Timber.w("Refresh token is null. User already logged out or device is offline.")
      launchScreen(LoginActivity::class.java)
    } else {
      val logoutService: Call<ResponseBody> =
        oAuthService.logout(
          clientId = authConfiguration.clientId,
          clientSecret = authConfiguration.clientSecret,
          refreshToken = refreshToken
        )
      logoutService.enqueue(
        object : Callback<ResponseBody> {
          override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
            if (response.isSuccessful) {
              onLogout()
              invalidateSession()
            } else {
              onLogout()
              Timber.w(response.body()?.string())
              context.showToast(context.getString(R.string.cannot_logout_user))
            }
            launchScreen(LoginActivity::class.java)
          }

          override fun onFailure(call: Call<ResponseBody>, throwable: Throwable) {
            onLogout()
            Timber.w(throwable)
            context.showToast(
              context.getString(R.string.error_logging_out, throwable.localizedMessage)
            )
            launchScreen(LoginActivity::class.java)
          }
        }
      )
    }
  }

  suspend fun refreshSessionAuthToken(): Bundle {
    return withContext(dispatcherProvider.io()) {
      val bundle = Bundle()
      val activeAccount = getActiveAccount()
      if (activeAccount != null) {
        var authToken = tokenManagerService.getActiveAuthToken()
        if (authToken.isNullOrEmpty()) {
          getRefreshToken()?.let { theRefreshToken ->
            runCatching { refreshToken(theRefreshToken) }
              .onFailure { throwable ->
                if (throwable is UnknownHostException) {
                  bundle.putString(
                    AccountManager.KEY_ERROR_MESSAGE,
                    context.getString(R.string.refresh_token_fail_error_message)
                  )
                }
                bundle.putInt(
                  AccountManager.KEY_ERROR_CODE,
                  AccountManager.ERROR_CODE_NETWORK_ERROR
                )
                Timber.e("Failed to get the refresh token", throwable)
                authToken = null
              }
              .onSuccess { oAuthResponse ->
                Timber.i("Received new access token")
                if (oAuthResponse != null) {
                  authToken = oAuthResponse.accessToken
                  updateSession(oAuthResponse)
                  accountManager.setAuthToken(
                    activeAccount,
                    authConfiguration.accountType,
                    authToken
                  )
                }
              }
          }
        }
        bundle.putString(AccountManager.KEY_ACCOUNT_NAME, activeAccount.name)
        bundle.putString(AccountManager.KEY_ACCOUNT_TYPE, activeAccount.type)
        if (!authToken.isNullOrEmpty()) {
          bundle.putString(AccountManager.KEY_AUTHTOKEN, authToken)
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            accountManager.notifyAccountAuthenticated(activeAccount)
          }
        }
      }
      bundle
    }
  }

  fun invalidateSession() {
    accountManager.invalidateAuthToken(
      authConfiguration.accountType,
      secureSharedPreference.retrieveSessionToken()
    )
    secureSharedPreference.deleteSessionTokens()
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

  fun validatePreviousLogin(username: String): Boolean {
    if (secureSharedPreference.retrieveCredentials() == null) return true
    return accountExists(username)
  }

  private fun accountExists(username: String) =
    accountManager.accounts.find {
      it.name.equals(username, true) && it.type == authConfiguration.accountType
    } != null && secureSharedPreference.retrieveCredentials()?.username.equals(username, true)

  private fun getActiveAccount(): Account? {
    Timber.v("Checking for an active account stored")
    return secureSharedPreference.retrieveSessionUsername()?.let { username ->
      accountManager.getAccountsByType(configService.provideAuthConfiguration().accountType).find {
        it.name.equals(username)
      }
    }
  }
  companion object {
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
