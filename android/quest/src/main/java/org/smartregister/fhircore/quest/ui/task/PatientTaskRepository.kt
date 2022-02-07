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

package org.smartregister.fhircore.quest.ui.task

import android.content.Context
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.search.count
import com.google.android.fhir.search.search
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.withContext
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Task
import org.smartregister.fhircore.engine.data.domain.util.RegisterRepository
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.quest.util.filterByPractitioner

class PatientTaskRepository
@Inject
constructor(
  @ApplicationContext val context: Context,
  override val fhirEngine: FhirEngine,
  override val domainMapper: PatientTaskItemMapper,
  private val dispatcherProvider: DispatcherProvider
) : RegisterRepository<PatientTask, PatientTaskItem> {

  override suspend fun loadData(
    query: String,
    pageNumber: Int,
    loadAll: Boolean
  ): List<PatientTaskItem> {
    return withContext(dispatcherProvider.io()) {
      val tasks = fhirEngine.search<Task> { filterByPractitioner(Task.OWNER, "6744") }

      tasks.map { task ->
        val patientId = task.`for`.reference.replace("Patient/", "")
        val patient = fhirEngine.load(Patient::class.java, patientId)

        domainMapper.mapToDomainModel(PatientTask(patient, task))
      }
    }
  }

  override suspend fun countAll(): Long {
    return fhirEngine.count<Task> { filterByPractitioner(Task.OWNER, "6744") }
  }
}
