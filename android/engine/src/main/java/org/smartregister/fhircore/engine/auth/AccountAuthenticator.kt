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
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.core.os.bundleOf
import java.util.Locale
import timber.log.Timber

class AccountAuthenticator(val context: Context, val authenticationService: AuthenticationService) :
  AbstractAccountAuthenticator(context) {

  val accountManager: AccountManager = AccountManager.get(context)

  override fun addAccount(
    response: AccountAuthenticatorResponse,
    accountType: String,
    authTokenType: String?,
    requiredFeatures: Array<out String>?,
    options: Bundle
  ): Bundle {
    Timber.i("Adding account of type $accountType with auth token of type $authTokenType")

    val intent =
      Intent(context, authenticationService.getLoginActivityClass()).apply {
        putExtra(AccountManager.KEY_ACCOUNT_TYPE, authenticationService.getAccountType())
        putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response)
        putExtra(AuthenticationService.AUTH_TOKEN_TYPE, authTokenType)
        putExtra(AuthenticationService.IS_NEW_ACCOUNT, true)
      }

    return Bundle().apply { putParcelable(AccountManager.KEY_INTENT, intent) }
  }

  override fun getAuthToken(
    response: AccountAuthenticatorResponse,
    account: Account,
    authTokenType: String,
    options: Bundle?
  ): Bundle {
    var accessToken = authenticationService.getLocalSessionToken()

    Timber.i(
      "Access token for user ${account.name}, account type ${account.type}, token type $authTokenType is available:${accessToken?.isNotBlank()}"
    )

    if (accessToken.isNullOrBlank()) {
      // Use saved refresh token to try to get new access token. Logout user otherwise
      authenticationService.getRefreshToken()?.let {
        Timber.i("Saved active refresh token is available")

        runCatching {
          authenticationService.refreshToken(it)?.let { newTokenResponse ->
            accessToken = newTokenResponse.accessToken!!
            authenticationService.updateSession(newTokenResponse)
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
  ): Bundle {
    return Bundle()
  }

  override fun confirmCredentials(
    response: AccountAuthenticatorResponse,
    account: Account,
    options: Bundle?
  ): Bundle {
    return bundleOf()
  }

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
      Intent(context, authenticationService.getLoginActivityClass()).apply {
        putExtra(AccountManager.KEY_ACCOUNT_TYPE, account.type)
        putExtra(AccountManager.KEY_ACCOUNT_NAME, account.name)
        putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response)
        putExtra(AuthenticationService.AUTH_TOKEN_TYPE, authTokenType)
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
}
