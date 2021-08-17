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

package org.smartregister.fhircore.eir.ui.login

import android.accounts.AccountManager
import android.accounts.AccountManagerCallback
import android.accounts.AccountManagerFuture
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.google.android.material.internal.TextWatcherAdapter
import okhttp3.ResponseBody
import org.smartregister.fhircore.eir.BuildConfig
import org.smartregister.fhircore.eir.EirAuthenticationService
import org.smartregister.fhircore.eir.R
import org.smartregister.fhircore.eir.ui.base.model.LoginUser
import org.smartregister.fhircore.engine.auth.AuthCredentials
import org.smartregister.fhircore.engine.auth.AuthenticationService
import org.smartregister.fhircore.engine.data.remote.model.response.OAuthResponse
import org.smartregister.fhircore.engine.util.SecureSharedPreference
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import timber.log.Timber

class LoginViewModel(application: Application) :
  AndroidViewModel(application), Callback<OAuthResponse>, AccountManagerCallback<Bundle> {

  var loginUser = LoginUser()
  var allowLogin = MutableLiveData(false)
  var loginFailed = MutableLiveData(false)
  var showPassword = MutableLiveData(false)
  var goHome = MutableLiveData(false)
  var showProgressIcon = MutableLiveData(false)

  internal var secureSharedPreference: SecureSharedPreference = SecureSharedPreference(application)
  internal var authenticationService: AuthenticationService
  private var accountManager: AccountManager

  init {
    Timber.i("Starting auth flow for login")

    accountManager = AccountManager.get(application)
    authenticationService = EirAuthenticationService(application.applicationContext)

    if ((BuildConfig.DEBUG && BuildConfig.SKIP_AUTH_CHECK) ||
        authenticationService.isSessionActive(secureSharedPreference.retrieveSessionToken())
    ) {
      goHome.value = true
    } else {
      authenticationService.loadAccount(
        accountManager,
        secureSharedPreference.retrieveSessionUsername(),
        this,
        Handler(OnTokenError())
      )
    }
  }

  var credentialsWatcher: TextWatcher =
    object : TextWatcherAdapter() {
      override fun afterTextChanged(editable: Editable) {
        allowLogin.value = loginUser.username.isNotEmpty() && loginUser.password.isNotEmpty()
      }
    }

  fun remoteLogin(view: View) {
    Timber.i("Logging in for ${loginUser.username} ")
    showProgressIcon.value = true
    allowLogin.value = false
    authenticationService
      .fetchToken(loginUser.username, loginUser.password.toCharArray())
      .enqueue(this)
  }

  override fun onResponse(call: Call<OAuthResponse>, response: Response<OAuthResponse>) {
    Timber.i("Got login response ${response.isSuccessful}")
    loginFailed.value = !response.isSuccessful
    showProgressIcon.value = false

    if (!response.isSuccessful) {
      Timber.e("Error in fetching credentials %s", response.errorBody()?.string())
      allowLogin.value = true
      if (allowLocalLogin()) {
        goHome.value = true
      }
      return
    }

    authenticationService.addAuthenticatedAccount(accountManager, response, loginUser.username)

    val accessToken = response.body()!!.accessToken!!

    secureSharedPreference.saveCredentials(
      AuthCredentials(loginUser.username, loginUser.password, accessToken)
    )

    authenticationService.getUserInfo().enqueue(OnUserInfoResponse(this, getApplication()))
    goHome.value = true
  }

  override fun onFailure(call: Call<OAuthResponse>, throwable: Throwable) {
    Timber.e(javaClass.name, throwable.stackTraceToString())
    showProgressIcon.value = false
    Toast.makeText(getApplication(), R.string.login_call_fail_error_message, Toast.LENGTH_LONG)
      .show()

    if (allowLocalLogin()) {
      goHome.value = true
      return
    }

    allowLogin.value = true
  }

  internal fun allowLocalLogin(): Boolean {
    val creds = secureSharedPreference.retrieveCredentials() ?: return false
    return (creds.username.contentEquals(loginUser.username) &&
      creds.password.contentEquals(loginUser.password))
  }

  override fun run(future: AccountManagerFuture<Bundle>?) {
    val bundle: Bundle = future?.result ?: bundleOf()

    // The token is a named value in the bundle. The name of the value
    // is stored in the constant AccountManager.KEY_AUTHTOKEN.
    val token = bundle.getString(AccountManager.KEY_AUTHTOKEN)

    Timber.i("Token received is null/empty: ${token.isNullOrEmpty()}")

    if (!token.isNullOrEmpty() && authenticationService.isSessionActive(token)) {
      goHome.value = true
    }
  }
}

private class OnUserInfoResponse(val viewModel: LoginViewModel, context: Context) :
  Callback<ResponseBody> {
  val baseContext = context

  override fun onFailure(call: Call<ResponseBody>?, throwable: Throwable) {
    Timber.e(throwable)
    viewModel.showProgressIcon.value = false
    Toast.makeText(baseContext, R.string.userinfo_call_fail_error_message, Toast.LENGTH_LONG).show()
  }

  override fun onResponse(call: Call<ResponseBody>?, response: Response<ResponseBody>?) {
    Timber.i(response?.body()?.string())
    SharedPreferencesHelper.init(baseContext).write("USER", response?.body()?.string()!!)
  }
}

private class OnTokenError : Handler.Callback {
  override fun handleMessage(msg: Message): Boolean {
    Timber.i("Error in getting token in view model")
    return true
  }
}
