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

package org.smartregister.fhircore.quest.util

import com.google.android.fhir.FhirEngine
import com.google.android.fhir.search.Search
import com.google.android.fhir.search.filter.ReferenceParamFilterCriterion
import com.google.android.fhir.search.filter.TokenParamFilterCriterion
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import java.util.Date
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Condition
import org.hl7.fhir.r4.model.Enumerations
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.robolectric.util.ReflectionHelpers
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.quest.app.fakes.Faker
import org.smartregister.fhircore.quest.configuration.view.Code
import org.smartregister.fhircore.quest.configuration.view.DynamicColor
import org.smartregister.fhircore.quest.configuration.view.Filter
import org.smartregister.fhircore.quest.configuration.view.Properties
import org.smartregister.fhircore.quest.configuration.view.Property
import org.smartregister.fhircore.quest.robolectric.RobolectricTest

@HiltAndroidTest
class PatientUtilTest : RobolectricTest() {

  @BindValue
  var configurationRegistry: ConfigurationRegistry =
    Faker.buildTestConfigurationRegistry("g6pd", mockk())
  @Inject lateinit var fhirEngine: FhirEngine

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  @Before
  fun setUp() {
    hiltRule.inject()

    fhirEngine = spyk(fhirEngine)
  }

  @Test
  fun testLoadAdditionalDataShouldReturnExpectedData() {
    val searchSlot = slot<Search>()
    coEvery { fhirEngine.search<Condition>(capture(searchSlot)) } returns getConditions()

    val data = runBlocking { loadAdditionalData("", configurationRegistry, fhirEngine) }

    Assert.assertNotNull(searchSlot.captured)

    val referenceFilterParamCriterion: MutableList<Any> =
      ReflectionHelpers.getField(searchSlot.captured, "referenceFilterCriteria")
    val referenceFilters: MutableList<ReferenceParamFilterCriterion> =
      ReflectionHelpers.getField(referenceFilterParamCriterion[0], "filters")

    Assert.assertEquals(1, referenceFilters.size)

    val tokenFilterParamCriterion: MutableList<Any> =
      ReflectionHelpers.getField(searchSlot.captured, "tokenFilterCriteria")
    val tokenFilters: MutableList<TokenParamFilterCriterion> =
      ReflectionHelpers.getField(tokenFilterParamCriterion[0], "filters")

    Assert.assertEquals(1, tokenFilters.size)

    Assert.assertEquals(1, data.size)
    with(data[0]) {
      Assert.assertNull(label)
      Assert.assertEquals(" G6PD Status - ", valuePrefix)
      Assert.assertEquals("Intermediate", value)
      Assert.assertEquals("#FFA500", properties?.value?.color)
    }
  }

  @Test
  fun testPropertiesMapping() {

    var filter =
      Filter(
        resourceType = Enumerations.ResourceType.CONDITION,
        key = "code",
        valueType = Enumerations.DataType.CODEABLECONCEPT,
        valueCoding = Code(),
        dynamicColors = listOf(DynamicColor("Normal", "#00FF00"))
      )

    var properties = runBlocking { propertiesMapping("Normal", filter) }

    Assert.assertNull(properties.label)
    Assert.assertNull(properties.value?.textSize)
    Assert.assertEquals("#00FF00", properties.value?.color)

    filter =
      Filter(
        resourceType = Enumerations.ResourceType.CONDITION,
        key = "code",
        valueType = Enumerations.DataType.CODEABLECONCEPT,
        valueCoding = Code(),
        properties = Properties(label = Property(), value = Property(color = "#000000", 20))
      )

    properties = runBlocking { propertiesMapping("Deficient", filter) }

    Assert.assertNotNull(properties.label)
    Assert.assertEquals("#000000", properties.value?.color)
    Assert.assertEquals(20, properties.value?.textSize)
  }

  private fun getConditions(): List<Condition> {
    return listOf(
      Condition().apply {
        recordedDate = Date()
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
