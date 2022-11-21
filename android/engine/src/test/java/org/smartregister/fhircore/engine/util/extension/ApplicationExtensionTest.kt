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

package org.smartregister.fhircore.engine.util.extension

import com.google.android.fhir.FhirEngine
import com.google.android.fhir.db.ResourceNotFoundException
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.ResourceType
import org.junit.Assert
import org.junit.Test
import org.smartregister.fhircore.engine.robolectric.RobolectricTest

class ApplicationExtensionTest : RobolectricTest() {

  @Test
  fun `FhirEngine#loadResource() should call load and return resources`() {
    val fhirEngine = mockk<FhirEngine>()
    val patientId = "patient-john-doe"
    val patient2 = Patient().apply { id = patientId }

    coEvery { fhirEngine.get(ResourceType.Patient, patientId) } returns patient2

    val patient: Patient?
    runBlocking { patient = fhirEngine.loadResource(patientId) }

    coVerify { fhirEngine.get(ResourceType.Patient, patientId) }
    Assert.assertEquals(patient2, patient)
    Assert.assertEquals(patient2.id, patient!!.id)
  }

  @Test
  fun `FhirEngine#loadResource() should return null when resource not found and ResourceNotFoundException is thrown`() {
    val fhirEngine = mockk<FhirEngine>()
    val patientId = "patient-john-doe"
    coEvery { fhirEngine.get(ResourceType.Patient, patientId) } throws
      ResourceNotFoundException("Patient not found", "Patient with id $patientId was not found")

    val patient: Patient?
    runBlocking { patient = fhirEngine.loadResource(patientId) }

    coVerify { fhirEngine.get(ResourceType.Patient, patientId) }
    Assert.assertNull(patient)
  }
}
