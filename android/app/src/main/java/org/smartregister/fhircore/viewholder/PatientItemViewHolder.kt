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
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import org.smartregister.fhircore.R
import org.smartregister.fhircore.fragment.PatientListFragment
import org.smartregister.fhircore.model.PatientItem
import org.smartregister.fhircore.model.PatientStatus
import org.smartregister.fhircore.model.VaccineStatus
import org.smartregister.fhircore.util.Utils
import org.smartregister.fhircore.util.Utils.getPatientAgeGender

class PatientItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
  private val tvPatientDemographics: TextView = itemView.findViewById(R.id.tv_patient_demographics)
  private val tvLastSeen: TextView = itemView.findViewById(R.id.date_last_seen)
  private val tvRecordVaccine: TextView = itemView.findViewById(R.id.tv_record_vaccine)
  private val contVaccineStatus: LinearLayout = itemView.findViewById(R.id.cont_vaccine_status)
  private val imgVaccineStatus: ImageView = itemView.findViewById(R.id.img_vaccine_status)

  private val atRisk: TextView = itemView.findViewById(R.id.risk_flag)

  fun bindTo(
    patientItem: PatientItem,
    onItemClicked: (PatientListFragment.Intention, PatientItem) -> Unit
  ) {
    setPatientStatus(null, patientItem, this.tvRecordVaccine, onItemClicked)
    this.tvPatientDemographics.text = getPatientDemographics(patientItem)
    this.tvLastSeen.text = patientItem.lastSeen
    this.itemView.setOnClickListener {
      onItemClicked(PatientListFragment.Intention.VIEW, patientItem)
    }
    this.atRisk.text = patientItem.risk
    this.atRisk.visibility = if (patientItem.risk.isNullOrEmpty()) View.INVISIBLE else View.VISIBLE

    setPatientStatus(patientItem.vaccineStatus, patientItem, this.tvRecordVaccine, onItemClicked)
  }

  private fun setPatientStatus(
    patientStatus: PatientStatus?,
    patientItem: PatientItem,
    tvRecordVaccine: TextView,
    onItemClicked: (PatientListFragment.Intention, PatientItem) -> Unit,
  ) {
    tvRecordVaccine.text = ""
    val status = patientStatus?.status ?: return

    when (status) {
      VaccineStatus.VACCINATED -> {
        tvRecordVaccine.text = tvRecordVaccine.context.getString(R.string.status_vaccinated)
        tvRecordVaccine.setTextColor(
          ContextCompat.getColor(tvRecordVaccine.context, R.color.status_green)
        )
        Utils.setBgColor(contVaccineStatus, R.color.white)
        imgVaccineStatus.visibility = View.VISIBLE
        tvRecordVaccine.setOnClickListener {}
      }
      VaccineStatus.OVERDUE -> {
        tvRecordVaccine.text = tvRecordVaccine.context.getString(R.string.status_overdue)
        tvRecordVaccine.setTextColor(ContextCompat.getColor(tvRecordVaccine.context, R.color.white))
        Utils.setBgColor(contVaccineStatus, R.color.status_red)
        imgVaccineStatus.visibility = View.GONE
        tvRecordVaccine.setOnClickListener {
          onItemClicked(PatientListFragment.Intention.RECORD_VACCINE, patientItem)
        }
      }
      VaccineStatus.PARTIAL -> {
        tvRecordVaccine.text =
          tvRecordVaccine.context.getString(
            R.string.status_received_vaccine,
            1,
            patientStatus.details
          )
        tvRecordVaccine.setTextColor(
          ContextCompat.getColor(tvRecordVaccine.context, R.color.status_gray)
        )
        Utils.setBgColor(contVaccineStatus, R.color.white)
        imgVaccineStatus.visibility = View.GONE
        tvRecordVaccine.setOnClickListener {
          onItemClicked(PatientListFragment.Intention.RECORD_VACCINE, patientItem)
        }
      }
      VaccineStatus.DUE -> {
        tvRecordVaccine.text = tvRecordVaccine.context.getString(R.string.record_vaccine_nl)
        tvRecordVaccine.setTextColor(
          ContextCompat.getColor(tvRecordVaccine.context, R.color.status_blue)
        )
        Utils.setBgColor(contVaccineStatus, R.color.white)
        imgVaccineStatus.visibility = View.GONE
        tvRecordVaccine.setOnClickListener {
          onItemClicked(PatientListFragment.Intention.RECORD_VACCINE, patientItem)
        }
      }
    }
  }

  private fun getPatientDemographics(patientItem: PatientItem): String {
    val (age, gender) = getPatientAgeGender(patientItem)
    val names = patientItem.name.split(' ')
    return listOf(names[1], names[0], gender, "$age").joinToString()
  }
}
