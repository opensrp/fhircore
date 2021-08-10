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

package org.smartregister.fhircore.viewholder

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.smartregister.fhircore.R
import org.smartregister.fhircore.model.PatientDetailsCard
import org.smartregister.fhircore.util.Utils

class PatientDetailsCardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
  private val cardTitle: TextView = itemView.findViewById(R.id.card_title)
  private val cardDetails: TextView = itemView.findViewById(R.id.card_details)

  fun bindTo(patientDetailsCard: PatientDetailsCard) {
    this.cardTitle.text = patientDetailsCard.title

    this.cardDetails.visibility =
      if (patientDetailsCard.details.isBlank()) View.GONE else View.VISIBLE
    this.cardDetails.text = patientDetailsCard.details

    if (patientDetailsCard.details.contains("overdue"))
      Utils.setTextColor(this.cardDetails, R.color.status_red)
  }
}
