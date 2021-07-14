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

package org.smartregister.fhircore.data

import android.content.Context
import androidx.work.WorkerParameters
import ca.uhn.fhir.context.FhirContext
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.sync.DataSource
import com.google.android.fhir.sync.FhirSyncWorker
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.FhirApplication
import org.smartregister.fhircore.api.HapiFhirService

class FhirPeriodicSyncWorker(appContext: Context, workerParams: WorkerParameters) :
  FhirSyncWorker(appContext, workerParams) {
  override fun getDataSource(): DataSource {
    return HapiFhirResourceDataSource(
      HapiFhirService.create(FhirContext.forR4().newJsonParser(), applicationContext)
    )
  }

  override fun getFhirEngine(): FhirEngine {
    return FhirApplication.fhirEngine(applicationContext)
  }

  override fun getSyncData() =
    mapOf(
      ResourceType.Patient to emptyMap<String, String>(),
      ResourceType.StructureMap to mapOf(),
      ResourceType.RelatedPerson to mapOf()
    )
}
