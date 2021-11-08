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

package org.smartregister.fhircore.anc.ui.family.register

import com.google.android.fhir.logicalId
import org.hl7.fhir.r4.model.CarePlan
import org.hl7.fhir.r4.model.Patient
import org.smartregister.fhircore.anc.AncApplication
import org.smartregister.fhircore.anc.data.report.model.ReportItem
import org.smartregister.fhircore.engine.data.domain.util.DomainMapper
import org.smartregister.fhircore.engine.util.extension.extractAge
import org.smartregister.fhircore.engine.util.extension.extractGender
import org.smartregister.fhircore.engine.util.extension.extractName

data class Report(val head: Patient, val members: List<Patient>, val servicesDue: List<CarePlan>)

object ReportsItemMapper : DomainMapper<Report, ReportItem> {

  override fun mapToDomainModel(dto: Report): ReportItem {
    val head = dto.head

    return ReportItem(
      id = head.logicalId,
      title = head.extractName(),
      description =
        (head.extractGender(AncApplication.getContext())?.firstOrNull() ?: "").toString(),
      reportType = head.extractAge()
    )
  }
}
