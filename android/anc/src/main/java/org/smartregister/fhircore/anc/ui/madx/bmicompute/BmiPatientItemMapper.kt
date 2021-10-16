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

package org.smartregister.fhircore.anc.ui.madx.bmicompute

import com.google.android.fhir.logicalId
import org.hl7.fhir.r4.model.Patient
import org.smartregister.fhircore.anc.data.sharedmodel.PatientBMIItem
import org.smartregister.fhircore.engine.data.domain.util.DomainMapper
import org.smartregister.fhircore.engine.util.extension.extractHeight
import org.smartregister.fhircore.engine.util.extension.extractName
import org.smartregister.fhircore.engine.util.extension.extractWeight

object BmiPatientItemMapper : DomainMapper<Patient, PatientBMIItem> {

  override fun mapToDomainModel(dto: Patient): PatientBMIItem {
    val name = dto.extractName()
    val height = dto.extractHeight()
    val weight = dto.extractWeight()
    return PatientBMIItem(
      patientIdentifier = dto.logicalId,
      name = name,
      height = height,
      weight = weight
    )
  }
}
