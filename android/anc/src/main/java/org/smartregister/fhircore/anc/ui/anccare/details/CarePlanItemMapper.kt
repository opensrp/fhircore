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

import org.hl7.fhir.r4.model.CarePlan
import org.smartregister.fhircore.anc.data.sharedmodel.CarePlanItem
import org.smartregister.fhircore.anc.data.sharedmodel.UpcomingServiceItem
import org.smartregister.fhircore.engine.data.domain.util.DomainMapper
import org.smartregister.fhircore.engine.util.DateUtils.makeItReadable
import org.smartregister.fhircore.engine.util.extension.due
import org.smartregister.fhircore.engine.util.extension.overdue

// upcoming services

object CarePlanItemMapper : DomainMapper<CarePlan, CarePlanItem> {

  override fun mapToDomainModel(dto: CarePlan): CarePlanItem {
    var typeString = ""
    for (j in dto.activity.indices) {
      if (dto.activity[j].hasDetail())
        if (dto.activity[j].detail.hasDescription()) typeString = dto.activity[j].detail.description
    }
    return CarePlanItem(
      carePlanIdentifier = dto.id,
      title = typeString,
      due = dto.due(),
      overdue = dto.overdue()
    )
  }

  fun mapToUpcomingServiceItem(dto: CarePlan): UpcomingServiceItem {
    var typeString = ""
    var dateString = ""
    for (j in dto.activity.indices) {
      if (dto.activity[j].hasDetail())
        if (dto.activity[j].detail.hasDescription()) typeString = dto.activity[j].detail.description
      if (dto.activity[j].detail.hasScheduledPeriod())
        dateString = dto.activity[j].detail.scheduledPeriod.start.makeItReadable()
    }
    return UpcomingServiceItem(dto.id, typeString, dateString)
  }
}
