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

package org.smartregister.fhircore.quest.configuration.parser

import ca.uhn.fhir.rest.gclient.TokenClientParam
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.search.search
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.coEvery
import io.mockk.spyk
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.Condition
import org.hl7.fhir.r4.model.Encounter
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.Reference
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.util.extension.asDdMmmYyyy
import org.smartregister.fhircore.quest.configuration.view.FontWeight
import org.smartregister.fhircore.quest.configuration.view.patientDetailsViewConfigurationOf
import org.smartregister.fhircore.quest.robolectric.RobolectricTest

@HiltAndroidTest
class G6PDDetailConfigParserTest : RobolectricTest() {

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)
  @Inject lateinit var fhirEngine: FhirEngine
  lateinit var g6PDDetailConfigParser: G6PDDetailConfigParser

  @Before
  fun setUp() {
    hiltRule.inject()
    fhirEngine = spyk(fhirEngine)
    g6PDDetailConfigParser = G6PDDetailConfigParser(fhirEngine)
  }

  @Test
  fun testGetResultItemShouldReturnCorrectData() {
    val today = Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant())

    coEvery {
      fhirEngine.search<Condition> {
        filter(Condition.ENCOUNTER, { value = "Encounter/1" })
        filter(
          TokenClientParam("category"),
          {
            value =
              of(CodeableConcept().addCoding(Coding("http://snomed.info/sct", "9024005", null)))
          }
        )
      }
    } returns getConditions()

    coEvery {
      fhirEngine.search<Observation> {
        filter(Observation.ENCOUNTER, { value = "Encounter/1" })
        filter(
          TokenClientParam("code"),
          {
            value =
              of(CodeableConcept().addCoding(Coding("http://snomed.info/sct", "259695003", null)))
          }
        )
      }
    } returns getObservations()

    val questionnaire =
      Questionnaire().apply {
        this.id = "1"
        this.name = "Questionnaire Name"
        this.title = "Questionnaire Title"
      }

    val questionnaireResponse =
      QuestionnaireResponse().apply {
        this.id = "1"
        this.questionnaire = "Questionnaire/1"
        this.authored = today
        this.contained = listOf(Encounter().apply { id = "1" })
      }

    val patientDetailsViewConfiguration = patientDetailsViewConfigurationOf(appId = "quest")

    val data = runBlocking {
      g6PDDetailConfigParser.getResultItem(
        questionnaire,
        questionnaireResponse,
        patientDetailsViewConfiguration
      )
    }

    Assert.assertEquals(2, data.data.size)

    with(data.data[0]) {
      Assert.assertEquals("Intermediate", this[0].value)
      Assert.assertEquals(" (${today.asDdMmmYyyy()}) ", this[1].value)
    }

    with(data.data[1]) {
      Assert.assertEquals("G6PD: ", this[0].label)
      Assert.assertEquals("#74787A", this[0].properties?.label?.color)
      Assert.assertEquals(16, this[0].properties?.label?.textSize)
      Assert.assertEquals(FontWeight.NORMAL, this[0].properties?.label?.fontWeight)

      Assert.assertEquals(" - Hb: ", this[1].label)
      Assert.assertEquals("#74787A", this[1].properties?.label?.color)
      Assert.assertEquals(16, this[1].properties?.label?.textSize)
      Assert.assertEquals(FontWeight.NORMAL, this[1].properties?.label?.fontWeight)
    }
  }

  private fun getObservations(): List<Observation> {
    return listOf(
      Observation().apply {
        encounter = Reference().apply { reference = "Encounter/1" }
        code =
          CodeableConcept().apply {
            addCoding().apply {
              system = "http://snomed.info/sct"
              code = "86859003"
            }
          }
      },
      Observation().apply {
        encounter = Reference().apply { reference = "Encounter/1" }
        code =
          CodeableConcept().apply {
            addCoding().apply {
              system = "http://snomed.info/sct"
              code = "259695003"
            }
          }
      }
    )
  }

  private fun getConditions(): List<Condition> {
    val today = Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant())
    return listOf(
      Condition().apply {
        recordedDate = today
        category =
          listOf(
            CodeableConcept().apply {
              addCoding().apply {
                system = "http://snomed.info/sct"
                code = "9024005"
              }
            }
          )
        code =
          CodeableConcept().apply {
            addCoding().apply {
              system = "http://snomed.info/sct"
              code = "11896004"
              display = "Intermediate"
            }
          }
      }
    )
  }
}
