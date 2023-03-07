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

package org.smartregister.fhircore.engine.app.fakes

import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.spyk
import java.io.File
import java.util.Calendar
import java.util.Date
import kotlinx.coroutines.runBlocking
import org.hl7.fhir.r4.model.Binary
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.Composition
import org.hl7.fhir.r4.model.DateType
import org.hl7.fhir.r4.model.Enumerations
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.StringType
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.robolectric.RobolectricTest.Companion.readFile
import org.smartregister.fhircore.engine.util.extension.decodeResourceFromString

object Faker {

  private val systemPath =
    (System.getProperty("user.dir") +
      File.separator +
      "src" +
      File.separator +
      "main" +
      File.separator +
      "assets" +
      File.separator)

  fun loadTestConfigurationRegistryData(
    defaultRepository: DefaultRepository,
    configurationRegistry: ConfigurationRegistry
  ) {
    val composition =
      getBasePath("composition").readFile(systemPath).decodeResourceFromString() as Composition
    coEvery { defaultRepository.searchCompositionByIdentifier(any()) } returns composition

    coEvery { defaultRepository.getBinary(any()) } answers
      {
        val sectionComponent =
          composition.section.find {
            this.args.first().toString() == it.focus.reference.substringAfter("Binary/")
          }
        val configName = sectionComponent!!.focus.identifier.value
        Binary().apply { content = getBasePath(configName).readFile(systemPath).toByteArray() }
      }

    runBlocking { configurationRegistry.loadConfigurations(appId = "default") {} }
  }

  private fun getBasePath(configName: String): String {
    return "/configs/default/config_$configName.json"
  }

  fun buildTestConfigurationRegistry(defaultRepository: DefaultRepository): ConfigurationRegistry {
    val configurationRegistry =
      spyk(ConfigurationRegistry(mockk(), mockk(), mockk(), mockk(), defaultRepository))

    loadTestConfigurationRegistryData(defaultRepository, configurationRegistry)

    return configurationRegistry
  }

  fun buildPatient(
    id: String = "sampleId",
    family: String = "Mandela",
    given: String = "Nelson",
    age: Int = 78,
    gender: Enumerations.AdministrativeGender? = Enumerations.AdministrativeGender.MALE,
    patientType: String = "",
    practitionerReference: String = "",
    deceased: Boolean = false
  ): Patient {
    return Patient().apply {
      this.id = id
      this.identifierFirstRep.value = id
      this.addName().apply {
        this.family = family
        this.given.add(StringType(given))
      }
      this.gender = gender
      this.birthDate = DateType(Date()).apply { add(Calendar.YEAR, -age) }.dateTimeValue().value

      this.addAddress().apply {
        district = "Dist 1"
        city = "City 1"
      }

      this.meta.addTag(
        Coding().apply {
          system = "https://d-tree.org"
          code = patientType
          display = "Exposed Infant"
        }
      )

      this.generalPractitionerFirstRep.apply { reference = practitionerReference }
      this.deceased = if (deceased) DateType(Date()) else null
    }
  }
}
