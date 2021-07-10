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
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import org.smartregister.fhircore.R
import org.smartregister.fhircore.fragment.PatientListFragment
import org.smartregister.fhircore.util.Utils.getAgeFromDate
import org.smartregister.fhircore.viewmodel.PatientListViewModel

class PatientItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
  private val tvPatientDemographics: TextView = itemView.findViewById(R.id.tv_patient_demographics)
  private val tvRecordVaccine: TextView = itemView.findViewById(R.id.tv_record_vaccine)
  private val atRisk: TextView = itemView.findViewById(R.id.risk_flag)

  fun bindTo(
    patientItem: PatientListViewModel.PatientItem,
    onItemClicked: (PatientListFragment.Intention, PatientListViewModel.PatientItem) -> Unit
  ) {
    setPatientStatus(null, patientItem, this.tvRecordVaccine, onItemClicked)
    this.tvPatientDemographics.text = getPatientDemographics(patientItem)
    this.itemView.setOnClickListener {
      onItemClicked(PatientListFragment.Intention.VIEW, patientItem)
    }
    this.atRisk.text = patientItem.risk
    this.atRisk.visibility = if (patientItem.risk.isNullOrEmpty()) View.INVISIBLE else View.VISIBLE

    setPatientStatus(patientItem.vaccineStatus, patientItem, this.tvRecordVaccine, onItemClicked)
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
        tvRecordVaccine.text = tvRecordVaccine.context.getString(R.string.status_vaccinated)
        tvRecordVaccine.setTextColor(
          ContextCompat.getColor(tvRecordVaccine.context, R.color.status_green)
        )
        tvRecordVaccine.setOnClickListener {}
      }
      PatientListViewModel.VaccineStatus.OVERDUE -> {
        tvRecordVaccine.text = tvRecordVaccine.context.getString(R.string.status_overdue)
        tvRecordVaccine.setTextColor(
          ContextCompat.getColor(tvRecordVaccine.context, R.color.status_red)
        )
        tvRecordVaccine.setOnClickListener {
          onItemClicked(PatientListFragment.Intention.RECORD_VACCINE, patientItem)
        }
      }
      PatientListViewModel.VaccineStatus.PARTIAL -> {
        tvRecordVaccine.text =
          tvRecordVaccine.context.getString(
            R.string.status_received_vaccine,
            1,
            patientStatus.details
          )
        tvRecordVaccine.setTextColor(
          ContextCompat.getColor(tvRecordVaccine.context, R.color.status_gray)
        )
        tvRecordVaccine.setOnClickListener {
          onItemClicked(PatientListFragment.Intention.RECORD_VACCINE, patientItem)
        }
      }
      PatientListViewModel.VaccineStatus.DUE -> {
        tvRecordVaccine.text = tvRecordVaccine.context.getString(R.string.status_record_vaccine)
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
