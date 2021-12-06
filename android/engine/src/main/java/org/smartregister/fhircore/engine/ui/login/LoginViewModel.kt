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

package org.smartregister.fhircore.engine.ui.login

import android.accounts.AccountManager
import android.accounts.AccountManagerCallback
import android.accounts.AccountManagerFuture
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch
import okhttp3.ResponseBody
import org.jetbrains.annotations.TestOnly
import org.smartregister.fhircore.engine.auth.AccountAuthenticator
import org.smartregister.fhircore.engine.configuration.view.LoginViewConfiguration
import org.smartregister.fhircore.engine.configuration.view.loginViewConfigurationOf
import org.smartregister.fhircore.engine.data.remote.model.response.OAuthResponse
import org.smartregister.fhircore.engine.data.remote.model.response.UserInfo
import org.smartregister.fhircore.engine.data.remote.shared.ResponseCallback
import org.smartregister.fhircore.engine.data.remote.shared.ResponseHandler
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.USER_INFO_SHARED_PREFERENCE_KEY
import org.smartregister.fhircore.engine.util.extension.decodeJson
import org.smartregister.fhircore.engine.util.extension.encodeJson
import retrofit2.Call
import retrofit2.Response
import timber.log.Timber

@HiltViewModel
class LoginViewModel
@Inject
constructor(
  val accountAuthenticator: AccountAuthenticator,
  val dispatcher: DispatcherProvider,
  val sharedPreferences: SharedPreferencesHelper
) : ViewModel(), AccountManagerCallback<Bundle> {

  private val _launchDialPad: MutableLiveData<String?> = MutableLiveData(null)
  val launchDialPad
    get() = _launchDialPad

  val responseBodyHandler =
    object : ResponseHandler<ResponseBody> {
      override fun handleResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
        response.body()?.run {
          storeUserPreferences(this)
          _navigateToHome.value = true
          _showProgressBar.postValue(false)
        }
      }

      override fun handleFailure(call: Call<ResponseBody>, throwable: Throwable) {
        Timber.e(throwable)
        _loginError.postValue(throwable.localizedMessage)
        _showProgressBar.postValue(false)
      }
    }

  private fun storeUserPreferences(responseBody: ResponseBody) {
    val responseBodyString = responseBody.string()
    Timber.d(responseBodyString)
    val userResponse = responseBodyString.decodeJson<UserInfo>()
    sharedPreferences.write(USER_INFO_SHARED_PREFERENCE_KEY, userResponse.encodeJson())
  }

  private val userInfoResponseCallback: ResponseCallback<ResponseBody> by lazy {
    object : ResponseCallback<ResponseBody>(responseBodyHandler) {}
  }

  val oauthResponseHandler =
    object : ResponseHandler<OAuthResponse> {
      override fun handleResponse(call: Call<OAuthResponse>, response: Response<OAuthResponse>) {
        if (!response.isSuccessful) {
          val errorResponse = response.errorBody()?.string()
          _loginError.postValue(errorResponse?.decodeJson<LoginError>()?.errorDescription)
          _showProgressBar.postValue(false)
          Timber.e("Error fetching access token %s", errorResponse)
          return
        } else {
          if (attemptLocalLogin()) _navigateToHome.value = true
          _showProgressBar.postValue(false)
          with(accountAuthenticator) {
            addAuthenticatedAccount(
              response,
              username.value!!.trim(),
              password.value?.trim()?.toCharArray()!!
            )
            getUserInfo().enqueue(userInfoResponseCallback)
          }
        }
      }

      override fun handleFailure(call: Call<OAuthResponse>, throwable: Throwable) {
        Timber.e(throwable.stackTraceToString())
        if (attemptLocalLogin()) {
          _navigateToHome.value = true
          return
        }
        _loginError.postValue(throwable.localizedMessage)
        _showProgressBar.postValue(false)
      }
    }

  fun attemptLocalLogin(): Boolean {
    return accountAuthenticator.validLocalCredentials(
      username.value!!.trim(),
      password.value!!.trim().toCharArray()
    )
  }

  private val oauthResponseCallback: ResponseCallback<OAuthResponse> by lazy {
    object : ResponseCallback<OAuthResponse>(oauthResponseHandler) {}
  }

  private val _navigateToHome = MutableLiveData<Boolean>()
  val navigateToHome: LiveData<Boolean>
    get() = _navigateToHome

  private val _username = MutableLiveData<String>()
  val username: LiveData<String>
    get() = _username

  private val _password = MutableLiveData<String>()
  val password: LiveData<String>
    get() = _password

  private val _loginError = MutableLiveData<String>()
  val loginError: LiveData<String>
    get() = _loginError

  private val _showProgressBar = MutableLiveData(false)
  val showProgressBar
    get() = _showProgressBar

  private val _loginViewConfiguration = MutableLiveData(loginViewConfigurationOf())
  val loginViewConfiguration: LiveData<LoginViewConfiguration>
    get() = _loginViewConfiguration

  fun loginUser() {
    viewModelScope.launch(dispatcher.io()) {
      if (accountAuthenticator.hasActiveSession()) {
        Timber.v("Login not needed .. navigating to home directly")
        _navigateToHome.postValue(true)
      } else {
        accountAuthenticator.loadActiveAccount(this@LoginViewModel)
      }
    }
  }

  fun updateViewConfigurations(registerViewConfiguration: LoginViewConfiguration) {
    _loginViewConfiguration.value = registerViewConfiguration
  }

  fun onUsernameUpdated(username: String) {
    _loginError.postValue("")
    _username.postValue(username)
  }

  fun onPasswordUpdated(password: String) {
    _loginError.postValue("")
    _password.postValue(password)
  }

  override fun run(future: AccountManagerFuture<Bundle>?) {
    val bundle = future?.result ?: bundleOf()
    bundle.getString(AccountManager.KEY_AUTHTOKEN)?.run {
      if (this.isNotEmpty() && accountAuthenticator.tokenManagerService.isTokenActive(this)) {
        _navigateToHome.value = true
      }
    }
  }

  fun attemptRemoteLogin() {
    if (!username.value.isNullOrBlank() && !password.value.isNullOrBlank()) {
      _loginError.postValue("")
      _showProgressBar.postValue(true)
      accountAuthenticator
        .fetchToken(username.value!!.trim(), password.value!!.trim().toCharArray())
        .enqueue(oauthResponseCallback)
    }
  }

  fun forgotPassword() {
    // TODO load supervisor contact e.g.
    _launchDialPad.value = "tel:0123456789"
  }

  @TestOnly
  fun navigateToHome(navigateHome: Boolean = true) {
    _navigateToHome.value = navigateHome
    _navigateToHome.postValue(navigateHome)
  }
}
