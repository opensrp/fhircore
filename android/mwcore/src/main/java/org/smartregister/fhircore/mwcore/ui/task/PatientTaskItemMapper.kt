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

package org.smartregister.fhircore.mwcore.ui.task

import android.content.Context
import com.google.android.fhir.logicalId
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Task
import org.smartregister.fhircore.engine.data.domain.util.DomainMapper
import org.smartregister.fhircore.engine.util.DateUtils
import org.smartregister.fhircore.engine.util.extension.extractAddress
import org.smartregister.fhircore.engine.util.extension.extractGender
import org.smartregister.fhircore.engine.util.extension.extractName
import org.smartregister.fhircore.mwcore.data.task.model.PatientTaskItem

data class PatientTask(val patient: Patient, val task: Task)

class PatientTaskItemMapper
@Inject
constructor(
  @ApplicationContext val context: Context,
) : DomainMapper<PatientTask, PatientTaskItem> {

  override fun mapToDomainModel(dto: PatientTask): PatientTaskItem {
    val patient = dto.patient
    val task = dto.task

    return PatientTaskItem(
      id = task.logicalId,
      name = patient.extractName(),
      gender = (patient.extractGender(context)?.firstOrNull() ?: "").toString(),
      birthdate = patient.birthDate,
      address = patient.extractAddress(),
      description = task.description,
      overdue = DateUtils.hasPastDays(task.executionPeriod.endElement)
    )
  }
}
