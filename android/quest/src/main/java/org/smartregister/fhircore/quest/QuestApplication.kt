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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.context.SimpleWorkerContext
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.auth.AuthenticationService
import org.smartregister.fhircore.engine.configuration.app.ApplicationConfiguration
import org.smartregister.fhircore.engine.configuration.app.ConfigurableApplication
import org.smartregister.fhircore.engine.configuration.app.loadApplicationConfiguration
import org.smartregister.fhircore.engine.configuration.view.loadRegisterViewConfiguration
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.engine.util.SecureSharedPreference
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.USER_QUESTIONNAIRE_PUBLISHER_SHARED_PREFERENCE_KEY
import org.smartregister.fhircore.engine.util.extension.initializeWorkerContext
import org.smartregister.fhircore.engine.util.extension.join
import org.smartregister.fhircore.engine.util.extension.runPeriodicSync
import timber.log.Timber

class QuestApplication : Application(), ConfigurableApplication {

  private val defaultDispatcherProvider = DefaultDispatcherProvider

  override lateinit var workerContextProvider: SimpleWorkerContext

  override val syncJob: SyncJob
    get() = Sync.basicSyncJob(getContext())

  override lateinit var applicationConfiguration: ApplicationConfiguration

  override val authenticationService: AuthenticationService
    get() = QuestAuthenticationService(applicationContext)

  override val fhirEngine: FhirEngine by lazy { constructFhirEngine() }

  override val secureSharedPreference: SecureSharedPreference
    get() = SecureSharedPreference(applicationContext)

  override val resourceSyncParams: Map<ResourceType, Map<String, String>>
    get() {
      val metadata = getMetadataSyncParams()

      val primaryFilter = getPatientRegisterConfig().primaryFilter ?: return metadata

      val clinicalData = mutableMapOf(
        ResourceType.Patient to
                mapOf(primaryFilter.key to "${primaryFilter.system}|${primaryFilter.code}"),
        ResourceType.QuestionnaireResponse to
                mapOf(primaryFilter.key to "${primaryFilter.system}|${primaryFilter.code}"),
      )
      clinicalData.plusAssign(metadata)

      return clinicalData
    }

  fun getMetadataSyncParams(): Map<ResourceType, Map<String, String>> {
    return mutableMapOf(
      ResourceType.Binary to mapOf("_id" to getBinaryConfigIds()),
      ResourceType.Questionnaire to buildPublisherFilterMap(),
      ResourceType.StructureMap to buildPublisherFilterMap()
    )
  }

  private fun buildPublisherFilterMap(): MutableMap<String, String> {
    val questionnaireFilterMap: MutableMap<String, String> = HashMap()
    val publisher = getPublisher()
    if (publisher != null) questionnaireFilterMap[Questionnaire.SP_PUBLISHER] = publisher
    return questionnaireFilterMap
  }

  private fun constructFhirEngine(): FhirEngine {
    return FhirEngineProvider.getInstance(this)
  }

  override fun configureApplication(applicationConfiguration: ApplicationConfiguration) {
    this.applicationConfiguration = applicationConfiguration
    SharedPreferencesHelper.write(SharedPreferencesHelper.THEME, applicationConfiguration.theme)
  }

  fun applyApplicationConfiguration() {
    configureApplication(
      loadApplicationConfiguration(CONFIG_APP).apply {
        fhirServerBaseUrl = BuildConfig.FHIR_BASE_URL
        oauthServerBaseUrl = BuildConfig.OAUTH_BASE_URL
        clientId = BuildConfig.OAUTH_CIENT_ID
        clientSecret = BuildConfig.OAUTH_CLIENT_SECRET
      }
    )
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

    applyApplicationConfiguration()

    initializeWorkerContextProvider()

    schedulePeriodicSync()
  }

  fun initializeWorkerContextProvider() {
    CoroutineScope(defaultDispatcherProvider.io()).launch {
      workerContextProvider = this@QuestApplication.initializeWorkerContext()!!
    }

    schedulePeriodicSync()

    DataCaptureConfig.attachmentResolver = ReferenceAttachmentResolver(this)
  }

  companion object {
    private lateinit var questApplication: QuestApplication
    const val CONFIG_APP = "quest-app"
    private const val CONFIG_PATIENT_REGISTER = "quest-app-patient-register"
    private const val CONFIG_PROFILE = "quest-app-profile"

    private fun getBinaryConfigIds() =
      "$CONFIG_APP,${getPatientRegisterConfigId()},${getProfileConfigId()}"

    fun getPatientRegisterConfig() =
      getContext().loadRegisterViewConfiguration(getPatientRegisterConfigId())

    fun getPatientRegisterConfigId() =
      CONFIG_PATIENT_REGISTER.join(getPublisher()?.lowercase()?.let { "-$it" }, "")

    fun getProfileConfigId() = CONFIG_PROFILE.join(getPublisher()?.lowercase()?.let { "-$it" }, "")

    fun getContext() = questApplication

    fun getPublisher() =
      SharedPreferencesHelper.read(USER_QUESTIONNAIRE_PUBLISHER_SHARED_PREFERENCE_KEY, null)
  }
}
