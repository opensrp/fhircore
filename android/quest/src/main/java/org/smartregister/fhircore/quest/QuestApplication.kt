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
import ca.uhn.fhir.context.support.DefaultProfileValidationSupport
import com.google.android.fhir.datacapture.DataCaptureConfig
import com.google.android.fhir.datacapture.mapping.ResourceMapper
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.hapi.ctx.HapiWorkerContext
import org.hl7.fhir.r4.utils.FHIRPathEngine
import org.smartregister.fhircore.engine.data.remote.fhir.resource.ReferenceAttachmentResolver
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

    configureSingletonDefaultValidationSupport()
  }

  override fun getDataCaptureConfig(): DataCaptureConfig {
    configuration =
      configuration ?: DataCaptureConfig(attachmentResolver = referenceAttachmentResolver)
    return configuration as DataCaptureConfig
  }

  // TODO https://github.com/google/android-fhir/issues/1173
  // Action: Remove once fixed
  // Once SDK resolves the issue this can be removed from here as there would be no
  // duplication of objects
  private fun configureSingletonDefaultValidationSupport() {

    CoroutineScope(Dispatchers.Default).launch {
      val fhirContext =
        FhirContext.forR4Cached().apply {
          Timber.i("Loading FhirContext.forR4Cached on application init")
        }
      ResourceMapper.run {
        val validationSupport = extractResourceMapperValidationSupport()
        fhirContext.validationSupport = validationSupport
        Timber.i("Loading ResourceMapper on application init")
      }
    }
  }

  // TODO https://github.com/google/android-fhir/issues/1173
  // Action: Remove once fixed
  private fun extractResourceMapperValidationSupport() =
    ResourceMapper::class
      .java
      .getDeclaredField("fhirPathEngine")
      .also { fhirPathEngineProperty -> fhirPathEngineProperty.isAccessible = true }
      .get(null)
      .let { fhirPathEngine ->
        ((fhirPathEngine as FHIRPathEngine).worker as HapiWorkerContext).let { hapiWorkerContext ->
          hapiWorkerContext
            .javaClass
            .getDeclaredField("myValidationSupport")
            .also { myValidationSupportProperty -> myValidationSupportProperty.isAccessible = true }
            .get(hapiWorkerContext) as
            DefaultProfileValidationSupport
        }
      }
}
