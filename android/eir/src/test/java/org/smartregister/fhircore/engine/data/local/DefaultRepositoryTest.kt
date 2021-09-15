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

package org.smartregister.fhircore.engine.data.local

import com.google.android.fhir.FhirEngine
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.util.Date
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.hl7.fhir.r4.model.Address
import org.hl7.fhir.r4.model.ContactPoint
import org.hl7.fhir.r4.model.Enumerations
import org.hl7.fhir.r4.model.HumanName
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.StringType
import org.junit.Ignore
import org.junit.jupiter.api.Test
import org.robolectric.annotation.Config
import org.smartregister.fhircore.eir.robolectric.RobolectricTest
import org.smartregister.fhircore.eir.shadow.EirApplicationShadow
import org.smartregister.fhircore.shadow.ShadowNpmPackageProvider

/** Created by Ephraim Kigamba - nek.eam@gmail.com on 15-09-2021. */
@Config(shadows = [EirApplicationShadow::class, ShadowNpmPackageProvider::class])
@Ignore("JSONObject constructor with string not working causing NPE")
class DefaultRepositoryTest : RobolectricTest() {

  @Test
  fun `addOrUpdate() should call fhirEngine#update when resource exists`() {
    val patient =
      Patient().apply {
        id = UUID.randomUUID().toString()
        active = true
        birthDate = Date(1996, 8, 17)
        gender = Enumerations.AdministrativeGender.MALE
        address =
          listOf(
            Address().apply {
              city = "Lahore"
              country = "Pakistan"
            }
          )
        name =
          listOf(
            HumanName().apply {
              given = mutableListOf(StringType("Salman"))
              family = "Ali"
            }
          )
        telecom = listOf(ContactPoint().apply { value = "12345" })
      }

    val fhirEngine: FhirEngine = mockk()
    coEvery { fhirEngine.load(Patient::class.java, patient.idElement.idPart) } returns patient
    /*coEvery { fhirEngine.load(Patient::class.java, any()) } returns TestUtils.TEST_PATIENT_1

    mockkObject(EirApplication)
    every { EirApplication.getContext().fhirEngine } returns fhirEngine*/
    val defaultRepository = DefaultRepository(fhirEngine)

    runBlocking { defaultRepository.addOrUpdate(patient) }

    coVerify { fhirEngine.update(patient) }
  }
}
