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

package org.smartregister.fhircore.engine.data.local.patient.dao.register.anc

import com.google.android.fhir.logicalId
import org.smartregister.fhircore.engine.data.local.patient.dao.register.DefaultPatientRegisterDao.Companion.OFFICIAL_IDENTIFIER
import org.smartregister.fhircore.engine.domain.model.RegisterData
import org.smartregister.fhircore.engine.domain.model.VisitStatus
import org.smartregister.fhircore.engine.domain.util.DataMapper
import org.smartregister.fhircore.engine.util.extension.extractAddress
import org.smartregister.fhircore.engine.util.extension.extractName
import org.smartregister.fhircore.engine.util.extension.milestonesDue
import org.smartregister.fhircore.engine.util.extension.milestonesOverdue
import org.smartregister.fhircore.engine.util.extension.toAgeDisplay

object AncRegisterDataMapper : DataMapper<Anc, RegisterData.AncRegisterData> {

  override fun transformInputToOutputModel(inputModel: Anc): RegisterData.AncRegisterData {
    val patient = inputModel.patient

    var visitStatus = VisitStatus.PLANNED
    if (inputModel.carePlans.any { it.milestonesOverdue().isNotEmpty() })
      visitStatus = VisitStatus.OVERDUE
    else if (inputModel.carePlans.any { it.milestonesDue().isNotEmpty() })
      visitStatus = VisitStatus.DUE

    return RegisterData.AncRegisterData(
      id = patient.logicalId,
      name = patient.extractName(),
      identifier =
        patient.identifier.firstOrNull { it.use.name.contentEquals(OFFICIAL_IDENTIFIER) }?.value,
      age = patient.birthDate.toAgeDisplay(),
      address = patient.extractAddress(),
      visitStatus = visitStatus,
      servicesDue = inputModel.carePlans.sumOf { it.milestonesDue().size },
      servicesOverdue = inputModel.carePlans.sumOf { it.milestonesOverdue().size }
    )
  }
}
