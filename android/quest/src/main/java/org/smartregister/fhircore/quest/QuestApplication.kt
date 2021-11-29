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

package org.smartregister.fhircore.quest

import android.app.Application
import android.content.Context
import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.parser.IParser
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.FhirEngineProvider
import com.google.android.fhir.datacapture.DataCaptureConfig
import com.google.android.fhir.sync.Sync
import com.google.android.fhir.sync.SyncJob
import org.hl7.fhir.r4.model.ResourceType
import org.hl7.fhir.r4.model.SearchParameter
import org.hl7.fhir.r4.utils.FHIRPathEngine
import org.json.JSONArray
import org.smartregister.fhircore.engine.auth.AuthenticationService
import org.smartregister.fhircore.engine.configuration.app.ApplicationConfiguration
import org.smartregister.fhircore.engine.configuration.app.ConfigurableApplication
import org.smartregister.fhircore.engine.data.remote.model.response.UserInfo
import org.smartregister.fhircore.engine.util.SecureSharedPreference
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.USER_INFO_SHARED_PREFERENCE_KEY
import org.smartregister.fhircore.engine.util.extension.decodeJson
import org.smartregister.fhircore.engine.util.extension.join
import org.smartregister.fhircore.engine.util.extension.runPeriodicSync
import timber.log.Timber

open class QuestApplication : Application(), ConfigurableApplication {

  override val syncJob: SyncJob
    get() = Sync.basicSyncJob(getContext())

  override lateinit var applicationConfiguration: ApplicationConfiguration
  val SYNC_CONFIG = "configurations/app/sync_config.json"

  override val authenticationService: AuthenticationService
    get() = QuestAuthenticationService(applicationContext)

  override val fhirEngine: FhirEngine by lazy { FhirEngineProvider.getInstance(this) }

  override val secureSharedPreference: SecureSharedPreference
    get() = SecureSharedPreference(applicationContext)

  override val fhirPathEngine = FHIRPathEngine(workerContextProvider)

  override val authenticatedUserInfo: UserInfo?
    get() =
      SharedPreferencesHelper.read(USER_INFO_SHARED_PREFERENCE_KEY, null)?.decodeJson<UserInfo>()

  override val resourceSyncParams: Map<ResourceType, Map<String, String>>
    get() {
      val searchParams = loadSearchParams(this)
      val pairs = mutableListOf<Pair<ResourceType, Map<String, String>>>()
      for (i in searchParams.indices) {
        // TODO: expressionValue supports for Organization and Publisher, extend it using
        // Composition resource
        val expressionValue =
          searchParams[i].expression?.let {
            when {
              it.contains("organization") -> authenticatedUserInfo?.organization
              it.contains("publisher") -> authenticatedUserInfo?.questionnairePublisher
              else -> null
            }
          }

        pairs.add(
          Pair(
            ResourceType.fromCode(searchParams[i].base[0].code),
            expressionValue?.let { mapOf(searchParams[i].expression to it) } ?: mapOf()
          )
        )
      }

      return mapOf(*pairs.toTypedArray())
    }

  private fun loadSearchParams(context: Context): List<SearchParameter> {
    val iParser: IParser = FhirContext.forR4().newJsonParser()
    val json = context.assets.open(SYNC_CONFIG).bufferedReader().use { it.readText() }
    val searchParameters = mutableListOf<SearchParameter>()

    val jsonArrayEntry = JSONArray(json)
    (0 until jsonArrayEntry.length()).forEach {
      searchParameters.add(
        iParser.parseResource(jsonArrayEntry.getJSONObject(it).toString()) as SearchParameter
      )
    }
    return searchParameters
  }

  override fun configureApplication(applicationConfiguration: ApplicationConfiguration) {
    this.applicationConfiguration = applicationConfiguration
    this.applicationConfiguration.apply {
      fhirServerBaseUrl = BuildConfig.FHIR_BASE_URL
      oauthServerBaseUrl = BuildConfig.OAUTH_BASE_URL
      clientId = BuildConfig.OAUTH_CIENT_ID
      clientSecret = BuildConfig.OAUTH_CLIENT_SECRET
    }
    SharedPreferencesHelper.write(SharedPreferencesHelper.THEME, applicationConfiguration.theme)
  }

  override fun schedulePeriodicSync() {
    this.runPeriodicSync<QuestFhirSyncWorker>()
  }

  override fun onCreate() {
    super.onCreate()
    SharedPreferencesHelper.init(this)
    questApplication = this

    if (BuildConfig.DEBUG) {
      Timber.plant(Timber.DebugTree())
    }
    DataCaptureConfig.attachmentResolver = ReferenceAttachmentResolver(this)
  }

  companion object {
    private lateinit var questApplication: QuestApplication
    private const val CONFIG_PROFILE = "quest-app-profile"

    fun getProfileConfigId() = CONFIG_PROFILE.join(getPublisher()?.lowercase()?.let { "-$it" }, "")

    fun getContext() = questApplication

    fun getPublisher() = SharedPreferencesHelper.read(USER_INFO_SHARED_PREFERENCE_KEY, null)
  }
}
