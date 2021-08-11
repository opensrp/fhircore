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
import org.smartregister.fhircore.auth.OAuthResponse
import org.smartregister.fhircore.auth.account.AccountConfig.AUTH_HANDLER_ACTIVITY
import timber.log.Timber

class AccountAuthenticator(context: Context) : AbstractAccountAuthenticator(context) {

  private val myContext = context
  internal var accountHelper = AccountHelper(myContext)
  var accountManager = AccountManager.get(myContext)

  override fun addAccount(
    response: AccountAuthenticatorResponse,
    accountType: String,
    authTokenType: String?,
    requiredFeatures: Array<out String>?,
    options: Bundle
  ): Bundle {
    Timber.i("Adding account of type $accountType with auth token of type $authTokenType")

    val intent = Intent(myContext, AUTH_HANDLER_ACTIVITY)

    intent.putExtra(AccountManager.KEY_ACCOUNT_TYPE, AccountConfig.ACCOUNT_TYPE)
    intent.putExtra(AccountConfig.KEY_AUTH_TOKEN_TYPE, authTokenType)
    intent.putExtra(AccountConfig.KEY_IS_NEW_ACCOUNT, true)
    intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response)

    val bundle = Bundle()
    bundle.putParcelable(AccountManager.KEY_INTENT, intent)
    return bundle
  }

  override fun getAuthToken(
    response: AccountAuthenticatorResponse,
    account: Account,
    authTokenType: String,
    options: Bundle
  ): Bundle {
    var accessToken = accountManager.peekAuthToken(account, authTokenType)
    var tokenResponse: OAuthResponse

    Timber.i("GetAuthToken for %s: AccessToken (%b)", account.name, accessToken?.isNotBlank())

    // if no access token try getting one with refresh token
    if (accessToken.isNullOrEmpty()) {
      val refreshToken = accountManager.getPassword(account)

      Timber.i("GetAuthToken: Refresh Token (%b)", refreshToken?.isNotBlank())

      if (!refreshToken.isNullOrEmpty()) {
        runCatching {
          tokenResponse = accountHelper.refreshToken(refreshToken)!!

          accessToken = tokenResponse.accessToken

          accountManager.setPassword(account, refreshToken)
          accountManager.setAuthToken(account, authTokenType, accessToken)

          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            accountManager.notifyAccountAuthenticated(account)
          }
        }
          .onFailure {
            Timber.e("Error refreshing token")
            Timber.e(it.stackTraceToString())
          }
          .onSuccess { Timber.i("Got new accessToken") }
      }
    }

    if (!accessToken.isNullOrEmpty()) {
      val result = Bundle()
      result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name)
      result.putString(AccountManager.KEY_ACCOUNT_TYPE, account.type)
      result.putString(AccountManager.KEY_AUTHTOKEN, accessToken)
      return result
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
    return authTokenType.toUpperCase(Locale.ROOT)
  }

  override fun updateCredentials(
    response: AccountAuthenticatorResponse,
    account: Account,
    authTokenType: String?,
    options: Bundle?
  ): Bundle {
    Timber.i("Updating credentials for ${account.name} from auth activity")

    val intent = Intent(myContext, AUTH_HANDLER_ACTIVITY)
    intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response)
    intent.putExtra(AccountManager.KEY_ACCOUNT_TYPE, account.type)
    intent.putExtra(AccountManager.KEY_ACCOUNT_NAME, account.name)

    val bundle = Bundle()
    bundle.putParcelable(AccountManager.KEY_INTENT, intent)
    return bundle
  }

  override fun hasFeatures(
    response: AccountAuthenticatorResponse,
    account: Account,
    features: Array<out String>
  ): Bundle {
    return bundleOf(Pair(AccountManager.KEY_BOOLEAN_RESULT, false))
  }
}
