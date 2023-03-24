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

import android.app.Application
import android.content.Intent
import android.database.CursorWindow
import android.os.Looper
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.google.android.fhir.datacapture.DataCaptureConfig
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import org.smartregister.fhircore.engine.data.remote.fhir.resource.ReferenceUrlResolver
import org.smartregister.fhircore.engine.util.extension.showToast
import org.smartregister.fhircore.quest.data.QuestXFhirQueryResolver
import org.smartregister.fhircore.quest.ui.appsetting.AppSettingActivity
import org.smartregister.fhircore.quest.ui.questionnaire.QuestionnaireItemViewHolderFactoryMatchersProviderFactoryImpl
import timber.log.Timber

@HiltAndroidApp
class QuestApplication : Application(), DataCaptureConfig.Provider, Configuration.Provider {

  @Inject lateinit var workerFactory: HiltWorkerFactory

  @Inject lateinit var referenceUrlResolver: ReferenceUrlResolver

  @Inject lateinit var xFhirQueryResolver: QuestXFhirQueryResolver

  private var configuration: DataCaptureConfig? = null

  override fun onCreate() {
    super.onCreate()
    if (BuildConfig.DEBUG) {
      Timber.plant(Timber.DebugTree())
    }

    if (BuildConfig.DEBUG.not()) {
      Thread.setDefaultUncaughtExceptionHandler(globalExceptionHandler)
    }

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

  /**
   * This method captures all uncaught exceptions in the app and redirects to the Launch Page in the
   * case that the exception was thrown on the main thread This will therefore prevent any app
   * crashes so we need some more handling for reporting the errors once we have a crash manager
   * installed
   *
   * TODO add crash reporting when a crash reporting tool is selected e.g. Fabric Crashlytics or
   * Sentry
   */
  private fun handleUncaughtException(e: Throwable) {
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
