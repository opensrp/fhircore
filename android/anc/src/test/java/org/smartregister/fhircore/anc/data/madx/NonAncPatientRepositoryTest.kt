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

package org.smartregister.fhircore.anc.data.madx

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.android.fhir.FhirEngine
import io.mockk.spyk
import java.util.Date
import org.hl7.fhir.r4.model.CarePlan
import org.hl7.fhir.r4.model.DateType
import org.hl7.fhir.r4.model.Reference
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.anc.robolectric.RobolectricTest
import org.smartregister.fhircore.anc.ui.madx.details.NonAncPatientItemMapper
import org.smartregister.fhircore.engine.util.DateUtils.getDate
import org.smartregister.fhircore.engine.util.DateUtils.makeItReadable
import org.smartregister.fhircore.engine.util.extension.plusWeeksAsString

class NonAncPatientRepositoryTest : RobolectricTest() {
  private lateinit var repository: NonAncPatientRepository
  private lateinit var fhirEngine: FhirEngine

  @get:Rule var instantTaskExecutorRule = InstantTaskExecutorRule()

  @Before
  fun setUp() {
    fhirEngine = spyk()
    repository = spyk(NonAncPatientRepository(fhirEngine, NonAncPatientItemMapper))
  }

  @Test
  fun fetchCarePlanItemTest() {
    val patientId = "1111"
    val carePlan = listOf(buildCarePlanWithActive(patientId))
    val listCarePlan = repository.fetchCarePlanItem(carePlan = carePlan, patientId = patientId)
    if (listCarePlan.isNotEmpty()) {
      Assert.assertEquals(patientId, listCarePlan[0].patientIdentifier)
      Assert.assertEquals("ABC", listCarePlan[0].title)
    }
  }

  @Test
  fun fetchUpcomingServiceItemTest() {
    val patientId = "1111"
    val carePlan = listOf(buildCarePlanWithActive(patientId))
    val listUpcomingServiceItem =
      repository.fetchUpcomingServiceItem(patientId = patientId, carePlan = carePlan)
    Assert.assertEquals(patientId, listUpcomingServiceItem[0].patientIdentifier)
    Assert.assertEquals("ABC", listUpcomingServiceItem[0].title)
    Assert.assertEquals(Date().makeItReadable(), listUpcomingServiceItem[0].date)
  }

  private fun buildCarePlanWithActive(subject: String): CarePlan {
    val date = DateType(Date())
    val end = date.plusWeeksAsString(4).getDate("yyyy-MM-dd")
    return CarePlan().apply {
      this.id = "11190"
      this.status = CarePlan.CarePlanStatus.ACTIVE
      this.period.start = date.value
      this.period.end = end
      this.subject = Reference().apply { reference = "Patient/$subject" }
      this.addActivity().detail.apply {
        this.description = "ABC"
        this.scheduledPeriod.start = Date()
        this.status = CarePlan.CarePlanActivityStatus.SCHEDULED
      }
    }
  }
}
