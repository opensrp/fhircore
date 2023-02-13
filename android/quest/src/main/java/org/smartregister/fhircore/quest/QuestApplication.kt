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

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import androidx.hilt.work.HiltWorkerFactory
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.work.Configuration
import com.github.anrwatchdog.ANRWatchDog
import com.google.android.fhir.datacapture.DataCaptureConfig
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import org.smartregister.fhircore.engine.auth.AccountAuthenticator
import org.smartregister.fhircore.engine.data.remote.fhir.resource.FhirXFhirQueryResolver
import org.smartregister.fhircore.engine.data.remote.fhir.resource.ReferenceAttachmentResolver
import org.smartregister.fhircore.engine.ui.appsetting.AppSettingActivity
import org.smartregister.fhircore.engine.ui.login.LoginActivity
import timber.log.Timber

@HiltAndroidApp
class QuestApplication :
  Application(), DataCaptureConfig.Provider, DefaultLifecycleObserver, Configuration.Provider {

  @Inject lateinit var workerFactory: HiltWorkerFactory

  @Inject lateinit var referenceAttachmentResolver: ReferenceAttachmentResolver

  @Inject lateinit var accountAuthenticator: AccountAuthenticator

  @Inject lateinit var xFhirQueryResolver: FhirXFhirQueryResolver

  var onInActivityListener: OnInActivityListener? = null

  var appInActivityListener: AppInActivityListener =
    AppInActivityListener(
      listOf(LoginActivity::class.java.name, AppSettingActivity::class.java.name)
    ) { onInActivityListener?.onTimeout() }

  private var mForegroundActivityContext: Context? = null

  private val launcherActivityName: String? by lazy {
    val pm = packageManager
    val launcherIntent = pm.getLaunchIntentForPackage(packageName)
    val activityList = pm.queryIntentActivities(launcherIntent!!, 0)
    activityList.first().activityInfo.name
  }

  private var configuration: DataCaptureConfig? = null

  override fun onCreate() {
    super<Application>.onCreate()

    // Detect input timeout ANRs
    ANRWatchDog().start()

    if (BuildConfig.DEBUG) {
      Timber.plant(Timber.DebugTree())
    }

    registerActivityLifecycleCallbacks(
      object : ActivityLifecycleCallbacks {
        override fun onActivityStarted(activity: Activity) {
          appInActivityListener.current(activity.javaClass)
          if (activity::class.java.name != launcherActivityName) {
            mForegroundActivityContext = activity
          }
        }

        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

        override fun onActivityResumed(activity: Activity) {}

        override fun onActivityPaused(activity: Activity) {}

        override fun onActivityStopped(activity: Activity) {}

        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

        override fun onActivityDestroyed(activity: Activity) {}
      }
    )
    ProcessLifecycleOwner.get().lifecycle.addObserver(this@QuestApplication)
  }

  override fun getDataCaptureConfig(): DataCaptureConfig {
    configuration =
      configuration
        ?: DataCaptureConfig(
          attachmentResolver = referenceAttachmentResolver,
          xFhirQueryResolver = xFhirQueryResolver
        )
    return configuration as DataCaptureConfig
  }

  override fun onStop(owner: LifecycleOwner) {
    appInActivityListener.start()
    mForegroundActivityContext = null
  }

  override fun onStart(owner: LifecycleOwner) {
    appInActivityListener.stop()
    if (mForegroundActivityContext != null) {
      accountAuthenticator.loadActiveAccount(
        onActiveAuthTokenFound = {},
        onValidTokenMissing = {
          if (it.component!!.className != mForegroundActivityContext!!::class.java.name) {
            mForegroundActivityContext!!.startActivity(it)
          }
        }
      )
    }
  }

  override fun getWorkManagerConfiguration(): Configuration =
    Configuration.Builder()
      .setMinimumLoggingLevel(android.util.Log.INFO)
      .setWorkerFactory(workerFactory)
      .build()
}
