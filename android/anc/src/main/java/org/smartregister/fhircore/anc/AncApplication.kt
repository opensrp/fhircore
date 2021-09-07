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

package org.smartregister.fhircore.anc

import android.app.Application
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.FhirEngineBuilder
import com.google.android.fhir.sync.PeriodicSyncConfiguration
import com.google.android.fhir.sync.RepeatInterval
import com.google.android.fhir.sync.Sync
import java.util.concurrent.TimeUnit
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.auth.AuthenticationService
import org.smartregister.fhircore.engine.configuration.app.ApplicationConfiguration
import org.smartregister.fhircore.engine.configuration.app.ConfigurableApplication
import org.smartregister.fhircore.engine.configuration.app.applicationConfigurationOf
import org.smartregister.fhircore.engine.util.SecureSharedPreference
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import timber.log.Timber

class AncApplication : Application(), ConfigurableApplication {
  override val applicationConfiguration: ApplicationConfiguration by lazy {
    constructConfiguration()
  }

  override val authenticationService: AuthenticationService
    get() = AncAuthenticationService(applicationContext)

  override val fhirEngine: FhirEngine by lazy { constructFhirEngine() }

  override val secureSharedPreference: SecureSharedPreference
    get() = SecureSharedPreference(applicationContext)

  override val resourceSyncParams: Map<ResourceType, Map<String, String>>
    get() =
      mapOf(
        ResourceType.Patient to emptyMap(),
        ResourceType.Questionnaire to emptyMap(),
        ResourceType.StructureMap to mapOf(),
        ResourceType.RelatedPerson to mapOf(),
        ResourceType.CarePlan to mapOf()
      )

  private fun constructFhirEngine(): FhirEngine {
    return FhirEngineBuilder(this).build()
  }

  private fun constructConfiguration(): ApplicationConfiguration {
    return applicationConfigurationOf(
      oauthServerBaseUrl = BuildConfig.OAUTH_BASE_URL,
      fhirServerBaseUrl = BuildConfig.FHIR_BASE_URL,
      clientId = BuildConfig.OAUTH_CIENT_ID,
      clientSecret = BuildConfig.OAUTH_CLIENT_SECRET,
      languages = listOf("en", "sw")
    )
  }

  override fun configureApplication(applicationConfiguration: ApplicationConfiguration) {
    throw UnsupportedOperationException("Can not override existing configuration")
  }

  override fun onCreate() {
    super.onCreate()
    SharedPreferencesHelper.init(this)
    ancApplication = this

    if (BuildConfig.DEBUG) {
      Timber.plant(Timber.DebugTree())
    }
  }

  companion object {
    private lateinit var ancApplication: AncApplication

    fun getContext() = ancApplication

    // Make sure that it is called only from one place in app and is done after login
    fun schedulePolling() =
      Sync.basicSyncJob(ancApplication)
        .poll(
          PeriodicSyncConfiguration(
            repeat = RepeatInterval(interval = 1, timeUnit = TimeUnit.HOURS)
          ),
          AncFhirSyncWorker::class.java
        )
  }
}
