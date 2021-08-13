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

package org.smartregister.fhircore.eir

import android.app.Application
import android.content.Context
import androidx.work.Constraints
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.FhirEngineBuilder
import com.google.android.fhir.sync.PeriodicSyncConfiguration
import com.google.android.fhir.sync.RepeatInterval
import com.google.android.fhir.sync.Sync
import java.util.concurrent.TimeUnit
import org.smartregister.fhircore.engine.configuration.app.ApplicationConfiguration
import org.smartregister.fhircore.engine.configuration.app.ConfigurableApplication
import org.smartregister.fhircore.engine.configuration.app.applicationConfigurationOf
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import timber.log.Timber

class EirApplication : Application(), ConfigurableApplication {

  override lateinit var applicationConfiguration: ApplicationConfiguration

  override fun onCreate() {
    super.onCreate()
    SharedPreferencesHelper.init(this)
    eirApplication = this
    configureApplication(
        applicationConfigurationOf(
            oauthServerBaseUrl = BuildConfig.OAUTH_BASE_URL,
            fhirServerBaseUrl = BuildConfig.FHIR_BASE_URL,
            clientId = BuildConfig.OAUTH_CIENT_ID,
            clientSecret = BuildConfig.OAUTH_CLIENT_SECRET))

    if (BuildConfig.DEBUG) {
      Timber.plant(Timber.DebugTree())
    }
  }

  // only initiate the FhirEngine when used for the first time, not when the app is created
  private val fhirEngine: FhirEngine by lazy { constructFhirEngine() }

  private fun constructFhirEngine(): FhirEngine {
    Sync.periodicSync<EirFhirSyncWorker>(
        this,
        PeriodicSyncConfiguration(
            syncConstraints = Constraints.Builder().build(),
            repeat = RepeatInterval(interval = 1, timeUnit = TimeUnit.HOURS)))

    return FhirEngineBuilder(this).build()
  }

  fun eirConfigurations() = this.applicationConfiguration

  companion object {

    private lateinit var eirApplication: EirApplication

    fun fhirEngine(context: Context) = (context.applicationContext as EirApplication).fhirEngine

    fun getContext() = eirApplication
  }

  override fun configureApplication(applicationConfiguration: ApplicationConfiguration) {
    this.applicationConfiguration = applicationConfiguration
  }
}
