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

package org.smartregister.fhircore.anc.ui.family.details.viewholder

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.hl7.fhir.r4.model.Encounter
import org.smartregister.fhircore.anc.R
import java.text.SimpleDateFormat

class FamilyEncounterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
  private val visitName: TextView = itemView.findViewById(R.id.visit_name)
  private val visitDate: TextView = itemView.findViewById(R.id.visit_date)

  fun bindTo(
    familyEncounter: Encounter,
    onItemClicked: (Encounter) -> Unit,
  ) {

    visitName.text = familyEncounter.class_?.display ?: ""
    visitDate.text = SimpleDateFormat.getDateInstance().format(familyEncounter.period.start)
    itemView.setOnClickListener { onItemClicked(familyEncounter) }
  }
}
