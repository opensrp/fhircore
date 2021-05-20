/*
 * Copyright 2021 Ona Systems Inc
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

package org.smartregister.fhircore.viewholder

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.hl7.fhir.r4.model.Immunization
import org.hl7.fhir.r4.model.PositiveIntType
import org.smartregister.fhircore.R
import org.smartregister.fhircore.util.Utils

class ImmunizationItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
  private val observationTextView: TextView = itemView.findViewById(R.id.observation_detail)
  private val viewObservationDetailTextView: TextView =
    itemView.findViewById(R.id.view_observation_detail)

  fun bindTo(immunizationItem: Immunization) {
    val doseNumber = (immunizationItem.protocolApplied[0].doseNumber as PositiveIntType).value
    val nextDoseNumber = doseNumber + 1
    val vaccineDate = immunizationItem.occurrenceDateTimeType.toHumanDisplay()
    val nextVaccineDate = Utils.addDays(vaccineDate, 28)
    this.observationTextView.text =
      itemView.resources.getString(
        R.string.immunization_brief_text,
        immunizationItem.vaccineCode.text,
        doseNumber
      )

    this.viewObservationDetailTextView.text =
      itemView.resources.getString(
        R.string.immunization_next_dose_text,
        nextDoseNumber,
        nextVaccineDate
      )
  }
}
