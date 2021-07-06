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

import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.smartregister.fhircore.FhirApplication
import org.smartregister.fhircore.R
import org.smartregister.fhircore.RobolectricTest
import org.smartregister.fhircore.viewmodel.PatientListViewModel

class PatientItemViewHolderTest : RobolectricTest() {

  private lateinit var viewHolder: PatientItemViewHolder
  private lateinit var itemView: View

  @Before
  fun setUp() {
    itemView =
      LayoutInflater.from(FhirApplication.getContext())
        .inflate(R.layout.patient_list_item, null, false)
    viewHolder = PatientItemViewHolder(itemView)
  }

  @Test
  fun testVerifyPatientItemData() {

    val itemClickListener = { item: PatientListViewModel.PatientItem -> verifyPatient(item) }

    val recordVaccineClickListener = { item: PatientListViewModel.PatientItem ->
      verifyPatient(item)
    }

    viewHolder.bindTo(
      PatientListViewModel.PatientItem("1", "Mc Jane", "male", "2000-01-01", "", "1234567", "2"),
      itemClickListener,
      recordVaccineClickListener
    )

    val tvPatientDemographics = itemView.findViewById<TextView>(R.id.tv_patient_demographics)
    val tvDateLastSeen = itemView.findViewById<TextView>(R.id.date_last_seen)
    val tvRecordVaccine = itemView.findViewById<TextView>(R.id.tv_record_vaccine)

    tvPatientDemographics.performClick()
    tvDateLastSeen.performClick()
    tvRecordVaccine.performClick()

    Assert.assertEquals("Jane, Mc, M, 21", tvPatientDemographics.text.toString())
  }

  private fun verifyPatient(patientItem: PatientListViewModel.PatientItem) {
    Assert.assertEquals("1", patientItem.id)
    Assert.assertEquals("Mc Jane", patientItem.name)
    Assert.assertEquals("male", patientItem.gender)
    Assert.assertEquals("", patientItem.html)
    Assert.assertEquals("1234567", patientItem.phone)
    Assert.assertEquals("2", patientItem.logicalId)
  }
}
