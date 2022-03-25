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

package org.smartregister.fhircore.quest.data.task.model

import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.smartregister.fhircore.engine.util.DateUtils.getDate
import org.smartregister.fhircore.engine.util.extension.toAgeDisplay
import org.smartregister.fhircore.quest.robolectric.RobolectricTest

@HiltAndroidTest
class PatientTaskItemTest : RobolectricTest() {

  private lateinit var patientTaskItem: PatientTaskItem

  @Before
  fun setUp() {
    patientTaskItem =
      PatientTaskItem(
        id = "1",
        name = "Eve",
        gender = "F",
        birthdate = "2020-03-10".getDate("yyyy-MM-dd"),
        address = "Nairobi",
        description = "Sick Visit",
        overdue = true
      )
  }

  @Test
  fun testDemographicsShouldReturnFlatDemographic() {
    Assert.assertEquals(
      "${patientTaskItem.name}, ${patientTaskItem.gender}, ${patientTaskItem.birthdate.toAgeDisplay()}",
      patientTaskItem.demographics()
    )
  }
}
