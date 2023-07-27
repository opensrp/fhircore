/*
 * Copyright 2021-2023 Ona Systems, Inc
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

package org.smartregister.fhircore.engine.data.remote.shared

import android.accounts.Account
import android.accounts.AccountManager
import android.accounts.AccountManagerFuture
import android.accounts.AuthenticatorException
import android.accounts.OperationCanceledException
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import androidx.core.os.bundleOf
import com.google.android.fhir.sync.HttpAuthenticationMethod
import com.google.android.fhir.sync.HttpAuthenticator as FhirAuthenticator
import dagger.hilt.android.qualifiers.ApplicationContext
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import java.io.IOException
import java.net.UnknownHostException
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton
import javax.net.ssl.SSLHandshakeException
import kotlinx.coroutines.runBlocking
import org.smartregister.fhircore.engine.configuration.app.ConfigService
import org.smartregister.fhircore.engine.data.remote.auth.OAuthService
import org.smartregister.fhircore.engine.data.remote.model.response.OAuthResponse
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.SecureSharedPreference
import org.smartregister.fhircore.engine.util.extension.today
import org.smartregister.fhircore.engine.util.toPasswordHash
import retrofit2.HttpException
import timber.log.Timber

@Singleton
class TokenAuthenticator
@Inject
constructor(
  val secureSharedPreference: SecureSharedPreference,
  val configService: ConfigService,
  val oAuthService: OAuthService,
  val dispatcherProvider: DispatcherProvider,
  val accountManager: AccountManager,
  @ApplicationContext val context: Context,
) : FhirAuthenticator {

  private val jwtParser = Jwts.parser()
  private val authConfiguration by lazy { configService.provideAuthConfiguration() }
  private var isLoginPageRendered = false

  fun getAccessToken(): String {
    val account = findAccount()
    return if (account != null) {
      val accessToken = accountManager.peekAuthToken(account, AUTH_TOKEN_TYPE) ?: ""
      if (!isTokenActive(accessToken)) {
        accountManager.run {
          invalidateAuthToken(account.type, accessToken)
          try {
            getAuthToken(
              account,
              AUTH_TOKEN_TYPE,
              bundleOf(),
              true,
              handleAccountManagerFutureCallback(account),
              Handler(Looper.getMainLooper()) { message: Message ->
                Timber.e(message.toString())
                true
              },
            )
          } catch (operationCanceledException: OperationCanceledException) {
            Timber.e(operationCanceledException)
          } catch (ioException: IOException) {
            Timber.e(ioException)
          } catch (authenticatorException: AuthenticatorException) {
            Timber.e(authenticatorException)
            // TODO: Should we cancel the sync job to avoid retries when offline?
          }
        }
      } else {
        isLoginPageRendered = false
      }
      accessToken
    } else {
      ""
    }
  }

  private fun AccountManager.handleAccountManagerFutureCallback(account: Account?) =
    { result: AccountManagerFuture<Bundle> ->
      val bundle = result.result
      when {
        bundle.containsKey(AccountManager.KEY_AUTHTOKEN) -> {
          val token = bundle.getString(AccountManager.KEY_AUTHTOKEN)
          setAuthToken(account, AUTH_TOKEN_TYPE, token)
        }
        bundle.containsKey(AccountManager.KEY_INTENT) -> {
          val launchIntent = bundle.get(AccountManager.KEY_INTENT) as? Intent

          // Deletes session PIN to allow reset
          secureSharedPreference.deleteSessionPin()

          if (launchIntent != null && !isLoginPageRendered) {
            context.startActivity(launchIntent.putExtra(CANCEL_BACKGROUND_SYNC, true))
            isLoginPageRendered = true
          }
        }
      }
    }

  /** This function checks if token is null or empty or expired */
  fun isTokenActive(authToken: String?): Boolean {
    if (authToken.isNullOrEmpty()) return false
    val tokenPart = authToken.substringBeforeLast('.').plus(".")
    return try {
      val body = jwtParser.parseClaimsJwt(tokenPart).body
      body.expiration.after(today())
    } catch (jwtException: JwtException) {
      false
    }
  }

  fun isCurrentRefreshTokenActive() = isTokenActive(accountManager.getPassword(findAccount()))

  private fun buildOAuthPayload(grantType: String) =
    mutableMapOf(
      GRANT_TYPE to grantType,
      CLIENT_ID to authConfiguration.clientId,
      CLIENT_SECRET to authConfiguration.clientSecret,
      SCOPE to authConfiguration.scope,
    )

  /**
   * This function fetches new access token from the authentication server and then creates a new
   * account if none exists; otherwise it updates the existing account.
   */
  suspend fun fetchAccessToken(username: String, password: CharArray): Result<OAuthResponse> {
    val body =
      buildOAuthPayload(PASSWORD).apply {
        put(USERNAME, username)
        put(PASSWORD, password.concatToString())
      }
    return try {
      val oAuthResponse = oAuthService.fetchToken(body)
      saveToken(username = username, password = password, oAuthResponse = oAuthResponse)
      Result.success(oAuthResponse)
    } catch (httpException: HttpException) {
      Result.failure(httpException)
    } catch (unknownHostException: UnknownHostException) {
      Result.failure(unknownHostException)
    } catch (sslHandShakeException: SSLHandshakeException) {
      Result.failure(sslHandShakeException)
    }
  }

  fun logout(): Result<Boolean> {
    val account = findAccount() ?: return Result.success(false)
    return runBlocking {
      try {
        // Logout remotely then invalidate token
        val responseBody =
          oAuthService.logout(
            clientId = authConfiguration.clientId,
            clientSecret = authConfiguration.clientSecret,
            refreshToken = accountManager.getPassword(account),
          )

        if (responseBody.isSuccessful) {
          accountManager.invalidateAuthToken(
            account.type,
            accountManager.peekAuthToken(account, AUTH_TOKEN_TYPE),
          )
          Result.success(true)
        } else Result.success(false)
      } catch (httpException: HttpException) {
        Result.failure(httpException)
      } catch (unknownHostException: UnknownHostException) {
        Result.failure(unknownHostException)
      }
    }
  }

  private fun saveToken(
    username: String,
    password: CharArray,
    oAuthResponse: OAuthResponse,
  ) {
    accountManager.run {
      val account =
        accounts.find { it.name == username && it.type == authConfiguration.accountType }
      if (account != null) {
        setPassword(account, oAuthResponse.refreshToken)
        setAuthToken(account, AUTH_TOKEN_TYPE, oAuthResponse.accessToken)
      } else {
        val newAccount = Account(username, authConfiguration.accountType)
        addAccountExplicitly(newAccount, oAuthResponse.refreshToken, null)
        setAuthToken(newAccount, AUTH_TOKEN_TYPE, oAuthResponse.accessToken)
      }
      // Save credentials
      secureSharedPreference.saveCredentials(username, password)
    }
  }

  /**
   * This function uses the provided [currentRefreshToken] to get a new auth token or throws
   * [HttpException] or [UnknownHostException] exceptions
   */
  @Throws(HttpException::class, UnknownHostException::class)
  fun refreshToken(currentRefreshToken: String): String {
    return runBlocking {
      val oAuthResponse =
        oAuthService.fetchToken(
          buildOAuthPayload(REFRESH_TOKEN).apply { put(REFRESH_TOKEN, currentRefreshToken) },
        )

      // Returns valid token or throws exception, NullPointerException not expected
      oAuthResponse.accessToken!!
    }
  }

  fun validateSavedLoginCredentials(username: String, enteredPassword: CharArray): Boolean {
    val credentials = secureSharedPreference.retrieveCredentials()
    return if (username.equals(credentials?.username, ignoreCase = true)) {
      val generatedHash =
        enteredPassword.toPasswordHash(Base64.getDecoder().decode(credentials!!.salt))
      generatedHash == credentials.passwordHash
    } else {
      false
    }
  }

  fun findAccount(): Account? {
    val credentials = secureSharedPreference.retrieveCredentials()
    return accountManager.getAccountsByType(authConfiguration.accountType).find {
      it.name == credentials?.username
    }
  }

  fun sessionActive(): Boolean =
    findAccount()?.let { isTokenActive(accountManager.peekAuthToken(it, AUTH_TOKEN_TYPE)) } ?: false

  fun invalidateSession(onSessionInvalidated: () -> Unit) {
    findAccount()?.let { account ->
      accountManager.run {
        invalidateAuthToken(account.type, AUTH_TOKEN_TYPE)
        runCatching { removeAccountExplicitly(account) }
          .onSuccess { onSessionInvalidated() }
          .onFailure {
            Timber.e(it)
            onSessionInvalidated()
          }
      }
    }
  }

  companion object {
    const val GRANT_TYPE = "grant_type"
    const val CLIENT_ID = "client_id"
    const val CLIENT_SECRET = "client_secret"
    const val SCOPE = "scope"
    const val USERNAME = "username"
    const val PASSWORD = "password"
    const val REFRESH_TOKEN = "refresh_token"
    const val AUTH_TOKEN_TYPE = "provider"
    const val CANCEL_BACKGROUND_SYNC = "cancelBackgroundSync"
  }

  override fun getAuthenticationMethod(): HttpAuthenticationMethod =
    HttpAuthenticationMethod.Bearer(getAccessToken())
}
