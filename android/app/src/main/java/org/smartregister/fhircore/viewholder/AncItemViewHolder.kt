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
import org.smartregister.fhircore.model.PatientItem
import org.smartregister.fhircore.util.Utils.getPatientAgeGender

class AncItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
  private val demographics: TextView = itemView.findViewById(R.id.tv_patient_demographics)
  private val area: TextView = itemView.findViewById(R.id.tv_area)

  fun bindTo(patientItem: PatientItem, onItemClicked: (PatientItem) -> Unit) {
    this.demographics.text = getPatientDemographics(patientItem)
    this.area.text = patientItem.area
  }

  private fun getPatientDemographics(patientItem: PatientItem): String {
    val (age, gender) = getPatientAgeGender(patientItem)
    val names = patientItem.name.split(' ')
    return listOf(names[1], names[0], gender, "$age").joinToString()
  }
}
