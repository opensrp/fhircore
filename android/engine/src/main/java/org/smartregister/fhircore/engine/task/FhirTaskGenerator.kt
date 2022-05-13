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

package org.smartregister.fhircore.engine.task

import com.google.android.fhir.FhirEngine
import com.google.android.fhir.get
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.StructureMap
import org.hl7.fhir.r4.model.Task
import org.hl7.fhir.r4.utils.StructureMapUtilities
import org.smartregister.fhircore.engine.util.extension.encodeResourceToString
import org.smartregister.fhircore.engine.util.helper.TransformSupportServices
import timber.log.Timber

@Singleton
class FhirTaskGenerator
@Inject
constructor(val fhirEngine: FhirEngine, val transformSupportServices: TransformSupportServices) {
  val structureMapUtilities by lazy {
    StructureMapUtilities(transformSupportServices.simpleWorkerContext, transformSupportServices)
  }

  suspend fun generateCarePlan(structureMapId: String, patient: Patient): Bundle {
    val structureMap = fhirEngine.get<StructureMap>(structureMapId)

    return Bundle()
      .apply {
        structureMapUtilities.transform(
          transformSupportServices.simpleWorkerContext,
          patient,
          structureMap,
          this
        )
      }
      .also { it.entry.forEach { entryComponent -> fhirEngine.create(entryComponent.resource) } }
      .also { Timber.i(it.encodeResourceToString()) }
  }

  suspend fun completeTask(id: String) {
    fhirEngine.run {
      create(
        get<Task>(id).apply {
          this.status = Task.TaskStatus.COMPLETED
          this.lastModified = Date()
        }
      )
    }
  }
}
