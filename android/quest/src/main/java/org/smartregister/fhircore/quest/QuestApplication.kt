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
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.FhirEngineProvider
import com.google.android.fhir.datacapture.DataCaptureConfig
import com.google.android.fhir.sync.Sync
import com.google.android.fhir.sync.SyncJob
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.ResourceType
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

  override val authenticationService: AuthenticationService
    get() = QuestAuthenticationService(applicationContext)

  override val fhirEngine: FhirEngine by lazy { FhirEngineProvider.getInstance(this) }

  override val secureSharedPreference: SecureSharedPreference
    get() = SecureSharedPreference(applicationContext)

  override val resourceSyncParams: Map<ResourceType, Map<String, String>>
    get() {
      return mapOf(
        ResourceType.CarePlan to mapOf(),
        ResourceType.Patient to mapOf(),
        ResourceType.Questionnaire to mapOf(),
        ResourceType.QuestionnaireResponse to mapOf(),
        ResourceType.Binary to mapOf()
      )
    }

  override val authenticatedUserInfo: UserInfo?
    get() = SharedPreferencesHelper.read(USER_INFO_SHARED_PREFERENCE_KEY, null)?.decodeJson<UserInfo>()

  private fun buildPublisherFilterMap(): MutableMap<String, String> {
    val questionnaireFilterMap: MutableMap<String, String> = HashMap()
    val publisher = authenticatedUserInfo?.questionnairePublisher
    if (publisher != null) questionnaireFilterMap[Questionnaire.SP_PUBLISHER] = publisher
    return questionnaireFilterMap
  }
 //TODO............ http://hl7.org/fhir/us/odh/Patient-patient-odh-maya-gordon.json.html
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

    fun getProfileConfigId() = CONFIG_PROFILE.join(getContext().authenticatedUserInfo?.questionnairePublisher?.lowercase()?.let { "-$it" }, "")

    fun getContext() = questApplication
  }
}
