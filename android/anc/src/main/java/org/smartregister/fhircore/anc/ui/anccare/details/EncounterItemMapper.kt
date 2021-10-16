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

import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.Encounter
import org.smartregister.fhircore.anc.data.sharedmodel.EncounterItem
import org.smartregister.fhircore.engine.data.domain.util.DomainMapper
import org.smartregister.fhircore.engine.util.DateUtils.getDate
import org.smartregister.fhircore.engine.util.DateUtils.makeItReadable

object EncounterItemMapper : DomainMapper<Encounter, EncounterItem> {

  override fun mapToDomainModel(dto: Encounter): EncounterItem {
    var type = CodeableConcept()
    var typeCoding = Coding()
    var typeString = ""
    var typeDate = ""
    if (dto.type != null && dto.type.isNotEmpty()) type = dto.type[0] as CodeableConcept
    if (type.hasCoding()) typeCoding = type.coding[0] as Coding
    if (typeCoding.hasDisplayElement()) typeString = typeCoding.display
    else if (type.hasText()) typeString = type.text
    if (type.hasText()) typeString = type.text
    if (dto.period.start != null) typeDate = dto.period.start.makeItReadable()
    return EncounterItem(dto.id, dto.status, typeString, typeDate.getDate("yyyy-MM-dd"))
  }
}
