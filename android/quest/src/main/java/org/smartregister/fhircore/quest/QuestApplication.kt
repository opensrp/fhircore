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

package org.smartregister.fhircore.quest

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.database.CursorWindow
import android.os.Bundle
import android.os.Handler
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.google.android.fhir.datacapture.DataCaptureConfig
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import org.smartregister.fhircore.engine.data.remote.fhir.resource.ReferenceUrlResolver
import org.smartregister.fhircore.engine.util.extension.showToast
import org.smartregister.fhircore.quest.data.QuestXFhirQueryResolver
import org.smartregister.fhircore.quest.ui.pin.PinLoginActivity
import org.smartregister.fhircore.quest.ui.questionnaire.QuestionnaireItemViewHolderFactoryMatchersProviderFactoryImpl
import timber.log.Timber

@HiltAndroidApp
class QuestApplication :
  Application(),
  DataCaptureConfig.Provider,
  Configuration.Provider,
  Application.ActivityLifecycleCallbacks {

  @Inject lateinit var workerFactory: HiltWorkerFactory

  @Inject lateinit var referenceUrlResolver: ReferenceUrlResolver

  @Inject lateinit var xFhirQueryResolver: QuestXFhirQueryResolver

  private var configuration: DataCaptureConfig? = null

  var currentActivity: Activity? = null

  var lastCreated: Activity? = null

  override fun onCreate() {
    super.onCreate()
    if (BuildConfig.DEBUG) {
      Timber.plant(Timber.DebugTree())
    }

    if (BuildConfig.DEBUG) {
      Thread.setDefaultUncaughtExceptionHandler(globalExceptionHandler)
    }

    registerActivityLifecycleCallbacks(this)

    // TODO Fix this workaround for cursor size issue. Currently size set to 10 MB
    try {
      val field = CursorWindow::class.java.getDeclaredField("sCursorWindowSize")
      field.apply {
        isAccessible = true
        set(null, 10 * 1024 * 1024) // 10MB
      }
    } catch (e: Exception) {
      Timber.e(e)
    }
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

  override fun getWorkManagerConfiguration(): Configuration =
    Configuration.Builder()
      .setMinimumLoggingLevel(if (BuildConfig.DEBUG) Log.VERBOSE else Log.INFO)
      .setWorkerFactory(workerFactory)
      .build()

  private val globalExceptionHandler =
    Thread.UncaughtExceptionHandler { _: Thread, e: Throwable -> handleUncaughtException(e) }

  private fun handleUncaughtException(e: Throwable) {
    showToast(this.getString(R.string.error_occurred))
    Timber.e(e)

    currentActivity?.finish()
    val mainHandler = Handler(applicationContext.mainLooper)

    mainHandler.post {
      val intent = Intent(applicationContext, PinLoginActivity::class.java)
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
      startActivity(intent)
    }
  }

  override fun registerActivityLifecycleCallbacks(callback: ActivityLifecycleCallbacks?) {
    super.registerActivityLifecycleCallbacks(callback)
  }

  override fun onActivityPaused(p0: Activity) {
    Log.d(TAG, "onActivityPaused at ${p0.localClassName}")
    currentActivity = p0
  }

  override fun onActivityStarted(p0: Activity) {
    Log.d(TAG, "onActivityStarted at ${p0.localClassName}")
  }

  override fun onActivityDestroyed(p0: Activity) {
    Log.d(TAG, "onActivityDestroyed at ${p0.localClassName}")
  }

  override fun onActivitySaveInstanceState(p0: Activity, p1: Bundle) {
    Log.d(TAG, "onActivitySaveInstanceState at ${p0.localClassName}")
  }

  override fun onActivityStopped(p0: Activity) {
    Log.d(TAG, "onActivityStopped at ${p0.localClassName}")
  }

  override fun onActivityCreated(p0: Activity, p1: Bundle?) {
    Log.d(TAG, "onActivityCreated at ${p0.localClassName}")
    lastCreated = p0
  }

  override fun onActivityResumed(p0: Activity) {
    Log.d(TAG, "onActivityResumed at ${p0.localClassName}")
  }

  companion object {
    private const val TAG = "LifecycleCallbacks"
  }
}
