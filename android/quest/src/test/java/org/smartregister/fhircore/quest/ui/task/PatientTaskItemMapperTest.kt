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

package org.smartregister.fhircore.quest.ui.task

import androidx.test.core.app.ApplicationProvider
import org.hl7.fhir.r4.model.Enumerations
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Period
import org.hl7.fhir.r4.model.StringType
import org.hl7.fhir.r4.model.Task
import org.junit.Assert
import org.junit.Test
import org.smartregister.fhircore.engine.util.DateUtils.getDate
import org.smartregister.fhircore.quest.robolectric.RobolectricTest

class PatientTaskItemMapperTest : RobolectricTest() {

  private val mapper = PatientTaskItemMapper(ApplicationProvider.getApplicationContext())

  @Test
  fun testPerformSearchFilterShouldReturnTrue() {
    val patientTask =
      PatientTask(
        patient =
          Patient().apply {
            id = "2"
            nameFirstRep.given = listOf(StringType("Name"))
            nameFirstRep.family = "Surname"
            gender = Enumerations.AdministrativeGender.MALE
            addressFirstRep.city = "Nairobi"
            birthDate = "2010-03-10".getDate("yyyy-MM-dd")
          },
        task =
          Task().apply {
            id = "1"
            description = "Sick Visit"
            executionPeriod =
              Period().apply {
                start = "2020-03-10".getDate("yyyy-MM-dd")
                end = "2020-03-12".getDate("yyyy-MM-dd")
              }
          }
      )

    val patientTaskItem = mapper.transformInputToOutputModel(patientTask)
    Assert.assertEquals("1", patientTaskItem.id)
    Assert.assertEquals("Name Surname", patientTaskItem.name)
    Assert.assertEquals("M", patientTaskItem.gender)
    Assert.assertEquals("2010-03-10".getDate("yyyy-MM-dd"), patientTaskItem.birthdate)
    Assert.assertEquals("Nairobi", patientTaskItem.address)
    Assert.assertEquals("Sick Visit", patientTaskItem.description)
    Assert.assertTrue(patientTaskItem.overdue)
  }
}
