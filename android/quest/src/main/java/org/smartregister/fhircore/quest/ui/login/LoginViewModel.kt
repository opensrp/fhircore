/*
 * Copyright 2021-2024 Ona Systems, Inc
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
import android.widget.Toast
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import io.sentry.Sentry
import io.sentry.protocol.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.hl7.fhir.r4.model.Bundle as FhirR4ModelBundle
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.configuration.ConfigType
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.app.ApplicationConfiguration
import org.smartregister.fhircore.engine.configuration.app.ConfigService
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.data.remote.auth.KeycloakService
import org.smartregister.fhircore.engine.data.remote.fhir.resource.FhirResourceService
import org.smartregister.fhircore.engine.data.remote.model.response.UserInfo
import org.smartregister.fhircore.engine.data.remote.shared.TokenAuthenticator
import org.smartregister.fhircore.engine.datastore.PreferenceDataStore
import org.smartregister.fhircore.engine.datastore.PreferenceDataStore.Keys.CARE_TEAM_ID
import org.smartregister.fhircore.engine.datastore.PreferenceDataStore.Keys.LOCATION_ID
import org.smartregister.fhircore.engine.datastore.PreferenceDataStore.Keys.ORGANIZATION_ID
import org.smartregister.fhircore.engine.datastore.PreferenceDataStore.Keys.PRACTITIONER_DETAILS
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.SecureSharedPreference
import org.smartregister.fhircore.engine.util.SharedPreferenceKey
import org.smartregister.fhircore.engine.util.clearPasswordInMemory
import org.smartregister.fhircore.engine.util.extension.decodeJson
import org.smartregister.fhircore.engine.util.extension.decodeResourceFromString
import org.smartregister.fhircore.engine.util.extension.encodeJson
import org.smartregister.fhircore.engine.util.extension.encodeResourceToString
import org.smartregister.fhircore.engine.util.extension.extractLogicalIdUuid
import org.smartregister.fhircore.engine.util.extension.formatPhoneNumber
import org.smartregister.fhircore.engine.util.extension.getActivity
import org.smartregister.fhircore.engine.util.extension.isDeviceOnline
import org.smartregister.fhircore.engine.util.extension.practitionerEndpointUrl
import org.smartregister.fhircore.engine.util.extension.showToast
import org.smartregister.fhircore.engine.util.extension.valueToString
import org.smartregister.fhircore.quest.BuildConfig
import org.smartregister.model.location.LocationHierarchy
import org.smartregister.model.practitioner.PractitionerDetails
import retrofit2.HttpException
import timber.log.Timber

@HiltViewModel
class LoginViewModel
@Inject
constructor(
  val configurationRegistry: ConfigurationRegistry,
  val accountAuthenticator: AccountAuthenticator,
  val preferenceDataStore: PreferenceDataStore,
  val secureSharedPreference: SecureSharedPreference,
  val defaultRepository: DefaultRepository,
  val configService: ConfigService,
  val keycloakService: KeycloakService,
  val fhirResourceService: FhirResourceService,
  val tokenAuthenticator: TokenAuthenticator,
  val dispatcherProvider: DispatcherProvider,
  val workManager: WorkManager,
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
      val passwordAsCharArray = password.value!!.trim().toCharArray()

      viewModelScope.launch(dispatcherProvider.io()) {
        if (context.getActivity()?.isDeviceOnline() == true) {
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
            onFetchPractitioner = { bundleResult, userInfo ->
              if (bundleResult.isSuccess) {
                val bundle = bundleResult.getOrDefault(FhirR4ModelBundle())
                savePractitionerDetails(bundle, userInfo) {
                  _showProgressBar.postValue(false)
                  updateNavigateHome(true)
                }
              } else {
                _showProgressBar.postValue(false)
                Timber.e(bundleResult.exceptionOrNull())
                Timber.e(bundleResult.getOrNull().valueToString())
                _loginErrorState.postValue(LoginErrorState.ERROR_FETCHING_USER)
              }
            },
          )
        } else {
          if (secureSharedPreference.retrieveSessionUsername() == null) {
            _showProgressBar.postValue(false)
            _loginErrorState.postValue(LoginErrorState.INVALID_OFFLINE_STATE)
          } else if (
            accountAuthenticator.validateLoginCredentials(trimmedUsername, passwordAsCharArray)
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

  fun forgotPassword(context: Context) {
    val formattedNumber =
      applicationConfiguration.loginConfig.supervisorContactNumber.formatPhoneNumber(context)
    if (!formattedNumber.isNullOrBlank()) {
      _launchDialPad.value = formattedNumber
    } else {
      context.showToast(context.getString(R.string.missing_supervisor_contact), Toast.LENGTH_LONG)
    }
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
    onFetchPractitioner: (Result<FhirR4ModelBundle>, UserInfo?) -> Unit,
  ) {
    // ToDo : This is an object --->Practitioner Details
    val practitionerDetails =
      runBlocking {
        preferenceDataStore.read(
          key = PRACTITIONER_DETAILS
        ).firstOrNull()
      }?.decodeJson<PractitionerDetails>()


    if (tokenAuthenticator.sessionActive() && practitionerDetails != null) {
      _showProgressBar.postValue(false)
      updateNavigateHome(true)
    } else {
      // Prevent user from logging in with different credentials
      val existingCredentials = secureSharedPreference.retrieveCredentials()
      if (
        existingCredentials != null &&
          !username.equals(
            existingCredentials.username,
            true,
          )
      ) {
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

  suspend fun fetchPractitioner(
    onFetchUserInfo: (Result<UserInfo>) -> Unit,
    onFetchPractitioner: (Result<FhirR4ModelBundle>, UserInfo?) -> Unit,
  ) {
    try {
      val userInfo = keycloakService.fetchUserInfo().body()
      if (userInfo != null && !userInfo.keycloakUuid.isNullOrEmpty()) {
        writeUserInfo(userInfo = userInfo)
        onFetchUserInfo(Result.success(userInfo))
        try {
          val bundle =
            fhirResourceService.getResource(url = userInfo.keycloakUuid!!.practitionerEndpointUrl())
          onFetchPractitioner(Result.success(bundle), userInfo)
        } catch (httpException: HttpException) {
          onFetchPractitioner(Result.failure(httpException), userInfo)
          Timber.e(httpException.response()?.errorBody()?.charStream()?.readText())
        } catch (unknownHostException: UnknownHostException) {
          onFetchPractitioner(Result.failure(unknownHostException), userInfo)
          Timber.e(
            unknownHostException,
            "An error occurred fetching the practitioner details",
          )
        } catch (socketTimeoutException: SocketTimeoutException) {
          onFetchPractitioner(Result.failure(socketTimeoutException), userInfo)
          Timber.e(
            socketTimeoutException,
            "An error occurred fetching the practitioner details",
          )
        } catch (exception: Exception) {
          onFetchPractitioner(Result.failure(exception), userInfo)
          Timber.e(exception, "An error occurred fetching the practitioner details")
        }
      } else {
        onFetchPractitioner(
          Result.failure(NullPointerException("Keycloak user is null. Failed to fetch user.")),
          userInfo,
        )
      }
    } catch (httpException: HttpException) {
      onFetchUserInfo(Result.failure(httpException))
    } catch (unknownHostException: UnknownHostException) {
      onFetchUserInfo(Result.failure(unknownHostException))
      Timber.e(unknownHostException, "An error occurred fetching the practitioner details")
    } catch (socketTimeoutException: SocketTimeoutException) {
      onFetchUserInfo(Result.failure(socketTimeoutException))
      Timber.e(socketTimeoutException, "An error occurred fetching the practitioner details")
    } catch (exception: Exception) {
      onFetchUserInfo(Result.failure(exception))
      Timber.e(exception, "An error occurred fetching the practitioner details")
    }
  }

  fun savePractitionerDetails(
    bundle: FhirR4ModelBundle,
    userInfo: UserInfo?,
    postProcess: () -> Unit,
  ) {
    if (bundle.entry.isNullOrEmpty()) return
    viewModelScope.launch {
      bundle.entry.forEach { entry ->
        val practitionerDetails = entry.resource as PractitionerDetails
        val careTeams = practitionerDetails.fhirPractitionerDetails?.careTeams ?: listOf()
        val organizations = practitionerDetails.fhirPractitionerDetails?.organizations ?: listOf()
        val locations = practitionerDetails.fhirPractitionerDetails?.locations ?: listOf()
        val practitioners = practitionerDetails.fhirPractitionerDetails?.practitioners ?: listOf()
        val practitionerId =
          practitionerDetails.fhirPractitionerDetails?.practitionerId.valueToString()
        val locationHierarchies =
          practitionerDetails.fhirPractitionerDetails?.locationHierarchyList ?: listOf()

        val careTeamId =
          defaultRepository.createRemote(false, *careTeams.toTypedArray()).run {
            careTeams.map { it.id.extractLogicalIdUuid() }
          }

        val organizationId =
          defaultRepository.createRemote(false, *organizations.toTypedArray()).run {
            organizations.map { it.id.extractLogicalIdUuid() }
          }

        val locationId =
          defaultRepository.createRemote(false, *locations.toTypedArray()).run {
            locations.map { it.id.extractLogicalIdUuid() }
          }

        val location =
          defaultRepository.createRemote(false, *locations.toTypedArray()).run {
            locations.map { it.name }
          }

        val careTeam =
          defaultRepository.createRemote(false, *careTeams.toTypedArray()).run {
            careTeams.map { it.name }
          }

        val organization =
          defaultRepository.createRemote(false, *organizations.toTypedArray()).run {
            organizations.map { it.name }
          }

        defaultRepository.createRemote(false, *practitioners.toTypedArray())
        practitionerDetails.fhirPractitionerDetails?.groups?.toTypedArray()?.let {
          defaultRepository.createRemote(false, *it)
        }
        practitionerDetails.fhirPractitionerDetails?.practitionerRoles?.toTypedArray()?.let {
          defaultRepository.createRemote(false, *it)
        }
        practitionerDetails.fhirPractitionerDetails?.organizationAffiliations?.toTypedArray()?.let {
          defaultRepository.createRemote(false, *it)
        }

        if (practitionerId.isNotEmpty()) {
          writePractitionerDetailsToPreference(
            careTeam = careTeam,
            organization = organization,
            location = location,
            fhirPractitionerDetails = practitionerDetails,
            careTeamId = careTeamId,
            organizationId = organizationId,
            locationId = locationId,
            locationHierarchies = locationHierarchies,
          )
        } else {
          // The assumption here is that only 1 practitioner is returned from the server in the
          // practitioner details
          practitioners.firstOrNull()?.identifier?.forEach { identifier ->
            if (
              identifier.hasUse() &&
                identifier.use == org.hl7.fhir.r4.model.Identifier.IdentifierUse.SECONDARY &&
                identifier.hasValue() &&
                identifier.value == userInfo!!.keycloakUuid
            ) {
              writePractitionerDetailsToPreference(
                careTeam = careTeam,
                organization = organization,
                location = location,
                fhirPractitionerDetails = practitionerDetails,
                careTeamId = careTeamId,
                organizationId = organizationId,
                locationId = locationId,
                locationHierarchies = locationHierarchies,
              )
            }
          }
        }
      }
      postProcess()
    }
  }

  // ToDo : This is an object ----> userinfo
  private fun writeUserInfo(
    userInfo: UserInfo?,
  ) {
    runBlocking {
      preferenceDataStore.write(
        key = PreferenceDataStore.USER_INFO,
        value = userInfo.encodeJson(),
      )
    }
  }

  fun writePractitionerDetailsToPreference(
    careTeam: List<String>,
    organization: List<String>,
    location: List<String>,
    fhirPractitionerDetails: PractitionerDetails,
    careTeamId: List<String>,
    organizationId: List<String>,
    locationId: List<String>,
    locationHierarchies: List<LocationHierarchy>,
  ) {
    viewModelScope.launch {
      preferenceDataStore.write(
        key = PreferenceDataStore.PRACTITIONER_ID,
        value = fhirPractitionerDetails.fhirPractitionerDetails?.id ?: "",
      )
      // ToDo: This is an object type ----> pratictioner details
      preferenceDataStore.write(
        key = PRACTITIONER_DETAILS,
        value = fhirPractitionerDetails.encodeResourceToString()
      )
      preferenceDataStore.write(CARE_TEAM_ID, careTeamId.joinToString(separator = ","))
      preferenceDataStore.write(ORGANIZATION_ID, organizationId.joinToString(separator = ","))
      preferenceDataStore.write(LOCATION_ID, locationId.joinToString(separator = ","))
      // ToDo: This is an object type ----> Location hierarchy
      preferenceDataStore.write(
        key = PreferenceDataStore.PRACTITIONER_LOCATION_HIERARCHIES,
        value = locationHierarchies.joinToString(separator = ",")
      )
      preferenceDataStore.write(
        key = PreferenceDataStore.PRACTITIONER_LOCATION_NAME,
        value = location.joinToString(separator = ","),
      )
      preferenceDataStore.write(
        key = PreferenceDataStore.CARE_TEAM_NAME,
        value = careTeam.joinToString(separator = ","),
      )
      preferenceDataStore.write(
        key = PreferenceDataStore.ORGANIZATION_NAME,
        value = organization.joinToString(separator = ","),
      )
      preferenceDataStore.write(
        key = PreferenceDataStore.PRACTITIONER_LOCATION_ID,
        value = location.joinToString(separator = ""),
      )
    }
  }

  fun downloadNowWorkflowConfigs() {
    workManager.enqueue(
      OneTimeWorkRequestBuilder<ConfigDownloadWorker>()
        .setConstraints(
          Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build(),
        )
        .build(),
    )
  }
}