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
import java.io.IOException
import java.net.UnknownHostException
import javax.inject.Inject
import kotlinx.coroutines.launch
import okhttp3.ResponseBody
import org.hl7.fhir.r4.model.ResourceType
import org.jetbrains.annotations.TestOnly
import org.smartregister.fhircore.engine.auth.AccountAuthenticator
import org.smartregister.fhircore.engine.configuration.ConfigType
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.app.ApplicationConfiguration
import org.smartregister.fhircore.engine.configuration.app.ConfigService
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.data.remote.model.response.OAuthResponse
import org.smartregister.fhircore.engine.data.remote.model.response.UserInfo
import org.smartregister.fhircore.engine.data.remote.shared.ResponseCallback
import org.smartregister.fhircore.engine.data.remote.shared.ResponseHandler
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.SharedPreferenceKey
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.extension.decodeJson
import org.smartregister.fhircore.engine.util.extension.extractLogicalIdUuid
import org.smartregister.model.practitioner.PractitionerDetails
import retrofit2.Call
import retrofit2.Response
import timber.log.Timber

@HiltViewModel
class LoginViewModel
@Inject
constructor(
  val configurationRegistry: ConfigurationRegistry,
  val accountAuthenticator: AccountAuthenticator,
  val dispatcher: DispatcherProvider,
  val sharedPreferences: SharedPreferencesHelper,
  val defaultRepository: DefaultRepository,
  val configService: ConfigService
) : ViewModel(), AccountManagerCallback<Bundle> {

  private val _launchDialPad: MutableLiveData<String?> = MutableLiveData(null)
  val launchDialPad
    get() = _launchDialPad

  /**
   * Fetch the user info after verifying credentials with flow.
   *
   * On user-resp (failure) show-error. On user-resp (success) store user info and goto home.
   */
  val responseBodyHandler =
    object : ResponseHandler<ResponseBody> {
      override fun handleResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
        if (!response.isSuccessful) {
          handleFailure(call, IOException("Network call failed with $response"))
          Timber.i(response.errorBody()?.toString())
          return
        }

        val jsonResponseBody = response.body()!!.string()

        viewModelScope.launch(dispatcher.io()) {
          kotlin
            .runCatching {
              val bundle =
                accountAuthenticator.getPractitionerDetails(
                  keycloakUuid = jsonResponseBody.decodeJson<UserInfo>().keycloakUuid!!
                )
              savePractitionerDetails(bundle)
            }
            .onSuccess {
              _showProgressBar.postValue(false)
              _navigateToHome.postValue(true)
            }
            .onFailure { throwable ->
              Timber.e("Error fetching practitioner details", throwable)
              handleErrorMessage(throwable)
              _showProgressBar.postValue(false)
            }
        }
      }

      override fun handleFailure(call: Call<ResponseBody>, throwable: Throwable) {
        Timber.e(throwable)
        handleErrorMessage(throwable)
        _showProgressBar.postValue(false)
      }
    }

  suspend fun savePractitionerDetails(bundle: org.hl7.fhir.r4.model.Bundle) {
    if (!bundle.hasEntry()) return

    val practitionerDetails = bundle.entry.first().resource as PractitionerDetails

    val careTeams = practitionerDetails.fhirPractitionerDetails.careTeams ?: listOf()
    val organizations = practitionerDetails.fhirPractitionerDetails.organizations ?: listOf()
    val locations = practitionerDetails.fhirPractitionerDetails.locations ?: listOf()
    val locationHierarchies =
      practitionerDetails.fhirPractitionerDetails.locationHierarchyList ?: listOf()

    val careTeamIds =
      defaultRepository.create(*careTeams.toTypedArray()).map { it.extractLogicalIdUuid() }
    val organizationIds =
      defaultRepository.create(*organizations.toTypedArray()).map { it.extractLogicalIdUuid() }
    val locationIds =
      defaultRepository.create(*locations.toTypedArray()).map { it.extractLogicalIdUuid() }

    sharedPreferences.write(ResourceType.Practitioner.name, practitionerDetails.userDetail)
    sharedPreferences.write(ResourceType.CareTeam.name, careTeamIds)
    sharedPreferences.write(ResourceType.Organization.name, organizationIds)
    sharedPreferences.write(ResourceType.Location.name, locationIds)
    sharedPreferences.write(
      SharedPreferenceKey.PRACTITIONER_LOCATION_HIERARCHIES.name,
      locationHierarchies
    )
  }

  private val userInfoResponseCallback: ResponseCallback<ResponseBody> by lazy {
    object : ResponseCallback<ResponseBody>(responseBodyHandler) {}
  }

  /**
   * Call after remote login and subsequently fetch userinfo, handles network failures incase
   * previous successful attempt exists.
   *
   * On auth-resp (failure) show error, attempt local login (true), and goto home.
   *
   * On auth-resp success, fetch userinfo #LoginViewModel.responseBodyHandler. On subsequent
   * user-resp (failure) show-error, otherwise on user-resp (success) store user info, and goto
   * home.
   * ```
   * ```
   */
  val oauthResponseHandler =
    object : ResponseHandler<OAuthResponse> {
      override fun handleResponse(call: Call<OAuthResponse>, response: Response<OAuthResponse>) {
        if (!response.isSuccessful) {
          handleFailure(call, IOException("Network call failed with $response"))
        } else {
          accountAuthenticator.run {
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
          _showProgressBar.postValue(false)
          return
        }
        handleErrorMessage(throwable)
        _showProgressBar.postValue(false)
      }
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

  private val _loginErrorState = MutableLiveData<LoginErrorState?>()
  val loginErrorState: LiveData<LoginErrorState?>
    get() = _loginErrorState

  private val _showProgressBar = MutableLiveData(false)
  val showProgressBar
    get() = _showProgressBar

  val applicationConfiguration: ApplicationConfiguration by lazy {
    configurationRegistry.retrieveConfiguration(ConfigType.Application)
  }

  fun attemptLocalLogin(): Boolean {
    return accountAuthenticator.validLocalCredentials(
      username.value!!.trim(),
      password.value!!.trim().toCharArray()
    )
  }

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

  fun onUsernameUpdated(username: String) {
    _loginErrorState.postValue(null)
    _username.postValue(username)
  }

  fun onPasswordUpdated(password: String) {
    _loginErrorState.postValue(null)
    _password.postValue(password)
  }

  override fun run(future: AccountManagerFuture<Bundle>?) {
    val bundle = future?.result ?: bundleOf()
    bundle.getString(AccountManager.KEY_AUTHTOKEN)?.run {
      if (this.isNotEmpty() && accountAuthenticator.tokenManagerService.isTokenActive(this)) {
        _navigateToHome.postValue(true)
      }
    }
  }

  fun attemptRemoteLogin() {
    if (!username.value.isNullOrBlank() && !password.value.isNullOrBlank()) {
      _loginErrorState.postValue(null)
      _showProgressBar.postValue(true)

      // For subsequent logins only allow previously logged in accounts
      accountAuthenticator.run {
        val trimmedUsername = username.value!!.trim()
        if (validatePreviousLogin(trimmedUsername)) {
          fetchToken(trimmedUsername, password.value!!.trim().toCharArray())
            .enqueue(object : ResponseCallback<OAuthResponse>(oauthResponseHandler) {})
        } else {
          _loginErrorState.postValue(LoginErrorState.MULTI_USER_LOGIN_ATTEMPT)
          _showProgressBar.postValue(false)
        }
      }
    }
  }

  fun forgotPassword() {
    // TODO load supervisor contact e.g.
    _launchDialPad.value = "tel:0123456789"
  }

  @TestOnly
  fun navigateToHome(navigateHome: Boolean = true) {
    _navigateToHome.postValue(navigateHome)
  }

  private fun handleErrorMessage(throwable: Throwable) {
    if (throwable is UnknownHostException) _loginErrorState.postValue(LoginErrorState.UNKNOWN_HOST)
    else _loginErrorState.postValue(LoginErrorState.INVALID_CREDENTIALS)
  }

  companion object {
    const val IDENTIFIER = "identifier"
  }
}
