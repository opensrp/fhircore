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

class ObservationItemViewHolderTest : RobolectricTest() {

  private lateinit var viewHolder: ObservationItemViewHolder
  private lateinit var itemView: View

  @Before
  fun setUp() {
    itemView =
      LayoutInflater.from(FhirApplication.getContext())
        .inflate(R.layout.observation_list_item, null, false)
    viewHolder = ObservationItemViewHolder(itemView)
  }

  @Test
  fun testVerifyObservationDetailText() {
    viewHolder.bindTo(PatientListViewModel.ObservationItem("1", "first", "second", "third"))
    val observationDetail = itemView.findViewById<TextView>(R.id.observation_detail)

    Assert.assertEquals("first: third\nEffective: second", observationDetail.text.toString())
  }
}
