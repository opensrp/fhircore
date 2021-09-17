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

package org.smartregister.fhircore.anc

import androidx.work.WorkerParameters
import com.google.android.fhir.search.search
import com.google.android.fhir.sync.FhirSyncWorker
import org.hl7.fhir.r4.model.Patient
import org.smartregister.fhircore.anc.sdk.extractExtendedPatient
import org.smartregister.fhircore.engine.data.remote.fhir.resource.FhirResourceDataSource

class AncFhirSyncWorker(workerParams: WorkerParameters) :
  FhirSyncWorker(AncApplication.getContext(), workerParams) {

  override fun getSyncData() = AncApplication.getContext().resourceSyncParams

  override fun getDataSource() = FhirResourceDataSource.getInstance(AncApplication.getContext())

  override fun getFhirEngine() = AncApplication.getContext().fhirEngine

  override suspend fun doWork(): Result {
    val result = super.doWork()

    forcePatientIndexes()

    return result
  }

  companion object {
    // todo remove this once we have following done
    // https://github.com/google/android-fhir/issues/735
    // this is to reindex patients for tags for family, and anc
    suspend fun forcePatientIndexes() {
      val engine = AncApplication.getContext().fhirEngine
      engine.search<Patient> { filter(Patient.ACTIVE, true) }.forEach {
        if (it.meta.hasTag() &&
            (it.meta.tagFirstRep.display.contains("Family") ||
              it.meta.tagFirstRep.display.contains("Pregnant"))
        ) {
          val extendedPatient = it.extractExtendedPatient()
          engine.save(extendedPatient)
        }
      }
    }
  }
}
