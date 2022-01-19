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
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.datacapture.DataCaptureConfig
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.Resource
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.util.DispatcherProvider
import timber.log.Timber

@HiltAndroidApp
class QuestApplication : Application(), DataCaptureConfig.Provider {

  @Inject lateinit var referenceAttachmentResolver: ReferenceAttachmentResolver

  @Inject lateinit var fhirEngine: FhirEngine
  @Inject lateinit var dispatcherProvider: DispatcherProvider

  private lateinit var dataCaptureConfig: DataCaptureConfig

  override fun onCreate() {
    super.onCreate()
    if (BuildConfig.DEBUG) {
      Timber.plant(Timber.DebugTree())
    }

    dataCaptureConfig = DataCaptureConfig(attachmentResolver = referenceAttachmentResolver)

    loadLocalDevWfpCodaFiles()
  }

  fun loadLocalDevWfpCodaFiles() {

    val files =
      listOf(
        "fhir-questionnaires/CODA/anthro-following-visit.json",
        "fhir-questionnaires/CODA/assistance-visit.json",
        "fhir-questionnaires/CODA/coda-child-registration.json",
        "fhir-questionnaires/CODA/coda-child-structure-map.json",
      )

    files.forEach { fileName ->
      val jsonString = assets.open(fileName).bufferedReader().readText()
      val questionnaire =
        FhirContext.forR4().newJsonParser().parseResource(jsonString)

      GlobalScope.launch {
        DefaultRepository(fhirEngine, dispatcherProvider).addOrUpdate(questionnaire as Resource)
      }
    }
  }

  override fun getDataCaptureConfig(): DataCaptureConfig = dataCaptureConfig
}
