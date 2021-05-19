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
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import org.smartregister.fhircore.R
import org.smartregister.fhircore.fragment.PatientListFragment
import org.smartregister.fhircore.util.Utils.getAgeFromDate
import org.smartregister.fhircore.viewmodel.PatientListViewModel

class PatientItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
  private val tvPatientDemographics: TextView = itemView.findViewById(R.id.tv_patient_demographics)
  private val tvDateLastSeen: TextView = itemView.findViewById(R.id.date_last_seen)
  private val tvRecordVaccine: TextView = itemView.findViewById(R.id.tv_record_vaccine)
  fun bindTo(
    patientItem: PatientListViewModel.PatientItem,
    onItemClicked: (PatientListFragment.Intention, PatientListViewModel.PatientItem) -> Unit,
    patientStatusObserver: (String, Observer<PatientListViewModel.PatientStatus>) -> Unit
  ) {
    setPatientStatus(null, patientItem, this.tvRecordVaccine, onItemClicked)
    this.tvPatientDemographics.text = getPatientDemographics(patientItem)
    this.tvPatientDemographics.setOnClickListener {
      onItemClicked(PatientListFragment.Intention.VIEW, patientItem)
    }
    this.tvDateLastSeen.setOnClickListener {
      onItemClicked(PatientListFragment.Intention.VIEW, patientItem)
    }

    patientStatusObserver(patientItem.logicalId) {
      setPatientStatus(it, patientItem, this.tvRecordVaccine, onItemClicked)
    }
  }

  private fun setPatientStatus(
    patientStatus: PatientListViewModel.PatientStatus?,
    patientItem: PatientListViewModel.PatientItem,
    tvRecordVaccine: TextView,
    onItemClicked: (PatientListFragment.Intention, PatientListViewModel.PatientItem) -> Unit,
  ) {
    tvRecordVaccine.text = ""
    val status = patientStatus?.status ?: return

    when (status) {
      PatientListViewModel.VaccineStatus.VACCINATED -> {
        tvRecordVaccine.text = "Vaccinated"
        tvRecordVaccine.setTextColor(
          ContextCompat.getColor(tvRecordVaccine.context, R.color.status_green)
        )
        tvRecordVaccine.setOnClickListener {}
      }
      PatientListViewModel.VaccineStatus.OVERDUE -> {
        tvRecordVaccine.text = "Overdue"
        tvRecordVaccine.setTextColor(
          ContextCompat.getColor(tvRecordVaccine.context, R.color.status_red)
        )
        tvRecordVaccine.setOnClickListener {
          onItemClicked(PatientListFragment.Intention.RECORD_VACCINE, patientItem)
        }
      }
      PatientListViewModel.VaccineStatus.PARTIAL -> {
        tvRecordVaccine.text = "Vaccine 1 \n " + patientStatus?.details
        tvRecordVaccine.setTextColor(
          ContextCompat.getColor(tvRecordVaccine.context, R.color.status_gray)
        )
        tvRecordVaccine.setOnClickListener {
          onItemClicked(PatientListFragment.Intention.RECORD_VACCINE, patientItem)
        }
      }
      PatientListViewModel.VaccineStatus.DUE -> {
        tvRecordVaccine.text = "Record \n Vaccine"
        tvRecordVaccine.setTextColor(
          ContextCompat.getColor(tvRecordVaccine.context, R.color.status_blue)
        )
        tvRecordVaccine.setOnClickListener {
          onItemClicked(PatientListFragment.Intention.RECORD_VACCINE, patientItem)
        }
      }
    }
  }

  private fun getPatientDemographics(patientItem: PatientListViewModel.PatientItem): String {
    val age = getAgeFromDate(patientItem.dob)
    val names = patientItem.name.split(' ')
    val gender = if (patientItem.gender == "male") 'M' else 'F'
    return listOf(names[1], names[0], gender, "$age").joinToString()
  }
}
