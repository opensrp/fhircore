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

package org.smartregister.fhircore.anc.ui.anccare.shared

import android.content.Context
import com.google.android.fhir.logicalId
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import org.hl7.fhir.r4.model.CarePlan
import org.hl7.fhir.r4.model.Condition
import org.hl7.fhir.r4.model.Patient
import org.smartregister.fhircore.anc.data.model.PatientItem
import org.smartregister.fhircore.anc.data.model.VisitStatus
import org.smartregister.fhircore.engine.domain.util.DataMapper
import org.smartregister.fhircore.engine.util.extension.extractAddress
import org.smartregister.fhircore.engine.util.extension.extractFamilyName
import org.smartregister.fhircore.engine.util.extension.extractGender
import org.smartregister.fhircore.engine.util.extension.extractId
import org.smartregister.fhircore.engine.util.extension.extractName
import org.smartregister.fhircore.engine.util.extension.hasActivePregnancy
import org.smartregister.fhircore.engine.util.extension.milestonesDue
import org.smartregister.fhircore.engine.util.extension.milestonesOverdue

data class Anc(
  val patient: Patient,
  val head: Patient?,
  val conditions: List<Condition>? = null,
  val carePlans: List<CarePlan>? = null
)

class AncItemMapper @Inject constructor(@ApplicationContext val context: Context) :
  DataMapper<Anc, PatientItem> {

  private var itemMapperType = AncItemMapperType.REGISTER

  override fun transformInputToOutputModel(inputModel: Anc): PatientItem {
    val patient = inputModel.patient

    var visitStatus = VisitStatus.PLANNED
    if (inputModel.carePlans?.any { it.milestonesOverdue().isNotEmpty() } == true)
      visitStatus = VisitStatus.OVERDUE
    else if (inputModel.carePlans?.any { it.milestonesDue().isNotEmpty() } == true)
      visitStatus = VisitStatus.DUE

    return PatientItem(
      patientIdentifier = patient.logicalId,
      name = patient.extractName(),
      gender = patient.extractGender(context)?.first()?.toString() ?: "",
      birthDate = patient.birthDate,
      address = patient.extractAddress().ifEmpty { inputModel.head?.extractAddress() } ?: "",
      isPregnant = inputModel.conditions?.hasActivePregnancy(),
      visitStatus = visitStatus,
      familyName = patient.extractFamilyName(),
      headId = if (patient.hasLink()) patient.linkFirstRep.other.extractId() else null
    )
  }

  fun setAncItemMapperType(ancItemMapperType: AncItemMapperType) {
    this.itemMapperType = ancItemMapperType
  }

  enum class AncItemMapperType {
    REGISTER,
    DETAILS
  }
}
