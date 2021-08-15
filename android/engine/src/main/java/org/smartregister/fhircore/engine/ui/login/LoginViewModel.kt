package org.smartregister.fhircore.engine.ui.login

import android.accounts.AccountManager
import android.accounts.AccountManagerCallback
import android.accounts.AccountManagerFuture
import android.app.Application
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import okhttp3.ResponseBody
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.auth.AuthCredentials
import org.smartregister.fhircore.engine.auth.AuthenticationService
import org.smartregister.fhircore.engine.configuration.app.ConfigurableApplication
import org.smartregister.fhircore.engine.configuration.view.LoginViewConfiguration
import org.smartregister.fhircore.engine.data.model.response.OAuthResponse
import org.smartregister.fhircore.engine.data.remote.shared.ResponseCallback
import org.smartregister.fhircore.engine.data.remote.shared.ResponseHandler
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.extension.showToast
import retrofit2.Call
import retrofit2.Response
import timber.log.Timber

class LoginViewModel(
  application: Application,
  val authenticationService: AuthenticationService,
  loginViewConfiguration: LoginViewConfiguration,
  private val dispatcher: DispatcherProvider = DefaultDispatcherProvider
) : AndroidViewModel(application), AccountManagerCallback<Bundle> {

  private val accountManager = AccountManager.get(application)

  val responseBodyHandler =
    object : ResponseHandler<ResponseBody> {
      override fun handleResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
        response.body()?.run {
          Timber.i(this.string())
          SharedPreferencesHelper.init(application.applicationContext).write("USER", this.string())
        }
      }

      override fun handleFailure(call: Call<ResponseBody>, throwable: Throwable) {
        Timber.e(throwable)
        application.showToast(application.getString(R.string.userinfo_call_fail_error_message))
      }
    }

  private val userInfoResponseCallback: ResponseCallback<ResponseBody> by lazy {
    object : ResponseCallback<ResponseBody>(responseBodyHandler) {}
  }

  private val secureSharedPreference =
    (application as ConfigurableApplication).secureSharedPreference

  val oauthResponseHandler =
    object : ResponseHandler<OAuthResponse> {
      override fun handleResponse(call: Call<OAuthResponse>, response: Response<OAuthResponse>) {
        if (!response.isSuccessful) {
          Timber.e("Error fetching access token %s", response.errorBody()?.string())
          if (attemptLocalLogin()) _navigateToHome.value = true
          return
        }
        with(authenticationService) {
          addAuthenticatedAccount(accountManager, response, username.value!!)
          secureSharedPreference.saveCredentials(
            AuthCredentials(username.value!!, password.value!!, response.body()?.accessToken!!)
          )
          getUserInfo().enqueue(userInfoResponseCallback)
          _navigateToHome.value = true
        }
      }

      override fun handleFailure(call: Call<OAuthResponse>, throwable: Throwable) {
        Timber.e(throwable.stackTraceToString())
        application.showToast(application.getString(R.string.login_call_fail_error_message))

        if (attemptLocalLogin()) {
          _navigateToHome.value = true
          return
        }
      }
    }

  private fun attemptLocalLogin(): Boolean {
    val (localUsername, localPassword) =
      secureSharedPreference.retrieveCredentials() ?: return false
    return (localUsername.contentEquals(username.value, ignoreCase = false) &&
      localPassword.contentEquals(password.value))
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

  private val _loginViewConfiguration = MutableLiveData(loginViewConfiguration)
  val loginViewConfiguration: LiveData<LoginViewConfiguration>
    get() = _loginViewConfiguration

  fun loginUser() {
    viewModelScope.launch(dispatcher.io()) {
      if (authenticationService.skipLogin() ||
          authenticationService.isSessionActive(secureSharedPreference.retrieveSessionToken())
      ) {
        _navigateToHome.postValue(true)
      } else {
        authenticationService.loadAccount(
          accountManager,
          secureSharedPreference.retrieveSessionUsername(),
          this@LoginViewModel
        )
      }
    }
  }

  fun updateViewConfigurations(registerViewConfiguration: LoginViewConfiguration) {
    _loginViewConfiguration.value = registerViewConfiguration
  }

  fun onUsernameUpdated(username: String) {
    _username.value = username
  }

  fun onPasswordUpdated(password: String) {
    _password.value = password
  }

  override fun run(future: AccountManagerFuture<Bundle>?) {
    val bundle = future?.result ?: bundleOf()
    bundle.getString(AccountManager.KEY_AUTHTOKEN)?.run {
      if (this.isNotEmpty() && authenticationService.isSessionActive(this)) {
        _navigateToHome.value = true
      }
    }
  }

  fun attemptRemoteLogin() {
    if (username.value != null && password.value != null) {
      authenticationService
        .fetchToken(username.value!!, password.value!!.toCharArray())
        .enqueue(oauthResponseCallback)
    }
  }
}
