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
import org.smartregister.fhircore.anc.data.model.CarePlanItem
import org.smartregister.fhircore.engine.domain.util.DataMapper
import org.smartregister.fhircore.engine.util.extension.due
import org.smartregister.fhircore.engine.util.extension.overdue

object CarePlanItemMapper : DataMapper<CarePlan, CarePlanItem> {

  override fun transformInputToOutputModel(inputModel: CarePlan): CarePlanItem {
    var typeString = ""
    for (j in inputModel.activity.indices) {
      if (inputModel.activity[j].hasDetail())
        if (inputModel.activity[j].detail.hasDescription())
          typeString = inputModel.activity[j].detail.description
    }
    return CarePlanItem(
      carePlanIdentifier = inputModel.id,
      title = typeString,
      due = inputModel.due(),
      overdue = inputModel.overdue()
    )
  }
}
