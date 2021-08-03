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
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert
import org.junit.Test
import org.smartregister.fhircore.R
import org.smartregister.fhircore.RobolectricTest
import org.smartregister.fhircore.fragment.PatientDetailsCard

class PatientDetailsCardViewHolderTest : RobolectricTest() {

  @Test
  fun verifyBindToAppendsDataToView() {
    val card = PatientDetailsCard(0, 0, "1", "Patient", "RegistrationDate", "Details")

    val itemView = mockk<View>()

    val tvCardTitle = mockk<TextView>()
    val tvCardDetail = mockk<TextView>()

    every { tvCardTitle.text = any<String>() } returns Unit
    every { tvCardDetail.text = any<String>() } returns Unit
    every { tvCardDetail.visibility = any() } returns Unit

    every { tvCardTitle.text } returns card.title
    every { tvCardDetail.text } returns card.details

    every {
      hint(TextView::class)
      itemView.findViewById<TextView>(R.id.card_title)
    } returns tvCardTitle
    every {
      hint(TextView::class)
      itemView.findViewById<TextView>(R.id.card_details)
    } returns tvCardDetail

    val viewHolder = PatientDetailsCardViewHolder(itemView)
    viewHolder.bindTo(card)

    val cardTitle = itemView.findViewById<TextView>(R.id.card_title)
    val cardDetails = itemView.findViewById<TextView>(R.id.card_details)

    Assert.assertEquals("RegistrationDate", cardTitle.text.toString())
    Assert.assertEquals("Details", cardDetails.text.toString())
  }
}
