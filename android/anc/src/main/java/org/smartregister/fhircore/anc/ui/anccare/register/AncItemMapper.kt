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

package org.smartregister.fhircore.anc.ui.anccare.register

import com.google.android.fhir.logicalId
import org.hl7.fhir.r4.model.Patient
import org.smartregister.fhircore.anc.AncApplication
import org.smartregister.fhircore.anc.data.model.AncItem
import org.smartregister.fhircore.engine.data.domain.util.DomainMapper
import org.smartregister.fhircore.engine.util.extension.atRisk
import org.smartregister.fhircore.engine.util.extension.extractAge
import org.smartregister.fhircore.engine.util.extension.extractGender
import org.smartregister.fhircore.engine.util.extension.extractName

object AncItemMapper : DomainMapper<Patient, AncItem> {
  private const val RISK = "risk"

  override fun mapToDomainModel(dto: Patient): AncItem {
    val name = dto.extractName()
    val gender = dto.extractGender(AncApplication.getContext()).first()
    val age = dto.extractAge()
    return AncItem(
      patientIdentifier = dto.logicalId,
      name = name,
      gender = gender.toString(),
      age = age,
      demographics = "$name, $gender, $age",
      atRisk = dto.atRisk(RISK)
    )
  }

  override fun mapFromDomainModel(domainModel: AncItem): Patient {
    throw UnsupportedOperationException()
  }
}
