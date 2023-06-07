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
import android.content.Intent
import android.os.Bundle
import android.os.Looper
import androidx.hilt.work.HiltWorkerFactory
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.work.Configuration
import com.github.anrwatchdog.ANRWatchDog
import com.google.android.fhir.datacapture.DataCaptureConfig
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import com.google.firebase.perf.ktx.performance
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import org.smartregister.fhircore.engine.auth.AccountAuthenticator
import org.smartregister.fhircore.engine.data.remote.fhir.resource.FhirXFhirQueryResolver
import org.smartregister.fhircore.engine.data.remote.fhir.resource.ReferenceUrlResolver
import org.smartregister.fhircore.engine.ui.appsetting.AppSettingActivity
import org.smartregister.fhircore.engine.ui.login.LoginActivity
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireItemViewHolderFactoryMatchersProviderFactoryImpl
import org.smartregister.fhircore.engine.util.extension.showToast
import timber.log.Timber

@HiltAndroidApp
class QuestApplication :
  Application(), DataCaptureConfig.Provider, DefaultLifecycleObserver, Configuration.Provider {

  @Inject lateinit var workerFactory: HiltWorkerFactory

  @Inject lateinit var referenceUrlResolver: ReferenceUrlResolver

  @Inject lateinit var accountAuthenticator: AccountAuthenticator

  @Inject lateinit var xFhirQueryResolver: FhirXFhirQueryResolver

  private val launcherActivityName: String? by lazy {
    val pm = packageManager
    val launcherIntent = pm.getLaunchIntentForPackage(packageName)
    val activityList = pm.queryIntentActivities(launcherIntent!!, 0)
    activityList.first().activityInfo.name
  }

  private val activitiesAccessWithoutAuth by lazy {
    listOfNotNull(
      LoginActivity::class.java.name,
      AppSettingActivity::class.java.name,
      launcherActivityName
    )
  }

  var onInActivityListener: OnInActivityListener? = null

  lateinit var appInActivityListener: AppInActivityListener

  private var mForegroundActivityContext: Context? = null

  private var configuration: DataCaptureConfig? = null

  override fun onCreate() {
    super<Application>.onCreate()

    initANRWatcher()

    if (BuildConfig.DEBUG.not()) {
      Thread.setDefaultUncaughtExceptionHandler(globalExceptionHandler)
    } else {
      Firebase.performance.isPerformanceCollectionEnabled = false
      Firebase.crashlytics.setCrashlyticsCollectionEnabled(false)
      Timber.plant(Timber.DebugTree())
    }

    appInActivityListener =
      AppInActivityListener(activitiesAccessWithoutAuth) { onInActivityListener?.onTimeout() }

    registerActivityLifecycleCallbacks(
      object : ActivityLifecycleCallbacks {
        override fun onActivityStarted(activity: Activity) {
          appInActivityListener.current(activity.javaClass)
          val activityName = activity::class.java.name
          if (activityName !in activitiesAccessWithoutAuth) {
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
          urlResolver = referenceUrlResolver,
          xFhirQueryResolver = xFhirQueryResolver,
          questionnaireItemViewHolderFactoryMatchersProviderFactory =
            QuestionnaireItemViewHolderFactoryMatchersProviderFactoryImpl
        )
    return configuration as DataCaptureConfig
  }

  override fun onStop(owner: LifecycleOwner) {
    appInActivityListener.start()
    mForegroundActivityContext = null
  }

  override fun onStart(owner: LifecycleOwner) {
    appInActivityListener.stop()
    mForegroundActivityContext
      ?.takeIf {
        val name = it::class.java.name
        name !in activitiesAccessWithoutAuth
      }
      ?.let { accountAuthenticator.confirmActiveAccount { intent -> it.startActivity(intent) } }
  }

  private fun initANRWatcher() {
    // Detect input timeout ANRs
    ANRWatchDog().setANRListener { Timber.e(it) }.start()
  }

  override fun getWorkManagerConfiguration(): Configuration =
    Configuration.Builder()
      .setMinimumLoggingLevel(android.util.Log.INFO)
      .setWorkerFactory(workerFactory)
      .build()

  private val globalExceptionHandler =
    Thread.UncaughtExceptionHandler { _: Thread, e: Throwable -> handleUncaughtException(e) }

  private fun handleUncaughtException(e: Throwable) {
    Firebase.crashlytics.recordException(e)
    showToast(this.getString(R.string.error_occurred))
    Timber.e(e)

    if (Looper.myLooper() == Looper.getMainLooper()) {
      val intent = Intent(applicationContext, AppSettingActivity::class.java)
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
      intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
      startActivity(intent)
    }
  }
}
