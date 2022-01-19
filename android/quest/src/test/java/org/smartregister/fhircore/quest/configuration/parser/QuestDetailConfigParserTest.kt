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

import com.google.android.fhir.FhirEngine
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.spyk
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.util.extension.asDdMmmYyyy
import org.smartregister.fhircore.quest.configuration.view.patientDetailsViewConfigurationOf
import org.smartregister.fhircore.quest.robolectric.RobolectricTest

@HiltAndroidTest
class QuestDetailConfigParserTest : RobolectricTest() {

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)
  @Inject lateinit var fhirEngine: FhirEngine
  lateinit var questDetailConfigParser: QuestDetailConfigParser

  @Before
  fun setUp() {
    hiltRule.inject()
    fhirEngine = spyk(fhirEngine)
    questDetailConfigParser = QuestDetailConfigParser(fhirEngine)
  }

  @Test
  fun testGetResultItemShouldReturnCorrectData() {
    val today = Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant())

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
      }

    val patientDetailsViewConfiguration = patientDetailsViewConfigurationOf()

    val data = runBlocking {
      questDetailConfigParser.getResultItem(
        questionnaire,
        questionnaireResponse,
        patientDetailsViewConfiguration
      )
    }
    with(data.data[0]) {
      Assert.assertEquals("Questionnaire Name", this[0].value)
      Assert.assertEquals(" (${today.asDdMmmYyyy()})", this[1].value)
    }

    with(data.source) {
      Assert.assertEquals("1", first.id)
      Assert.assertEquals("Questionnaire/1", first.questionnaire)
      Assert.assertEquals(today, first.authored)

      Assert.assertEquals("1", second.id)
      Assert.assertEquals("Questionnaire Name", second.name)
      Assert.assertEquals("Questionnaire Title", second.title)
    }
  }

  @Test
  fun testGetResultItemWithNullNameTitleShouldReturnCorrectData() {
    val today = Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant())

    val questionnaire = Questionnaire().apply { this.id = "1" }

    val questionnaireResponse =
      QuestionnaireResponse().apply {
        this.id = "1"
        this.questionnaire = "Questionnaire/1"
        this.authored = today
      }

    val patientDetailsViewConfiguration = patientDetailsViewConfigurationOf()

    val data = runBlocking {
      questDetailConfigParser.getResultItem(
        questionnaire,
        questionnaireResponse,
        patientDetailsViewConfiguration
      )
    }
    with(data.data[0]) {
      Assert.assertEquals("1", this[0].value)
      Assert.assertEquals(" (${today.asDdMmmYyyy()})", this[1].value)
    }

    with(data.source) {
      Assert.assertEquals("1", first.id)
      Assert.assertEquals("Questionnaire/1", first.questionnaire)
      Assert.assertEquals(today, first.authored)

      Assert.assertEquals("1", second.id)
      Assert.assertEquals("Questionnaire Name", second.name)
      Assert.assertEquals("Questionnaire Title", second.title)
    }
  }

  @Test
  fun testGetResultItemWithNullNameShouldReturnCorrectData() {
    val today = Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant())

    val questionnaire =
      Questionnaire().apply {
        this.id = "1"
        this.title = "Questionnaire Title"
      }

    val questionnaireResponse =
      QuestionnaireResponse().apply {
        this.id = "1"
        this.questionnaire = "Questionnaire/1"
        this.authored = today
      }

    val patientDetailsViewConfiguration = patientDetailsViewConfigurationOf()

    val data = runBlocking {
      questDetailConfigParser.getResultItem(
        questionnaire,
        questionnaireResponse,
        patientDetailsViewConfiguration
      )
    }
    with(data.data[0]) {
      Assert.assertEquals("Questionnaire Title", this[0].value)
      Assert.assertEquals(" (${today.asDdMmmYyyy()})", this[1].value)
    }

    with(data.source) {
      Assert.assertEquals("1", first.id)
      Assert.assertEquals("Questionnaire/1", first.questionnaire)
      Assert.assertEquals(today, first.authored)

      Assert.assertEquals("1", second.id)
      Assert.assertEquals("Questionnaire Name", second.name)
      Assert.assertEquals("Questionnaire Title", second.title)
    }
  }
}
