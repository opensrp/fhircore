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

package org.smartregister.fhircore.anc.ui.anccare.details

import android.content.Context
import com.google.android.fhir.logicalId
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import org.smartregister.fhircore.anc.data.model.PatientItem
import org.smartregister.fhircore.anc.ui.anccare.register.Anc
import org.smartregister.fhircore.engine.data.domain.util.DomainMapper
import org.smartregister.fhircore.engine.util.extension.extractAge
import org.smartregister.fhircore.engine.util.extension.extractGender
import org.smartregister.fhircore.engine.util.extension.extractName
import org.smartregister.fhircore.engine.util.extension.isPregnant

class AncPatientItemMapper @Inject constructor(@ApplicationContext val context: Context) :
  DomainMapper<Anc, PatientItem> {

  override fun mapToDomainModel(dto: Anc): PatientItem {
    val patient = dto.patient
    val name = patient.extractName()
    val isPregnant = patient.isPregnant()
    val gender = patient.extractGender(context)?.first() ?: ""
    val age = patient.extractAge()
    return PatientItem(
      patientIdentifier = patient.logicalId,
      name = name,
      gender = gender.toString(),
      age = age,
      isPregnant = isPregnant,
      demographics = "$name, $gender, $age",
    )
  }
}
