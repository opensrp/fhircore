package org.smartregister.fhircore.auth.account

import android.accounts.Account
import android.accounts.AccountManager
import android.accounts.AccountManagerCallback
import android.accounts.NetworkErrorException
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Handler
import okhttp3.ResponseBody
import org.smartregister.fhircore.BuildConfig
import org.smartregister.fhircore.api.OAuthService
import org.smartregister.fhircore.auth.OAuthResponse
import retrofit2.Call
import retrofit2.HttpException
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
    } catch (e: HttpException) {
      throw e
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
}
