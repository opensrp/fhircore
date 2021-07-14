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

package org.smartregister.fhircore

import android.app.Application
import android.content.Context
import androidx.work.Constraints
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.FhirEngineBuilder
import com.google.android.fhir.sync.PeriodicSyncConfiguration
import com.google.android.fhir.sync.RepeatInterval
import com.google.android.fhir.sync.Sync
import java.util.concurrent.TimeUnit
import org.smartregister.fhircore.data.FhirPeriodicSyncWorker
import org.smartregister.fhircore.util.SharedPreferencesHelper
import timber.log.Timber

class FhirApplication : Application() {

  override fun onCreate() {
    super.onCreate()
    SharedPreferencesHelper.init(this)
    mContext = this

    if (BuildConfig.DEBUG) {
      Timber.plant(Timber.DebugTree())
    }
  }

  // only initiate the FhirEngine when used for the first time, not when the app is created
  private val fhirEngine: FhirEngine by lazy { constructFhirEngine() }
  private val mInstance: FhirApplication by lazy { this }

  private fun constructFhirEngine(): FhirEngine {
    SharedPreferencesHelper.init(this)

    // Schedule periodic syncing
    Sync.periodicSync<FhirPeriodicSyncWorker>(
      this,
      PeriodicSyncConfiguration(
        syncConstraints = Constraints.Builder().build(),
        repeat = RepeatInterval(interval = 15, timeUnit = TimeUnit.MINUTES)
      )
    )

    return FhirEngineBuilder(this).build()
  }

  companion object {

    private lateinit var mContext: FhirApplication

    @JvmStatic
    fun fhirEngine(context: Context) = (context.applicationContext as FhirApplication).fhirEngine

    fun getContext() = mContext
  }
}
