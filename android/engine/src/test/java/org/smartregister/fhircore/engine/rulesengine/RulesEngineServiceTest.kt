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

package org.smartregister.fhircore.engine.rulesengine

import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import java.util.Locale
import javax.inject.Inject
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.robolectric.RobolectricTest

@HiltAndroidTest
class RulesEngineServiceTest : RobolectricTest() {
  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  @Inject lateinit var rulesEngineService: RulesEngineService

  @Before
  fun setUp() {
    hiltRule.inject()

    Assert.assertNotNull(rulesEngineService)
  }

  @After
  fun tearDown() {
    Locale.setDefault(Locale.ENGLISH)
  }

  @Test
  fun testTranslateWithDefaultLocaleReturnsCorrectTranslatedString() {

    val templateString = "{{vaccine.status}} is @{fhirpath.evaluate(value)}"

    val result = rulesEngineService.translate(templateString)

    Assert.assertEquals("Vaccine status is @{fhirpath.evaluate(value)}", result)
  }

  @Test
  fun testTranslateWithOtherLocaleReturnsCorrectTranslatedString() {

    val templateString = "{{vaccine.status}} @{fhirpath.evaluate(value)}"
    Locale.setDefault(Locale.FRENCH)

    val result = rulesEngineService.translate(templateString)

    Assert.assertEquals("Le statut vaccinal est @{fhirpath.evaluate(value)}", result)
  }
}
