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
import ca.uhn.fhir.context.FhirContext
import com.google.android.fhir.datacapture.DataCaptureConfig
import com.google.android.fhir.datacapture.mapping.ResourceMapper
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

@HiltAndroidApp
class QuestApplication : Application(), DataCaptureConfig.Provider {

  @Inject lateinit var referenceAttachmentResolver: ReferenceAttachmentResolver
  private var configuration: DataCaptureConfig? = null

  override fun onCreate() {
    super.onCreate()
    if (BuildConfig.DEBUG) {
      Timber.plant(Timber.DebugTree())
    }

    CoroutineScope(Dispatchers.Default).launch {
      FhirContext.forR4Cached().apply {
        Timber.i("Loading FhirContext.forR4Cached on application init")
      }
      ResourceMapper.run { Timber.i("Loading ResourceMapper on application init") }
    }
  }

  override fun getDataCaptureConfig(): DataCaptureConfig {
    configuration =
      configuration ?: DataCaptureConfig(attachmentResolver = referenceAttachmentResolver)
    return configuration as DataCaptureConfig
  }
}
