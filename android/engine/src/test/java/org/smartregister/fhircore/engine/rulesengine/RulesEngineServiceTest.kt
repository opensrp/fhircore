/*
 * Copyright 2021-2023 Ona Systems, Inc
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

package org.smartregister.fhircore.engine.rulesengine

import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import java.util.Locale
import javax.inject.Inject
import org.hl7.fhir.r4.model.ResourceType
import org.joda.time.LocalDate
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.domain.model.RelatedResourceCount
import org.smartregister.fhircore.engine.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.util.extension.SDF_DD_MMM_YYYY
import org.smartregister.fhircore.engine.util.extension.SDF_YYYY_MM_DD
import org.smartregister.fhircore.engine.util.extension.formatDate

@HiltAndroidTest
class RulesEngineServiceTest : RobolectricTest() {
  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)
  @Inject lateinit var rulesFactory: RulesFactory
  private lateinit var rulesEngineService: RulesFactory.RulesEngineService

  @Before
  fun setUp() {
    hiltRule.inject()
    rulesEngineService = rulesFactory.RulesEngineService()
    Assert.assertNotNull(rulesEngineService)
  }

  @After
  fun tearDown() {
    Locale.setDefault(Locale.ENGLISH)
  }

  @Test
  fun testTranslateWithDefaultLocaleReturnsCorrectTranslatedString() {
    val templateString = "Vaccine status"

    val result = rulesEngineService.translate(templateString)

    Assert.assertEquals("Translated Vaccine status", result)
  }

  @Test
  fun testTranslateWithOtherLocaleReturnsCorrectTranslatedString() {
    val templateString = "Vaccine status"
    Locale.setDefault(Locale.FRENCH)

    val result = rulesEngineService.translate(templateString)

    Assert.assertEquals("Statut Vaccinal Traduit", result)
  }

  @Test
  fun testComputeTotalCountShouldReturnSumOfAllCounts() {
    val totalCount =
        rulesEngineService.computeTotalCount(
            listOf(
                RelatedResourceCount(relatedResourceType = ResourceType.Task, "abc", 20),
                RelatedResourceCount(relatedResourceType = ResourceType.Task, "zyx", 40),
                RelatedResourceCount(relatedResourceType = ResourceType.Task, "xyz", 40)))
    Assert.assertEquals(100, totalCount)

    Assert.assertEquals(0, rulesEngineService.computeTotalCount(emptyList()))
    Assert.assertEquals(0, rulesEngineService.computeTotalCount(null))
  }

  @Test
  fun testRetrieveCountShouldReturnExactCount() {
    val relatedResourceCounts =
        listOf(
            RelatedResourceCount(relatedResourceType = ResourceType.Task, "abc", 20),
            RelatedResourceCount(relatedResourceType = ResourceType.Task, "zyx", 40),
            RelatedResourceCount(relatedResourceType = ResourceType.Task, "xyz", 40))
    val theCount = rulesEngineService.retrieveCount("xyz", relatedResourceCounts)
    Assert.assertEquals(40, theCount)

    Assert.assertEquals(0, rulesEngineService.retrieveCount("abz", relatedResourceCounts))
    Assert.assertEquals(0, rulesEngineService.retrieveCount("abc", emptyList()))
    Assert.assertEquals(0, rulesEngineService.retrieveCount("abc", null))
  }
  @Test
  fun testGetDateDifferenceFromYearsAndWithDateFormat() {
    val limit1 = "5" // Under 5 years
    val limit2 = "18" // Under 18 years
    val simpleDateFormat = SDF_DD_MMM_YYYY
    val actualUnderFive =
        rulesEngineService.calculateStartOrEndDateFromCriteria(limit1, "BELOW", simpleDateFormat)
    val actualUnder18 = rulesEngineService.calculateStartOrEndDateFromCriteria(limit2, "BELOW")
    val expectedUnderFive =
        LocalDate.now().minusYears(limit1.toInt()).toDate().formatDate(simpleDateFormat)
    val expectedUnder18 =
        LocalDate.now().minusYears(limit2.toInt()).toDate().formatDate(simpleDateFormat)
    Assert.assertEquals(expectedUnderFive, actualUnderFive)
    Assert.assertEquals(expectedUnder18, actualUnder18)
  }
  @Test
  fun testGetDateDifferenceFromYearsWithoutDateFormat() {
    val limit1 = "5" // Above 5 years
    val limit2 = "18" // Above 18 years
    val actualUnderFive =
      rulesEngineService.calculateStartOrEndDateFromCriteria(limit1, "ABOVE")
    val actualUnder18 = rulesEngineService.calculateStartOrEndDateFromCriteria(limit2, "ABOVE")
    val expectedUnderFive =
      LocalDate.now().plusYears(limit1.toInt()).toDate().formatDate(SDF_YYYY_MM_DD)
    val expectedUnder18 =
      LocalDate.now().plusYears(limit2.toInt()).toDate().formatDate(SDF_YYYY_MM_DD)
    Assert.assertEquals(expectedUnderFive, actualUnderFive)
    Assert.assertEquals(expectedUnder18, actualUnder18)
  }
}
