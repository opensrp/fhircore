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
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.MalformedJwtException
import io.jsonwebtoken.UnsupportedJwtException
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton
import org.smartregister.fhircore.engine.configuration.app.ConfigService
import org.smartregister.fhircore.engine.util.SecureSharedPreference
import timber.log.Timber

@Singleton
class TokenManagerService
@Inject
constructor(
  @ApplicationContext val context: Context,
  val accountManager: AccountManager,
  val configService: ConfigService,
  val secureSharedPreference: SecureSharedPreference
) {

  fun getBlockingActiveAuthToken(): String? {
    getLocalSessionToken()?.let {
      return it
    }
    Timber.v("Trying to get blocking auth token from account manager")
    return getActiveAccount()?.let {
      accountManager.blockingGetAuthToken(it, AccountAuthenticator.AUTH_TOKEN_TYPE, false)
        ?: error("Auth Token expired or invalid")
    }
  }

  fun getActiveAccount(): Account? {
    Timber.v("Checking for an active account stored")
    return secureSharedPreference.retrieveSessionUsername()?.let { username ->
      accountManager.getAccountsByType(configService.provideAuthConfiguration().accountType).find {
        it.name.equals(username)
      }
    }
  }

  fun getLocalSessionToken(invalidateCached: Boolean = true): String? {
    Timber.v("Checking local storage for access token")
    val token = secureSharedPreference.retrieveSessionToken()
    if (invalidateCached && !token.isNullOrBlank() && !isTokenActive(token)) {
      accountManager.invalidateAuthToken(AccountAuthenticator.AUTH_TOKEN_TYPE, token)
    }
    return if (isTokenActive(token)) token else null
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
}
