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
import org.smartregister.fhircore.engine.p2p.dao.P2PReceiverTransferDao
import org.smartregister.fhircore.engine.p2p.dao.P2PSenderTransferDao
import org.smartregister.p2p.P2PLibrary
import timber.log.Timber

@HiltAndroidApp
class QuestApplication : Application(), DataCaptureConfig.Provider {

  @Inject lateinit var referenceAttachmentResolver: ReferenceAttachmentResolver
  @Inject lateinit var pSenderTransferDao: P2PSenderTransferDao
  @Inject lateinit var p2PReceiverTransferDao: P2PReceiverTransferDao
  private var configuration: DataCaptureConfig? = null

  override fun onCreate() {
    super.onCreate()
    if (BuildConfig.DEBUG) {
      Timber.plant(Timber.DebugTree())
    }
    CoroutineScope(Dispatchers.Default).launch {
      val fhirContext =
        FhirContext.forR4Cached().apply {
          Timber.i("Loading FhirContext.forR4Cached on application init")
        }
      ResourceMapper.run {
        // TODO https://github.com/google/android-fhir/issues/1173
        // Action: Remove once fixed
        // Once SDK resolves the issue this can be removed from here as there would be no
        // duplication of objects
        val validationSupport = extractResourceMapperValidationSupport()
        fhirContext.validationSupport = validationSupport

        Timber.i("Loading ResourceMapper on application init")
      }

    }

    // Init P2PLibrary
    val p2POptions = P2PLibrary.Options(context = this,
      dbPassphrase = "demo",
      username = "demo",
      senderTransferDao = pSenderTransferDao,
      receiverTransferDao = p2PReceiverTransferDao
    )
    P2PLibrary().init(p2POptions)
  }

  // TODO https://github.com/google/android-fhir/issues/1173
  // Action: Remove once fixed
  private fun extractResourceMapperValidationSupport() =
    ResourceMapper::class
      .java
      .getDeclaredField("fhirPathEngine")
      .also { it.isAccessible = true }
      .get(null)
      .let {
        ((it as FHIRPathEngine).worker as HapiWorkerContext).let {
          it.javaClass
            .getDeclaredField("myValidationSupport")
            .also { it.isAccessible = true }
            .get(it) as
            DefaultProfileValidationSupport
        }
      }

  override fun getDataCaptureConfig(): DataCaptureConfig {
    configuration =
      configuration ?: DataCaptureConfig(attachmentResolver = referenceAttachmentResolver)
    return configuration as DataCaptureConfig
  }
}
