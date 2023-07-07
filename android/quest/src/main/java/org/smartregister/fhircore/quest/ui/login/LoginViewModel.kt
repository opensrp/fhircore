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

package org.smartregister.fhircore.quest.ui.login

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.lifecycle.HiltViewModel
import io.sentry.Sentry
import io.sentry.protocol.User
import java.net.UnknownHostException
import javax.inject.Inject
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.hl7.fhir.r4.model.Bundle as FhirR4ModelBundle
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.configuration.ConfigType
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.app.ApplicationConfiguration
import org.smartregister.fhircore.engine.configuration.app.ConfigService
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.data.remote.auth.KeycloakService
import org.smartregister.fhircore.engine.data.remote.fhir.resource.FhirResourceService
import org.smartregister.fhircore.engine.data.remote.model.response.UserInfo
import org.smartregister.fhircore.engine.data.remote.shared.TokenAuthenticator
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.SecureSharedPreference
import org.smartregister.fhircore.engine.util.SharedPreferenceKey
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.clearPasswordInMemory
import org.smartregister.fhircore.engine.util.extension.extractLogicalIdUuid
import org.smartregister.fhircore.engine.util.extension.getActivity
import org.smartregister.fhircore.engine.util.extension.isDeviceOnline
import org.smartregister.fhircore.engine.util.extension.practitionerEndpointUrl
import org.smartregister.fhircore.engine.util.extension.valueToString
import org.smartregister.fhircore.quest.BuildConfig
import org.smartregister.model.practitioner.PractitionerDetails
import retrofit2.HttpException
import timber.log.Timber

@HiltViewModel
class LoginViewModel
@Inject
constructor(
  val configurationRegistry: ConfigurationRegistry,
  val accountAuthenticator: AccountAuthenticator,
  val sharedPreferences: SharedPreferencesHelper,
  val secureSharedPreference: SecureSharedPreference,
  val defaultRepository: DefaultRepository,
  val configService: ConfigService,
  val keycloakService: KeycloakService,
  val fhirResourceService: FhirResourceService,
  val tokenAuthenticator: TokenAuthenticator,
  val dispatcherProvider: DispatcherProvider,
  val workManager: WorkManager
) : ViewModel() {

  private val _launchDialPad: MutableLiveData<String?> = MutableLiveData(null)
  val launchDialPad
    get() = _launchDialPad

  private val _navigateToHome = MutableLiveData(false)
  val navigateToHome: LiveData<Boolean>
    get() = _navigateToHome

  private val _username = MutableLiveData("")
  val username: LiveData<String>
    get() = _username

  private val _password = MutableLiveData("")
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

  fun onUsernameUpdated(username: String) {
    _loginErrorState.postValue(null)
    _username.value = username
  }

  fun onPasswordUpdated(password: String) {
    _loginErrorState.postValue(null)
    _password.value = password
  }

  fun login(context: Context) {
    if (!username.value.isNullOrBlank() && !password.value.isNullOrBlank()) {
      _loginErrorState.postValue(null)
      _showProgressBar.postValue(true)

      val trimmedUsername = username.value!!.trim()
      val passwordAsCharArray = password.value!!.toCharArray()

      viewModelScope.launch(dispatcherProvider.io()) {
        if (context.getActivity()!!.isDeviceOnline()) {
          fetchToken(
            username = trimmedUsername,
            password = passwordAsCharArray,
            onFetchUserInfo = {
              if (it.isFailure) {
                Timber.e(it.exceptionOrNull())
                _showProgressBar.postValue(false)
                _loginErrorState.postValue(LoginErrorState.ERROR_FETCHING_USER)
              }
            },
            onFetchPractitioner = { bundleResult ->
              if (bundleResult.isSuccess) {
                val bundle = bundleResult.getOrDefault(FhirR4ModelBundle())
                savePractitionerDetails(bundle) {
                  _showProgressBar.postValue(false)
                  updateNavigateHome(true)
                }
              } else {
                _showProgressBar.postValue(false)
                Timber.e(bundleResult.exceptionOrNull())
                Timber.e(bundleResult.getOrNull().valueToString())
                _loginErrorState.postValue(LoginErrorState.ERROR_FETCHING_USER)
              }
            }
          )
        } else {
          if (secureSharedPreference.retrieveSessionUsername() == null) {

            _showProgressBar.postValue(false)
            _loginErrorState.postValue(LoginErrorState.INVALID_OFFLINE_STATE)
          } else if (accountAuthenticator.validateLoginCredentials(
              trimmedUsername,
              passwordAsCharArray
            )
          ) {
            try {

              // Configure Sentry scope
              Sentry.configureScope { scope ->
                scope.setTag("versionCode", BuildConfig.VERSION_CODE.toString())
                scope.setTag("versionName", BuildConfig.VERSION_NAME)
                scope.user = User().apply { username = trimmedUsername }
              }
            } catch (e: Exception) {
              Timber.e(e)
            }

            _showProgressBar.postValue(false)
            updateNavigateHome(true)
          } else {
            _showProgressBar.postValue(false)
            _loginErrorState.postValue(LoginErrorState.INVALID_CREDENTIALS)
          }
        }
        clearPasswordInMemory(passwordAsCharArray)
      }
    }
  }

  fun forgotPassword() {
    // TODO load supervisor contact e.g.
    _launchDialPad.value = "tel:0123456789"
  }

  fun updateNavigateHome(navigateHome: Boolean = true) {
    _navigateToHome.postValue(navigateHome)
  }

  fun isPinEnabled(): Boolean = applicationConfiguration.loginConfig.enablePin ?: false

  /**
   * This function checks first if the existing token is active otherwise fetches a new token, then
   * gets the user information from the authentication server. The id of the retrieved user is used
   * to obtain the [PractitionerDetails] from the FHIR server.
   */
  private suspend fun fetchToken(
    username: String,
    password: CharArray,
    onFetchUserInfo: (Result<UserInfo>) -> Unit,
    onFetchPractitioner: (Result<FhirR4ModelBundle>) -> Unit
  ) {
    val practitionerDetails =
      sharedPreferences.read<PractitionerDetails>(
        key = SharedPreferenceKey.PRACTITIONER_DETAILS.name,
        decodeWithGson = true
      )
    if (tokenAuthenticator.sessionActive() && practitionerDetails != null) {
      _showProgressBar.postValue(false)
      updateNavigateHome(true)
    } else {
      // Prevent user from logging in with different credentials
      val existingCredentials = secureSharedPreference.retrieveCredentials()
      if (existingCredentials != null && !username.equals(existingCredentials.username, true)) {
        _showProgressBar.postValue(false)
        _loginErrorState.postValue(LoginErrorState.MULTI_USER_LOGIN_ATTEMPT)
      } else {
        tokenAuthenticator
          .fetchAccessToken(username, password)
          .onSuccess { fetchPractitioner(onFetchUserInfo, onFetchPractitioner) }
          .onFailure {
            _showProgressBar.postValue(false)
            var errorState = LoginErrorState.ERROR_FETCHING_USER

            if (it is HttpException) {
              when (it.code()) {
                401 -> errorState = LoginErrorState.INVALID_CREDENTIALS
              }
            } else if (it is UnknownHostException) {
              errorState = LoginErrorState.UNKNOWN_HOST
            }

            _loginErrorState.postValue(errorState)
            Timber.e(it)
          }
      }
    }
  }

  private suspend fun fetchPractitioner(
    onFetchUserInfo: (Result<UserInfo>) -> Unit,
    onFetchPractitioner: (Result<FhirR4ModelBundle>) -> Unit
  ) {
    try {
      val userInfo = keycloakService.fetchUserInfo().body()
      if (userInfo != null && !userInfo.keycloakUuid.isNullOrEmpty()) {
        onFetchUserInfo(Result.success(userInfo))
        try {
          val bundle =
            fhirResourceService.getResource(url = userInfo.keycloakUuid!!.practitionerEndpointUrl())
          onFetchPractitioner(Result.success(bundle))
        } catch (httpException: HttpException) {
          onFetchPractitioner(Result.failure(httpException))
          Timber.e(httpException.response()?.errorBody()?.charStream()?.readText())
        }
      } else {
        onFetchPractitioner(
          Result.failure(NullPointerException("Keycloak user is null. Failed to fetch user."))
        )
      }
    } catch (httpException: HttpException) {
      onFetchUserInfo(Result.failure(httpException))
    }
  }

  fun savePractitionerDetails(bundle: FhirR4ModelBundle, postProcess: () -> Unit) {
    if (bundle.entry.isNullOrEmpty()) return
    viewModelScope.launch {
      val practitionerDetails = bundle.entry.first().resource as PractitionerDetails

      val careTeams = practitionerDetails.fhirPractitionerDetails?.careTeams ?: listOf()
      val organizations = practitionerDetails.fhirPractitionerDetails?.organizations ?: listOf()
      val locations = practitionerDetails.fhirPractitionerDetails?.locations ?: listOf()
      val locationHierarchies =
        practitionerDetails.fhirPractitionerDetails?.locationHierarchyList ?: listOf()

      val careTeamIds =
        withContext(dispatcherProvider.io()) {
          defaultRepository.createRemote(false, *careTeams.toTypedArray()).run {
            careTeams.map { it.id.extractLogicalIdUuid() }
          }
        }
      val organizationIds =
        withContext(dispatcherProvider.io()) {
          defaultRepository.createRemote(false, *organizations.toTypedArray()).run {
            organizations.map { it.id.extractLogicalIdUuid() }
          }
        }
      val locationIds =
        withContext(dispatcherProvider.io()) {
          defaultRepository.createRemote(false, *locations.toTypedArray()).run {
            locations.map { it.id.extractLogicalIdUuid() }
          }
        }

      sharedPreferences.write(
        key = SharedPreferenceKey.PRACTITIONER_ID.name,
        value = practitionerDetails.fhirPractitionerDetails?.practitionerId.valueToString()
      )

      sharedPreferences.write(SharedPreferenceKey.PRACTITIONER_DETAILS.name, practitionerDetails)
      sharedPreferences.write(ResourceType.CareTeam.name, careTeamIds)
      sharedPreferences.write(ResourceType.Organization.name, organizationIds)
      sharedPreferences.write(ResourceType.Location.name, locationIds)
      sharedPreferences.write(
        SharedPreferenceKey.PRACTITIONER_LOCATION_HIERARCHIES.name,
        locationHierarchies
      )

      postProcess()
    }
  }

  fun downloadNowWorkflowConfigs() {
    val oneTimeWorkRequest: OneTimeWorkRequest =
      OneTimeWorkRequestBuilder<ConfigDownloadWorker>()
        .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
        .build()
    workManager.enqueue(oneTimeWorkRequest)
  }
}
